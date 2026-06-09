package cn.iocoder.yudao.server.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRateLimit;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRequest;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TextItemRespVO;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AliyunOcrTextImageFallbackProvider implements ImageTranslationProvider {

    public static final String NAME = "aliyun_ocr_text";
    private static final int MAX_TEXT_ITEMS = 40;

    @Resource
    private ImageTranslationProperties properties;
    @Resource
    private ProviderRateLimiter rateLimiter;
    @Resource
    private AppIntegrationConfigService integrationConfigService;
    @Resource
    private AliyunImageTextOcrClient ocrClient;
    @Resource
    private AliyunImageTextTranslateClient translateClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        return hasOcrCredential() && hasMtCredential();
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
            return failure("ProviderDisabled", "Aliyun OCR text fallback is disabled or not configured", 0, start);
        }
        byte[] imageBytes = firstBytes(request.getEnhancedImageBytes(), request.getOriginalImageBytes());
        if (imageBytes.length == 0) {
            return failure("InvalidImage", "Image is empty", 0, start);
        }
        rateLimiter.acquire(providerName(), rateLimit());
        try {
            AliyunImageTextOcrClient.OcrDetail detail = ocrClient.recognize(imageBytes);
            List<TextItemRespVO> items = normalizeItems(detail);
            if (items.isEmpty()) {
                return failure("OcrNoText", "OCR did not return text", 200, start)
                        .setRequestId(detail.getRequestId());
            }
            translateItems(items, request.getSourceLang(), request.getTargetLang());
            return new ProviderResult()
                    .setSuccess(true)
                    .setProvider(providerName())
                    .setRequestId(detail.getRequestId())
                    .setProviderTaskId(StrUtil.blankToDefault(request.getTaskNo(), "ocr-text"))
                    .setTextItems(items)
                    .setRawResponseJson(toRawJson(detail, items))
                    .setHttpStatus(200)
                    .setCostMs(elapsedMs(start));
        } catch (RuntimeException ex) {
            return failure("OcrTextFallbackFailed", "Aliyun OCR text fallback failed", 0, start);
        }
    }

    private List<TextItemRespVO> normalizeItems(AliyunImageTextOcrClient.OcrDetail detail) {
        List<TextItemRespVO> items = new ArrayList<>();
        if (detail != null && detail.getItems() != null) {
            for (TextItemRespVO item : detail.getItems()) {
                if (item != null && StrUtil.isNotBlank(item.getSourceText())) {
                    items.add(item);
                    if (items.size() >= MAX_TEXT_ITEMS) {
                        break;
                    }
                }
            }
        }
        if (items.isEmpty() && detail != null && StrUtil.isNotBlank(detail.getRawText())) {
            TextItemRespVO item = new TextItemRespVO();
            item.setSourceText(detail.getRawText().trim());
            item.setConfidence(1.0d);
            items.add(item);
        }
        return items;
    }

    private void translateItems(List<TextItemRespVO> items, String sourceLang, String targetLang) {
        for (TextItemRespVO item : items) {
            String sourceText = StrUtil.trimToEmpty(item.getSourceText());
            if (sourceText.isEmpty()) {
                continue;
            }
            item.setTranslatedText(translateClient.translateGeneral(sourceLang, targetLang, sourceText));
        }
    }

    private String toRawJson(AliyunImageTextOcrClient.OcrDetail detail, List<TextItemRespVO> items) {
        try {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("provider", NAME);
            raw.put("requestId", detail != null ? detail.getRequestId() : "");
            raw.put("rawTextLength", detail != null && detail.getRawText() != null ? detail.getRawText().length() : 0);
            raw.put("textItems", items);
            return objectMapper.writeValueAsString(raw);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private boolean hasOcrCredential() {
        return hasAny(AppIntegrationConfigService.ALIYUN_OCR_ACCESS_KEY_ID,
                AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID)
                && hasAny(AppIntegrationConfigService.ALIYUN_OCR_ACCESS_KEY_SECRET,
                AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET);
    }

    private boolean hasMtCredential() {
        return hasAny(AppIntegrationConfigService.ALIYUN_MT_ACCESS_KEY_ID,
                AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID)
                && hasAny(AppIntegrationConfigService.ALIYUN_MT_ACCESS_KEY_SECRET,
                AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET);
    }

    private boolean hasAny(String... keys) {
        if (keys == null) {
            return false;
        }
        for (String key : keys) {
            if (StrUtil.isNotBlank(integrationConfigService.getPlain(key))) {
                return true;
            }
        }
        return false;
    }

    private byte[] firstBytes(byte[]... values) {
        if (values == null) {
            return new byte[0];
        }
        for (byte[] value : values) {
            if (value != null && value.length > 0) {
                return value;
            }
        }
        return new byte[0];
    }

    private ProviderResult failure(String code, String message, int httpStatus, long start) {
        return new ProviderResult()
                .setSuccess(false)
                .setProvider(providerName())
                .setErrorCode(StrUtil.blankToDefault(code, "Unknown"))
                .setErrorMessage(StrUtil.maxLength(StrUtil.blankToDefault(message,
                        "Aliyun OCR text fallback is unavailable"), 512))
                .setHttpStatus(httpStatus)
                .setCostMs(elapsedMs(start));
    }

    private long elapsedMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }
}
