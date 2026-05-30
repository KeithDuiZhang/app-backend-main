package cn.iocoder.yudao.server.service.image;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ImageTranslationModels {

    private ImageTranslationModels() {
    }

    public enum Status {
        PENDING, PROCESSING, SUCCESS, FAILED, DEGRADED;

        public boolean isTerminal() {
            return this == SUCCESS || this == FAILED || this == DEGRADED;
        }
    }

    public enum DisplayMode {
        FINAL_IMAGE, APP_RENDERED_OVERLAY, TEXT_LIST, FAILED
    }

    public enum QualityStatus {
        PASS, WARN, FAIL_BUT_HAS_TEXT, FAIL
    }

    @Data
    public static class CreateTaskRespVO {
        private String taskId;
        private String status;
        private boolean cached;
        private String displayMode;
        private String message;
    }

    @Data
    public static class RetryReqVO {
        private boolean forceRefreshCache;
        private String preferProvider;
    }

    @Data
    public static class TaskStatusRespVO {
        private String taskId;
        private String status;
        private String displayMode;
        private String resultImageUrl;
        private List<TextItemRespVO> textItems = new ArrayList<>();
        private String warningMessage;
        private boolean cached;
        private String provider;
        private Double qualityScore;
        private String failReason;
        private LocalDateTime finishedAt;
        private LocalDateTime expireAt;
    }

    @Data
    public static class TextItemRespVO {
        private String sourceText;
        private String translatedText;
        private double confidence = 1.0d;
        private BoxRespVO box;
    }

    @Data
    public static class BoxRespVO {
        private double x;
        private double y;
        private double width;
        private double height;
        private List<Double> points = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class ProviderRateLimit {
        private int permits = 1;
        private long intervalMillis = 1000L;
    }

    @Data
    @Accessors(chain = true)
    public static class ProviderRequest {
        private Long taskId;
        private String taskNo;
        private Long userId;
        private String traceId;
        private String rawSha256;
        private String originalCosKey;
        private String enhancedCosKey;
        private byte[] originalImageBytes;
        private byte[] enhancedImageBytes;
        private String presignedImageUrl;
        private String sourceLang;
        private String targetLang;
        private String field;
        private String scene;
    }

    @Data
    @Accessors(chain = true)
    public static class ProviderResult {
        private boolean success;
        private String provider;
        private String requestId;
        private String providerTaskId;
        private String finalImageUrl;
        private byte[] finalImageBytes;
        private List<TextItemRespVO> textItems = new ArrayList<>();
        private String rawResponseJson;
        private String errorCode;
        private String errorMessage;
        private int httpStatus;
        private long costMs;
    }

    @Data
    @Accessors(chain = true)
    public static class ProcessedImage {
        private byte[] originalBytes;
        private byte[] enhancedBytes;
        private String originalExt;
        private String enhancedExt = "jpg";
        private int originalWidth;
        private int originalHeight;
        private int enhancedWidth;
        private int enhancedHeight;
        private long originalSizeBytes;
        private long enhancedSizeBytes;
    }

    @Data
    @Accessors(chain = true)
    public static class QualityResult {
        private QualityStatus status = QualityStatus.FAIL;
        private DisplayMode displayMode = DisplayMode.FAILED;
        private double qualityScore = 0d;
        private String warningMessage = "";
        private String failReason = "";
        private List<TextItemRespVO> textItems = new ArrayList<>();
        private String qualityJson = "{}";
    }
}
