package cn.iocoder.yudao.server.service.image;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import lombok.Data;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class ImageTranslationStorageService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Resource
    private ImageTranslationProperties properties;
    @Resource
    private AppIntegrationConfigService integrationConfigService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String originalKey(String rawSha256, String ext) {
        return basePath(rawSha256) + "/original." + safeExt(ext);
    }

    public String enhancedKey(String rawSha256, String ext) {
        return basePath(rawSha256) + "/enhanced." + safeExt(ext);
    }

    public String providerResultImageKey(String rawSha256, String provider, String sourceLang, String targetLang, String ext) {
        return providerPath(rawSha256, provider, sourceLang, targetLang) + "/final-image." + safeExt(ext);
    }

    public String providerResultJsonKey(String rawSha256, String provider, String sourceLang, String targetLang) {
        String name = ("bailian_anytrans".equals(provider) || "qwen_mt_image".equals(provider))
                ? "result.json" : "editor-data.json";
        return providerPath(rawSha256, provider, sourceLang, targetLang) + "/" + name;
    }

    public String providerQualityJsonKey(String rawSha256, String provider, String sourceLang, String targetLang) {
        return providerPath(rawSha256, provider, sourceLang, targetLang) + "/quality.json";
    }

    public String directUploadKey(Long userId, String fileName, String contentType) {
        String prefix = normalizePrefix(properties.getCos().getKeyPrefix());
        String day = LocalDate.now().format(DAY_FORMATTER);
        String safeUser = userId == null || userId <= 0 ? "anonymous" : String.valueOf(userId);
        return prefix + "direct-upload/" + day + "/user-" + safeUser + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + resolveExt(fileName, contentType);
    }

    public boolean isDirectUploadKeyForUser(Long userId, String objectKey) {
        if (StrUtil.isBlank(objectKey) || userId == null || userId <= 0) {
            return false;
        }
        String key = StrUtil.removePrefix(objectKey.trim(), "/");
        String prefix = normalizePrefix(properties.getCos().getKeyPrefix()) + "direct-upload/";
        return key.startsWith(prefix) && key.contains("/user-" + userId + "/");
    }

    public void uploadBytes(String objectKey, byte[] bytes, String contentType) {
        RuntimeCosConfig config = runtimeConfig();
        try (S3Client client = createS3Client(config)) {
            client.putObject(PutObjectRequest.builder()
                            .bucket(config.getBucket())
                            .key(objectKey)
                            .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream"))
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (Exception ex) {
            throw ServiceExceptionUtil.invalidParamException("图片存储失败，请稍后重试");
        }
    }

    public void deleteObjectQuietly(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            return;
        }
        try {
            RuntimeCosConfig config = runtimeConfig();
            try (S3Client client = createS3Client(config)) {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(objectKey)
                    .build());
            }
        } catch (Exception ignored) {
        }
    }

    public byte[] getObjectBytes(String objectKey) {
        RuntimeCosConfig config = runtimeConfig();
        try (S3Client client = createS3Client(config)) {
            ResponseBytes<GetObjectResponse> bytes = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(objectKey)
                    .build());
            return bytes.asByteArray();
        } catch (Exception ex) {
            throw ServiceExceptionUtil.invalidParamException("图片读取失败，请稍后重试");
        }
    }

    public String presignGetUrl(String objectKey, int minutes) {
        RuntimeCosConfig config = runtimeConfig();
        int ttlMinutes = Math.max(1, Math.min(minutes, 24 * 60));
        try (S3Presigner presigner = createPresigner(config)) {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(ttlMinutes))
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(config.getBucket())
                                    .key(objectKey)
                                    .build())
                            .build())
                    .url()
                    .toString();
        }
    }

    public String presignPutUrl(String objectKey, String contentType, int minutes) {
        RuntimeCosConfig config = runtimeConfig();
        int ttlMinutes = Math.max(1, Math.min(minutes, 60));
        String safeContentType = StrUtil.blankToDefault(contentType, "image/jpeg");
        try (S3Presigner presigner = createPresigner(config)) {
            return presigner.presignPutObject(PutObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(ttlMinutes))
                            .putObjectRequest(PutObjectRequest.builder()
                                    .bucket(config.getBucket())
                                    .key(objectKey)
                                    .contentType(safeContentType)
                                    .build())
                            .build())
                    .url()
                    .toString();
        }
    }

    public byte[] downloadUrl(String url, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
            throw new IOException("Remote image unavailable");
        }
        return response.body();
    }

    public int presignedUrlExpireMinutes() {
        return Math.max(1, properties.getCos().getPresignedUrlExpireMinutes());
    }

    private String basePath(String rawSha256) {
        String sha = StrUtil.blankToDefault(rawSha256, "unknown");
        String prefix = normalizePrefix(properties.getCos().getKeyPrefix());
        String day = LocalDate.now().format(DAY_FORMATTER);
        String shard = sha.length() >= 2 ? sha.substring(0, 2) : "xx";
        return prefix + day + "/" + shard + "/" + sha;
    }

    private String providerPath(String rawSha256, String provider, String sourceLang, String targetLang) {
        return basePath(rawSha256) + "/" + provider + "/" + safePath(sourceLang) + "_" + safePath(targetLang);
    }

    private RuntimeCosConfig runtimeConfig() {
        ImageTranslationProperties.Cos cos = properties.getCos();
        String secretId = first(integrationConfigService.getPlain(AppIntegrationConfigService.COS_SECRET_ID), cos.getSecretId());
        String secretKey = first(integrationConfigService.getPlain(AppIntegrationConfigService.COS_SECRET_KEY), cos.getSecretKey());
        String bucket = first(integrationConfigService.getPlain(AppIntegrationConfigService.COS_BUCKET), cos.getBucket());
        String region = first(integrationConfigService.getPlain(AppIntegrationConfigService.COS_REGION), cos.getRegion());
        if (StrUtil.hasBlank(secretId, secretKey, bucket, region)) {
            throw ServiceExceptionUtil.invalidParamException("图片存储暂未开通，请稍后再试");
        }
        RuntimeCosConfig config = new RuntimeCosConfig();
        config.setSecretId(secretId);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        config.setRegion(region);
        config.setEndpoint(first(integrationConfigService.getPlain(AppIntegrationConfigService.COS_ENDPOINT),
                cos.getEndpoint(), "https://cos." + region + ".myqcloud.com"));
        return config;
    }

    private S3Client createS3Client(RuntimeCosConfig config) {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getSecretId(), config.getSecretKey())))
                .region(Region.of(config.getRegion()))
                .endpointOverride(URI.create(config.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private S3Presigner createPresigner(RuntimeCosConfig config) {
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getSecretId(), config.getSecretKey())))
                .region(Region.of(config.getRegion()))
                .endpointOverride(URI.create(config.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private String normalizePrefix(String value) {
        String prefix = StrUtil.blankToDefault(value, "image-translation");
        prefix = StrUtil.removePrefix(prefix, "/");
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private String safePath(String value) {
        return StrUtil.blankToDefault(value, "auto")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "-");
    }

    private String safeExt(String ext) {
        String value = StrUtil.blankToDefault(ext, "jpg").toLowerCase(Locale.ROOT);
        return value.matches("jpg|jpeg|png|webp|bmp|json") ? ("jpeg".equals(value) ? "jpg" : value) : "jpg";
    }

    private String resolveExt(String filename, String contentType) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot >= 0 && dot < lower.length() - 1) {
            return safeExt(lower.substring(dot + 1));
        }
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("png")) {
            return "png";
        }
        if (type.contains("webp")) {
            return "webp";
        }
        if (type.contains("bmp")) {
            return "bmp";
        }
        return "jpg";
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

    @Data
    private static class RuntimeCosConfig {
        private String secretId;
        private String secretKey;
        private String bucket;
        private String region;
        private String endpoint;
    }
}
