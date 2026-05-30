package cn.iocoder.yudao.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "image-translation")
public class ImageTranslationProperties {

    private boolean enabled = true;
    private String defaultProvider = "qwen_mt_image";
    private boolean cacheEnabled = true;
    private int cacheTtlDays = 30;
    private int resultRetentionDays = 30;
    private int uploadMaxSizeMb = 20;
    private String pipelineVersion = "v1";
    private String preprocessVersion = "v1";
    private String renderVersion = "v1";
    private String queueType = "redis";
    private Cos cos = new Cos();
    private AliyunTranslateImage aliyunTranslateImage = new AliyunTranslateImage();
    private BailianImageTranslate bailianImageTranslate = new BailianImageTranslate();
    private Quality quality = new Quality();

    @Data
    public static class Cos {
        private boolean enabled = false;
        private String bucket = "";
        private String region = "";
        private String secretId = "";
        private String secretKey = "";
        private String endpoint = "";
        private String domain = "";
        private boolean privateBucket = true;
        private int presignedUrlExpireMinutes = 30;
        private String keyPrefix = "image-translation";
    }

    @Data
    public static class AliyunTranslateImage {
        private boolean enabled = false;
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String endpoint = "";
        private String region = "";
        private String field = "general";
        private boolean needEditorData = true;
        private int timeoutSeconds = 30;
        private int qpsLimit = 5;
    }

    @Data
    public static class BailianImageTranslate {
        private boolean enabled = true;
        private String providerType = "qwen_mt_image";
        private String workspaceId = "";
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String apiKey = "";
        private String endpoint = "https://dashscope.aliyuncs.com";
        private String scene = "general";
        private int rpmLimit = 5;
        private int timeoutSeconds = 90;
        private int pollIntervalSeconds = 3;
        private int maxPollTimes = 20;
    }

    @Data
    public static class Quality {
        private boolean enableQualityCheck = true;
        private int minBoxCount = 1;
        private double maxOverlapRatio = 0.25;
        private double minConfidence = 0.5;
    }
}
