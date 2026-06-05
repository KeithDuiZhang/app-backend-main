package cn.iocoder.yudao.server.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
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
import java.util.Map;

@Slf4j
@Service
public class AliyunImageTextTranslateClient {

    private static final String VERSION = "2018-10-12";
    private static final String DEFAULT_REGION = "cn-hangzhou";

    @Resource
    private AppIntegrationConfigService integrationConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String translateGeneral(String sourceLanguage, String targetLanguage, String sourceText) {
        String text = StrUtil.trimToEmpty(sourceText);
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String accessKeyId = first(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_MT_ACCESS_KEY_ID),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID));
        String accessKeySecret = first(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_MT_ACCESS_KEY_SECRET),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET));
        if (StrUtil.hasBlank(accessKeyId, accessKeySecret)) {
            throw ServiceExceptionUtil.invalidParamException("文字翻译暂未开通，请稍后再试");
        }
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("FormatType", "text");
            params.put("Scene", "general");
            params.put("SourceLanguage", StrUtil.blankToDefault(sourceLanguage, "auto"));
            params.put("TargetLanguage", StrUtil.trimToEmpty(targetLanguage));
            params.put("SourceText", text);
            Map<String, String> signed = AliyunOpenApiSigner.sign(
                    "TranslateGeneral", VERSION, params, accessKeyId, accessKeySecret);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(AliyunOpenApiSigner.formBody(signed), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw userFailure("Http" + response.statusCode(), response.body());
            }
            return parseTranslatedText(response.body());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("image text MT request failed type={}", ex.getClass().getSimpleName());
            throw ServiceExceptionUtil.invalidParamException("文字翻译失败，请稍后重试");
        }
    }

    private String parseTranslatedText(String body) throws Exception {
        JsonNode root = StrUtil.isBlank(body) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        String code = text(root, "Code");
        if (StrUtil.isNotBlank(code) && !"200".equals(code)) {
            throw userFailure(code, text(root, "Message"));
        }
        JsonNode data = root.path("Data");
        String translated = firstText(data, "Translated", "TranslatedText");
        if (StrUtil.isBlank(translated)) {
            throw ServiceExceptionUtil.invalidParamException("译文暂不可用，请稍后重试");
        }
        return translated.trim();
    }

    private RuntimeException userFailure(String code, String message) {
        String normalized = StrUtil.blankToDefault(code, "").toLowerCase();
        log.warn("image text MT failed code={}", StrUtil.maxLength(StrUtil.blankToDefault(code, "Unknown"), 80));
        if (normalized.contains("accesskey") || normalized.contains("signature")
                || normalized.contains("nopermission") || normalized.contains("forbidden")
                || normalized.contains("servicedisabled") || normalized.contains("accountnotactivated")) {
            return ServiceExceptionUtil.invalidParamException("文字翻译暂未开通，请稍后再试");
        }
        if (normalized.contains("language") || normalized.contains("sourcelanguage")
                || normalized.contains("targetlanguage")) {
            return ServiceExceptionUtil.invalidParamException("当前语言方向暂不支持提取文字翻译");
        }
        if (normalized.contains("throttling") || normalized.contains("ratelimit")) {
            return ServiceExceptionUtil.invalidParamException("文字翻译请求较多，请稍后重试");
        }
        return ServiceExceptionUtil.invalidParamException(StrUtil.isNotBlank(message)
                ? "文字翻译失败，请稍后重试"
                : "文字翻译服务暂不可用，请稍后重试");
    }

    private String endpoint() {
        String region = StrUtil.blankToDefault(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_TRANSLATION_REGION),
                DEFAULT_REGION).trim();
        return "https://mt." + region + ".aliyuncs.com/";
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

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || !node.has(key) || node.get(key).isNull()) {
            return "";
        }
        return node.get(key).asText("");
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
}
