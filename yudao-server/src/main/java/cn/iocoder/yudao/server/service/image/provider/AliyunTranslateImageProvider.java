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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AliyunTranslateImageProvider implements ImageTranslationProvider {

    public static final String NAME = "aliyun_translate_image";
    private static final String DEFAULT_ENDPOINT = "https://mt.cn-hangzhou.aliyuncs.com/";
    private static final String VERSION = "2018-10-12";
    private static final int MAX_TRANSIENT_RETRY = 2;

    @Resource
    private ImageTranslationProperties properties;
    @Resource
    private AppIntegrationConfigService integrationConfigService;
    @Resource
    private ProviderRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        ImageTranslationProperties.AliyunTranslateImage config = properties.getAliyunTranslateImage();
        return config.isEnabled()
                && StrUtil.isNotBlank(accessKeyId())
                && StrUtil.isNotBlank(accessKeySecret());
    }

    @Override
    public ProviderRateLimit rateLimit() {
        int qps = Math.max(1, properties.getAliyunTranslateImage().getQpsLimit());
        return new ProviderRateLimit().setPermits(1).setIntervalMillis(Math.max(1L, 1000L / qps));
    }

    @Override
    public ProviderResult translate(ProviderRequest request) {
        long start = System.nanoTime();
        if (!enabled()) {
            return failure("ProviderDisabled", "Aliyun TranslateImage is disabled or not configured", 0, start);
        }
        rateLimiter.acquire(providerName(), rateLimit());
        ProviderResult lastTransient = null;
        for (int retry = 0; retry <= MAX_TRANSIENT_RETRY; retry++) {
            ProviderResult result = callOnce(request, start);
            if (result.isSuccess() || !isTransient(result) || retry == MAX_TRANSIENT_RETRY) {
                return result;
            }
            lastTransient = result;
            sleep(1000L * (retry + 1));
        }
        return lastTransient != null ? lastTransient : failure("Unknown", "Aliyun TranslateImage failed", 0, start);
    }

    private ProviderResult callOnce(ProviderRequest request, long start) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            if (request.getEnhancedImageBytes() != null && request.getEnhancedImageBytes().length > 0) {
                params.put("ImageBase64", Base64.getEncoder().encodeToString(request.getEnhancedImageBytes()));
            } else if (StrUtil.isNotBlank(request.getPresignedImageUrl())) {
                params.put("ImageUrl", request.getPresignedImageUrl());
            } else {
                return failure("InvalidImage", "Image is empty", 0, start);
            }
            params.put("SourceLanguage", request.getSourceLang());
            params.put("TargetLanguage", request.getTargetLang());
            params.put("Field", StrUtil.blankToDefault(request.getField(), properties.getAliyunTranslateImage().getField()));
            if (properties.getAliyunTranslateImage().isNeedEditorData()) {
                params.put("Ext", "{\"needEditorData\":\"true\"}");
            }
            Map<String, String> signed = AliyunOpenApiSigner.sign("TranslateImage", VERSION, params,
                    accessKeyId(), accessKeySecret());
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(Math.max(5, properties.getAliyunTranslateImage().getTimeoutSeconds())))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(AliyunOpenApiSigner.formBody(signed), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = parseJson(response.body());
            String apiCode = text(root, "Code");
            if (StrUtil.isNotBlank(apiCode) && !"200".equals(apiCode)) {
                return failure(apiCode, text(root, "Message"), response.statusCode(), start)
                        .setRawResponseJson(response.body());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return failure("Http" + response.statusCode(), "Aliyun TranslateImage HTTP failure", response.statusCode(), start)
                        .setRawResponseJson(response.body());
            }
            JsonNode data = root.path("Data");
            String finalImageUrl = text(data, "FinalImageUrl");
            String templateJson = firstText(data, "TemplateJson", "EditorData", "EditorDataJson");
            if (StrUtil.isBlank(finalImageUrl) && StrUtil.isBlank(templateJson)) {
                return failure("EmptyResult", "Aliyun TranslateImage returned empty result", response.statusCode(), start)
                        .setRequestId(text(root, "RequestId"))
                        .setRawResponseJson(response.body());
            }
            return new ProviderResult()
                    .setSuccess(true)
                    .setProvider(providerName())
                    .setRequestId(text(root, "RequestId"))
                    .setFinalImageUrl(finalImageUrl)
                    .setRawResponseJson(response.body())
                    .setHttpStatus(response.statusCode())
                    .setCostMs(elapsedMs(start));
        } catch (Exception ex) {
            return failure("RequestFailed", "Aliyun TranslateImage request failed", 0, start);
        }
    }

    private ProviderResult failure(String code, String message, int httpStatus, long start) {
        return new ProviderResult()
                .setSuccess(false)
                .setProvider(providerName())
                .setErrorCode(StrUtil.blankToDefault(code, "Unknown"))
                .setErrorMessage(StrUtil.maxLength(StrUtil.blankToDefault(message, "图片翻译服务暂不可用"), 512))
                .setHttpStatus(httpStatus)
                .setCostMs(elapsedMs(start));
    }

    private boolean isTransient(ProviderResult result) {
        int status = result.getHttpStatus();
        String code = StrUtil.blankToDefault(result.getErrorCode(), "");
        return status == 500 || status == 502 || status == 503 || status == 504
                || code.contains("ServiceUnavailable")
                || code.contains("InternalError")
                || code.contains("ServerError")
                || code.startsWith("Throttling");
    }

    private JsonNode parseJson(String value) throws Exception {
        if (StrUtil.isBlank(value)) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(value);
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
        return "";
    }

    private String endpoint() {
        return first(properties.getAliyunTranslateImage().getEndpoint(),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ENDPOINT),
                DEFAULT_ENDPOINT);
    }

    private String accessKeyId() {
        return first(integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID),
                properties.getAliyunTranslateImage().getAccessKeyId());
    }

    private String accessKeySecret() {
        return first(integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET),
                properties.getAliyunTranslateImage().getAccessKeySecret());
    }

    private String first(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
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
}
