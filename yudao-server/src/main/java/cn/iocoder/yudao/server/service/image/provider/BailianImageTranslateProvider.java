package cn.iocoder.yudao.server.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRateLimit;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRequest;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;
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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class BailianImageTranslateProvider implements ImageTranslationProvider {

    public static final String NAME = "bailian_anytrans";
    private static final String VERSION = "2025-07-07";

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
                && isAnyTransProvider(config.getProviderType())
                && StrUtil.isNotBlank(config.getWorkspaceId())
                && StrUtil.isNotBlank(endpoint())
                && StrUtil.isNotBlank(accessKeyId())
                && StrUtil.isNotBlank(accessKeySecret());
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
            log.info("image translation bailian disabled traceId={} taskNo={}",
                    safe(request.getTraceId()), safe(request.getTaskNo()));
            return failure("ProviderDisabled", "Bailian image translate is disabled or not configured", 0, start);
        }
        rateLimiter.acquire(providerName(), rateLimit());
        try {
            String taskId = submitTask(request);
            if (StrUtil.isBlank(taskId)) {
                return failure("EmptyTaskId", "Bailian returned empty task id", 0, start);
            }
            ProviderResult result = pollTask(taskId, start);
            result.setProviderTaskId(taskId);
            return result;
        } catch (Exception ex) {
            return failure("RequestFailed", "Bailian image translate request failed", 0, start);
        }
    }

    private String submitTask(ProviderRequest request) throws Exception {
        ImageTranslationProperties.BailianImageTranslate config = properties.getBailianImageTranslate();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workspaceId", config.getWorkspaceId());
        body.put("sourceLanguage", request.getSourceLang());
        body.put("targetLanguage", List.of(request.getTargetLang()));
        body.put("text", request.getPresignedImageUrl());
        body.put("scene", StrUtil.blankToDefault(request.getScene(), config.getScene()));
        JsonNode root = postRoa("/anytrans/translate/image/submit", "SubmitImageTranslateTask", body, config.getTimeoutSeconds());
        String code = firstText(root, "Code", "code", "ErrorCode", "errorCode");
        if (StrUtil.isNotBlank(code)
                && !"200".equals(code)
                && !"OK".equalsIgnoreCase(code)
                && !"success".equalsIgnoreCase(code)) {
            throw new IllegalStateException("Bailian submit failed: " + code);
        }
        return firstText(root, "TaskId", "taskId", "Id", "id");
    }

    private ProviderResult pollTask(String taskId, long start) throws Exception {
        ImageTranslationProperties.BailianImageTranslate config = properties.getBailianImageTranslate();
        JsonNode lastRoot = objectMapper.createObjectNode();
        for (int i = 0; i < Math.max(1, config.getMaxPollTimes()); i++) {
            sleep(Math.max(1, config.getPollIntervalSeconds()) * 1000L);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("workspaceId", config.getWorkspaceId());
            body.put("taskId", taskId);
            JsonNode root = postRoa("/anytrans/translate/image/get", "GetImageTranslateTask", body, config.getTimeoutSeconds());
            lastRoot = root;
            String status = firstText(root, "Status", "status", "TaskStatus", "taskStatus");
            boolean hasTranslation = hasAny(root, "translation", "Translation", "translations", "Translations", "boundingBoxes", "BoundingBoxes");
            if ("SUCCEEDED".equalsIgnoreCase(status)
                    || "SUCCESS".equalsIgnoreCase(status)
                    || "COMPLETED".equalsIgnoreCase(status)
                    || "FINISHED".equalsIgnoreCase(status)
                    || "success".equalsIgnoreCase(status)
                    || (StrUtil.isBlank(status) && hasTranslation)) {
                return new ProviderResult()
                        .setSuccess(true)
                        .setProvider(providerName())
                        .setRequestId(firstText(root, "RequestId", "requestId"))
                        .setProviderTaskId(taskId)
                        .setFinalImageUrl(firstText(root, "FinalImageUrl", "finalImageUrl", "imageUrl"))
                        .setRawResponseJson(root.toString())
                        .setHttpStatus(200)
                        .setCostMs(elapsedMs(start));
            }
            if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                return failure(firstText(root, "Code", "code", "ErrorCode", "errorCode"),
                        firstText(root, "Message", "message", "ErrorMessage", "errorMessage"),
                        200,
                        start).setRawResponseJson(root.toString()).setProviderTaskId(taskId);
            }
        }
        return failure("PollTimeout", "Bailian image translate polling timed out", 0, start)
                .setRawResponseJson(lastRoot.toString())
                .setProviderTaskId(taskId);
    }

    private JsonNode postRoa(String path, String action, Map<String, Object> body, int timeoutSeconds) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        URI base = URI.create(endpoint());
        URI uri = base.resolve(path);
        String jsonBody = objectMapper.writeValueAsString(body);
        Map<String, String> headers = AliyunOpenApiSigner.acs3Headers(
                "POST",
                uri.getRawPath(),
                StrUtil.blankToDefault(uri.getRawQuery(), ""),
                jsonBody,
                action,
                VERSION,
                accessKeyId(),
                accessKeySecret(),
                uri.getHost());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(10, timeoutSeconds)))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return StrUtil.isBlank(response.body()) ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    }

    private String endpoint() {
        String endpoint = StrUtil.blankToDefault(properties.getBailianImageTranslate().getEndpoint(),
                "https://anytrans.cn-hangzhou.aliyuncs.com");
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private boolean isAnyTransProvider(String providerType) {
        String value = StrUtil.blankToDefault(providerType, "").trim().toLowerCase(Locale.ROOT);
        return StrUtil.isBlank(value) || "anytrans".equals(value) || NAME.equals(value);
    }

    private String accessKeyId() {
        return StrUtil.blankToDefault(properties.getBailianImageTranslate().getAccessKeyId(),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID));
    }

    private String accessKeySecret() {
        return StrUtil.blankToDefault(properties.getBailianImageTranslate().getAccessKeySecret(),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET));
    }

    private ProviderResult failure(String code, String message, int httpStatus, long start) {
        return new ProviderResult()
                .setSuccess(false)
                .setProvider(providerName())
                .setErrorCode(StrUtil.blankToDefault(code, "Unknown"))
                .setErrorMessage(StrUtil.maxLength(StrUtil.blankToDefault(message, "图片翻译增强服务暂不可用"), 512))
                .setHttpStatus(httpStatus)
                .setCostMs(elapsedMs(start));
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || !node.has(key) || node.get(key).isNull()) {
            return "";
        }
        return node.get(key).asText("");
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = text(node, key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        JsonNode data = node != null ? node.path("Data") : null;
        if (data != null && !data.isMissingNode()) {
            for (String key : keys) {
                String value = text(data, key);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
        }
        data = node != null ? node.path("data") : null;
        if (data != null && !data.isMissingNode()) {
            for (String key : keys) {
                String value = text(data, key);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean hasAny(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return true;
            }
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                if (hasAny(child, keys)) {
                    return true;
                }
            }
        }
        return false;
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
}
