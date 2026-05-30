package cn.iocoder.yudao.server.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRateLimit;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRequest;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class QwenMtImageProvider implements ImageTranslationProvider {

    public static final String NAME = "qwen_mt_image";
    private static final String MODEL = "qwen-mt-image";

    @Resource
    private ImageTranslationProperties properties;
    @Resource
    private ProviderRateLimiter rateLimiter;
    @Resource
    private AppIntegrationConfigService integrationConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        ImageTranslationProperties.BailianImageTranslate config = properties.getBailianImageTranslate();
        return config.isEnabled()
                && isQwenProvider(config.getProviderType())
                && StrUtil.isNotBlank(apiKey());
    }

    @Override
    public ProviderRateLimit rateLimit() {
        int rpm = Math.max(1, properties.getBailianImageTranslate().getRpmLimit());
        return new ProviderRateLimit().setPermits(1).setIntervalMillis(Math.max(1000L, 60_000L / rpm));
    }

    @Override
    public ProviderResult translate(ProviderRequest request) {
        long start = System.nanoTime();
        if (!enabled()) {
            log.info("image translation qwen-mt-image disabled traceId={} taskNo={}",
                    safe(request.getTraceId()), safe(request.getTaskNo()));
            return failure("ProviderDisabled", "Qwen-MT-Image is disabled or not configured", 0, start);
        }
        rateLimiter.acquire(providerName(), rateLimit());
        try {
            SubmitResult submit = submitTask(request);
            if (StrUtil.isBlank(submit.taskId())) {
                return failure("EmptyTaskId", "Qwen-MT-Image returned empty task id", submit.httpStatus(), start)
                        .setRequestId(submit.requestId())
                        .setRawResponseJson(submit.rawJson());
            }
            ProviderResult result = pollTask(submit.taskId(), start);
            if (StrUtil.isBlank(result.getRequestId())) {
                result.setRequestId(submit.requestId());
            }
            result.setProviderTaskId(submit.taskId());
            return result;
        } catch (Exception ex) {
            return failure("RequestFailed", "Qwen-MT-Image request failed", 0, start);
        }
    }

    private SubmitResult submitTask(ProviderRequest request) throws Exception {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("image_url", request.getPresignedImageUrl());
        input.put("source_lang", StrUtil.blankToDefault(normalizeLanguage(request.getSourceLang()), "auto"));
        input.put("target_lang", normalizeLanguage(request.getTargetLang()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("input", input);

        HttpResponse<String> response = postJson(translateEndpoint(), body, true);
        JsonNode root = parse(response.body());
        String taskId = firstText(root.path("output"), "task_id", "taskId", "TaskId");
        String requestId = firstText(root, "request_id", "requestId", "RequestId");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Qwen-MT-Image submit failed: " + response.statusCode());
        }
        return new SubmitResult(taskId, requestId, root.toString(), response.statusCode());
    }

    private ProviderResult pollTask(String taskId, long start) throws Exception {
        ImageTranslationProperties.BailianImageTranslate config = properties.getBailianImageTranslate();
        JsonNode lastRoot = objectMapper.createObjectNode();
        int httpStatus = 0;
        for (int i = 0; i < Math.max(1, config.getMaxPollTimes()); i++) {
            sleep(Math.max(1, config.getPollIntervalSeconds()) * 1000L);
            HttpResponse<String> response = getJson(taskEndpoint(taskId));
            httpStatus = response.statusCode();
            JsonNode root = parse(response.body());
            lastRoot = root;
            String status = firstText(root.path("output"), "task_status", "taskStatus", "status");
            if ("SUCCEEDED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                JsonNode output = root.path("output");
                return new ProviderResult()
                        .setSuccess(true)
                        .setProvider(providerName())
                        .setRequestId(firstText(root, "request_id", "requestId", "RequestId"))
                        .setProviderTaskId(taskId)
                        .setFinalImageUrl(firstText(output, "image_url", "imageUrl", "result_url", "url"))
                        .setRawResponseJson(root.toString())
                        .setHttpStatus(httpStatus)
                        .setCostMs(elapsedMs(start));
            }
            if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                return failure(firstText(root, "code", "Code", "error_code", "ErrorCode"),
                        firstText(root, "message", "Message", "error_message", "ErrorMessage"),
                        httpStatus,
                        start).setRequestId(firstText(root, "request_id", "requestId", "RequestId"))
                        .setRawResponseJson(root.toString())
                        .setProviderTaskId(taskId);
            }
        }
        return failure("PollTimeout", "Qwen-MT-Image polling timed out", httpStatus, start)
                .setRawResponseJson(lastRoot.toString())
                .setProviderTaskId(taskId);
    }

    private HttpResponse<String> postJson(String uri, Map<String, Object> body, boolean async) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(Math.max(10, properties.getBailianImageTranslate().getTimeoutSeconds())))
                .header("Authorization", "Bearer " + apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
        if (async) {
            builder.header("X-DashScope-Async", "enable");
        }
        return httpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> getJson(String uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(Math.max(10, properties.getBailianImageTranslate().getTimeoutSeconds())))
                .header("Authorization", "Bearer " + apiKey())
                .GET()
                .build();
        return httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String translateEndpoint() {
        String endpoint = StrUtil.blankToDefault(properties.getBailianImageTranslate().getEndpoint(),
                "https://dashscope.aliyuncs.com");
        endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        if (endpoint.endsWith("/image-synthesis")) {
            return endpoint;
        }
        return endpoint + "/api/v1/services/aigc/image2image/image-synthesis";
    }

    private String taskEndpoint(String taskId) {
        URI uri = URI.create(translateEndpoint());
        return uri.getScheme() + "://" + uri.getHost() + "/api/v1/tasks/" + taskId;
    }

    private String apiKey() {
        String configured = properties.getBailianImageTranslate().getApiKey();
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        return integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_DASHSCOPE_API_KEY);
    }

    private boolean isQwenProvider(String providerType) {
        String value = StrUtil.blankToDefault(providerType, "").trim().toLowerCase(Locale.ROOT);
        return NAME.equals(value) || "qwen-mt-image".equals(value) || "qwenmtimage".equals(value);
    }

    private String normalizeLanguage(String lang) {
        String value = StrUtil.blankToDefault(lang, "").trim();
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return switch (value.toLowerCase(Locale.ROOT).replace('_', '-')) {
            case "auto" -> "auto";
            case "zh", "zh-cn", "cn", "chinese", "中文" -> "Chinese";
            case "zh-tw", "traditional-chinese", "繁体中文" -> "Traditional Chinese";
            case "en", "english", "英文" -> "English";
            case "ja", "jp", "japanese", "日文", "日语" -> "Japanese";
            case "ko", "kr", "korean", "韩文", "韩语" -> "Korean";
            case "fr", "french" -> "French";
            case "de", "german" -> "German";
            case "es", "spanish" -> "Spanish";
            case "ru", "russian" -> "Russian";
            case "it", "italian" -> "Italian";
            case "pt", "portuguese" -> "Portuguese";
            case "ar", "arabic" -> "Arabic";
            case "th", "thai" -> "Thai";
            case "vi", "vietnamese" -> "Vietnamese";
            case "id", "indonesian" -> "Indonesian";
            case "ms", "malay" -> "Malay";
            case "tr", "turkish" -> "Turkish";
            default -> value;
        };
    }

    private JsonNode parse(String body) throws Exception {
        return StrUtil.isBlank(body) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText("");
            }
        }
        return "";
    }

    private ProviderResult failure(String code, String message, int httpStatus, long start) {
        return new ProviderResult()
                .setSuccess(false)
                .setProvider(providerName())
                .setErrorCode(StrUtil.blankToDefault(code, "Unknown"))
                .setErrorMessage(StrUtil.maxLength(StrUtil.blankToDefault(message, "通义图片翻译服务暂不可用"), 512))
                .setHttpStatus(httpStatus)
                .setCostMs(elapsedMs(start));
    }

    private long elapsedMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record SubmitResult(String taskId, String requestId, String rawJson, int httpStatus) {
    }
}
