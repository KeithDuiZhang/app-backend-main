package cn.iocoder.yudao.server.service.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService.CosConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class AppOfflineModelService {

    private static final String COMPONENT_PACKS_RESOURCE = "classpath:offline-models/component-packs.json";
    private static final String BUSINESS_PACKS_RESOURCE = "classpath:offline-models/business-packs.json";
    private static final int CATALOG_SCHEMA_VERSION = 2;
    private static final String DEFAULT_LOCAL_PUBLISH_ROOT =
            "D:/Code_Project/md_CN_model_repo/published/model-repo/cn/1.0.0";
    private static final String LOCAL_REPO_GUARD = "D:/Code_Project/md_CN_model_repo";
    private static final Set<String> REALTIME_TRANSLATION_LANGUAGES =
            Set.of("zh", "en", "ja", "ko", "th", "vi", "id");

    @Resource
    private ResourceLoader resourceLoader;
    @Resource
    private AppIntegrationConfigService integrationConfigService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<ComponentPackRespVO> componentPacks = Collections.emptyList();
    private List<BusinessPackRespVO> businessPacks = Collections.emptyList();
    private Map<String, ComponentPackRespVO> componentPacksById = Collections.emptyMap();
    private Map<String, BusinessPackRespVO> businessPacksById = Collections.emptyMap();
    private List<TranslationModelRespVO> catalogModels = Collections.emptyList();

    @PostConstruct
    public void init() throws IOException {
        componentPacks = readJsonResource(COMPONENT_PACKS_RESOURCE, new TypeReference<>() {
        });
        businessPacks = readJsonResource(BUSINESS_PACKS_RESOURCE, new TypeReference<>() {
        });

        Map<String, ComponentPackRespVO> componentMap = new LinkedHashMap<>();
        for (ComponentPackRespVO component : componentPacks) {
            componentMap.put(component.getPackId(), component);
        }
        componentPacksById = Collections.unmodifiableMap(componentMap);

        Map<String, BusinessPackRespVO> businessMap = new LinkedHashMap<>();
        for (BusinessPackRespVO businessPack : businessPacks) {
            businessMap.put(businessPack.getPackId(), businessPack);
        }
        businessPacksById = Collections.unmodifiableMap(businessMap);
        catalogModels = Collections.unmodifiableList(buildCatalogModels(componentPacksById));
    }

    public ModelCatalogRespVO getCatalog() {
        validateCosRuntimeConfigured();
        CosConfig cosConfig = integrationConfigService.getCosRuntimeConfig();
        ModelCatalogRespVO respVO = new ModelCatalogRespVO();
        respVO.setSchemaVersion(CATALOG_SCHEMA_VERSION);
        respVO.setReleaseVersion("1.0.0");
        respVO.setBaseUrl(buildBaseUrl(cosConfig));
        respVO.setDownloadRequiresSignedUrl(true);
        respVO.setBusinessPacks(businessPacks);
        respVO.setComponents(componentPacks);
        respVO.setModels(catalogModels);
        return respVO;
    }

    public DownloadUrlRespVO createDownloadUrls(Long userId, DownloadUrlReqVO reqVO) {
        validateCosRuntimeConfigured();
        if (!hasActiveOfflineMembership(userId)) {
            throw ServiceExceptionUtil.invalidParamException("请先开通离线会员后再下载模型");
        }
        Set<String> componentIds = resolveRequestedComponentIds(reqVO);
        CosConfig cosConfig = integrationConfigService.getCosRuntimeConfig();
        int ttlSeconds = parseTtlSeconds(cosConfig.getSignedUrlTtlSeconds());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        List<SignedComponentRespVO> signedComponents = new ArrayList<>();
        try (S3Presigner presigner = createPresigner(cosConfig)) {
            for (String componentId : componentIds) {
                ComponentPackRespVO component = componentPacksById.get(componentId);
                String objectKey = buildObjectKey(cosConfig.getPrefix(), component.getUrl());
                String signedUrl = presigner.presignGetObject(GetObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                                .getObjectRequest(GetObjectRequest.builder()
                                        .bucket(cosConfig.getBucket())
                                        .key(objectKey)
                                        .build())
                                .build())
                        .url()
                        .toString();

                SignedComponentRespVO signed = new SignedComponentRespVO();
                signed.setPackId(component.getPackId());
                signed.setVersion(component.getVersion());
                signed.setType(component.getType());
                signed.setInstallPath(component.getInstallPath());
                signed.setObjectKey(objectKey);
                signed.setSizeBytes(component.getSizeBytes());
                signed.setSha256(component.getSha256());
                signed.setDownloadUrl(signedUrl);
                signed.setRequiredFiles(component.getRequiredFiles());
                signedComponents.add(signed);
            }
        }

        DownloadUrlRespVO respVO = new DownloadUrlRespVO();
        respVO.setExpiresAt(expiresAt);
        respVO.setExpiresInSeconds(ttlSeconds);
        respVO.setComponents(signedComponents);
        return respVO;
    }

    private void validateCosRuntimeConfigured() {
        if (!integrationConfigService.isCosRuntimeConfigured()) {
            throw ServiceExceptionUtil.invalidParamException("离线模型下载暂未开通，请稍后再试");
        }
    }

    public UploadPublishRespVO uploadPublishedRepository(UploadPublishReqVO reqVO) throws IOException {
        String requestedRoot = reqVO != null ? reqVO.getLocalRoot() : null;
        Path root = Paths.get(StrUtil.blankToDefault(requestedRoot, DEFAULT_LOCAL_PUBLISH_ROOT))
                .toAbsolutePath()
                .normalize();
        Path guard = Paths.get(LOCAL_REPO_GUARD).toAbsolutePath().normalize();
        if (!root.startsWith(guard) || !Files.isDirectory(root)) {
            throw ServiceExceptionUtil.invalidParamException("本地模型发布目录不正确");
        }
        if (!Files.exists(root.resolve("index.json")) || !Files.exists(root.resolve("component-packs.json"))) {
            throw ServiceExceptionUtil.invalidParamException("本地模型发布目录不完整");
        }

        CosConfig cosConfig = integrationConfigService.getCosRuntimeConfig();
        UploadPublishRespVO respVO = new UploadPublishRespVO();
        respVO.setBucket(cosConfig.getBucket());
        respVO.setRegion(cosConfig.getRegion());
        respVO.setPrefix(normalizePrefix(cosConfig.getPrefix()));

        try (S3Client client = createS3Client(cosConfig);
             Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            for (Path file : files) {
                String relativePath = root.relativize(file).toString().replace('\\', '/');
                String objectKey = buildObjectKey(cosConfig.getPrefix(), relativePath);
                long sizeBytes = Files.size(file);
                client.putObject(PutObjectRequest.builder()
                                .bucket(cosConfig.getBucket())
                                .key(objectKey)
                                .contentType(contentType(relativePath))
                                .contentLength(sizeBytes)
                                .build(),
                        RequestBody.fromFile(file));
                respVO.setUploadedFiles(respVO.getUploadedFiles() + 1);
                respVO.setUploadedBytes(respVO.getUploadedBytes() + sizeBytes);
            }
            respVO.setVerifiedFiles(respVO.getUploadedFiles());
            respVO.setVerifiedBytes(respVO.getUploadedBytes());
            respVO.setKeyJsonObjects(readKeyJsonObjects(root, cosConfig));
        }
        respVO.setFinishedAt(LocalDateTime.now());
        return respVO;
    }

    private boolean hasActiveOfflineMembership(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM app_offline_membership
                    WHERE user_id = ? AND status = 'active' AND deleted = 0
                      AND (expired_at IS NULL OR expired_at > NOW())
                    """, Integer.class, userId);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private List<KeyJsonObjectRespVO> readKeyJsonObjects(Path root, CosConfig cosConfig) throws IOException {
        List<KeyJsonObjectRespVO> objects = new ArrayList<>();
        for (String name : List.of("index.json", "component-packs.json", "business-packs.json")) {
            Path file = root.resolve(name);
            if (!Files.exists(file)) {
                continue;
            }
            KeyJsonObjectRespVO object = new KeyJsonObjectRespVO();
            object.setObjectKey(buildObjectKey(cosConfig.getPrefix(), name));
            object.setSizeBytes(Files.size(file));
            object.setSha256(sha256Hex(file));
            objects.add(object);
        }
        return objects;
    }

    private String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte value : digest.digest()) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private Set<String> resolveRequestedComponentIds(DownloadUrlReqVO reqVO) {
        if (reqVO == null) {
            throw ServiceExceptionUtil.invalidParamException("Missing download request");
        }
        LinkedHashSet<String> componentIds = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(reqVO.getBusinessPackId())) {
            BusinessPackRespVO businessPack = businessPacksById.get(reqVO.getBusinessPackId());
            if (businessPack == null) {
                throw ServiceExceptionUtil.invalidParamException("离线模型包暂不可用，请稍后重试");
            }
            componentIds.addAll(businessPack.getComponents());
        }
        if (CollUtil.isNotEmpty(reqVO.getComponentIds())) {
            componentIds.addAll(reqVO.getComponentIds());
        }
        if (componentIds.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("请选择需要下载的离线模型包");
        }
        for (String componentId : componentIds) {
            ComponentPackRespVO component = componentPacksById.get(componentId);
            if (component == null || StrUtil.isBlank(component.getUrl())) {
                throw ServiceExceptionUtil.invalidParamException("离线模型包暂不可用，请稍后重试");
            }
        }
        return componentIds;
    }

    private S3Presigner createPresigner(CosConfig cosConfig) {
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cosConfig.getSecretId(), cosConfig.getSecretKey())))
                .region(Region.of(cosConfig.getRegion()))
                .endpointOverride(URI.create(cosConfig.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private S3Client createS3Client(CosConfig cosConfig) {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cosConfig.getSecretId(), cosConfig.getSecretKey())))
                .region(Region.of(cosConfig.getRegion()))
                .endpointOverride(URI.create(cosConfig.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private String buildBaseUrl(CosConfig cosConfig) {
        return StrUtil.removeSuffix(cosConfig.getDomain(), "/") + "/" + normalizePrefix(cosConfig.getPrefix());
    }

    private String buildObjectKey(String prefix, String relativePath) {
        return normalizePrefix(prefix) + StrUtil.removePrefix(relativePath, "/");
    }

    private String normalizePrefix(String prefix) {
        String value = StrUtil.blankToDefault(prefix, "");
        value = StrUtil.removePrefix(value, "/");
        return value.endsWith("/") || value.isEmpty() ? value : value + "/";
    }

    private int parseTtlSeconds(String value) {
        try {
            int ttl = Integer.parseInt(StrUtil.blankToDefault(value, "3600"));
            return Math.max(60, Math.min(ttl, 24 * 60 * 60));
        } catch (NumberFormatException ex) {
            return 3600;
        }
    }

    private String contentType(String relativePath) {
        String value = relativePath.toLowerCase(Locale.ROOT);
        if (value.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (value.endsWith(".zip")) {
            return "application/zip";
        }
        if (value.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private static List<TranslationModelRespVO> buildCatalogModels(Map<String, ComponentPackRespVO> componentsById) {
        List<TranslationModelRespVO> models = new ArrayList<>();
        for (String[] pair : List.of(
                new String[]{"en", "zh"},
                new String[]{"zh", "en"},
                new String[]{"en", "ja"},
                new String[]{"zh", "ja"},
                new String[]{"ja", "zh"},
                new String[]{"ja", "en"},
                new String[]{"ko", "en"},
                new String[]{"th", "en"},
                new String[]{"en", "th"},
                new String[]{"vi", "en"},
                new String[]{"en", "vi"},
                new String[]{"id", "en"},
                new String[]{"en", "id"},
                new String[]{"ms", "en"},
                new String[]{"en", "ms"},
                new String[]{"fr", "en"},
                new String[]{"en", "fr"},
                new String[]{"de", "en"},
                new String[]{"en", "de"},
                new String[]{"es", "en"},
                new String[]{"en", "es"},
                new String[]{"ru", "en"},
                new String[]{"en", "ru"},
                new String[]{"zh", "ko"},
                new String[]{"ko", "zh"},
                new String[]{"zh", "vi"},
                new String[]{"zh", "ms"},
                new String[]{"zh", "de"},
                new String[]{"de", "zh"},
                new String[]{"ja", "vi"},
                new String[]{"ja", "es"},
                new String[]{"ja", "fr"},
                new String[]{"ja", "ru"},
                new String[]{"de", "vi"},
                new String[]{"ru", "vi"},
                new String[]{"fr", "de"},
                new String[]{"de", "fr"},
                new String[]{"fr", "es"},
                new String[]{"ru", "fr"},
                new String[]{"de", "es"},
                new String[]{"es", "de"},
                new String[]{"ru", "es"},
                new String[]{"ja", "ms"},
                new String[]{"ko", "fr"},
                new String[]{"th", "fr"},
                new String[]{"id", "fr"},
                new String[]{"ms", "fr"},
                new String[]{"ms", "de"},
                new String[]{"fr", "vi"},
                new String[]{"fr", "id"},
                new String[]{"fr", "ms"},
                new String[]{"de", "ms"},
                new String[]{"es", "id"},
                new String[]{"id", "es"})) {
            addOpusModel(models, pair[0], pair[1], componentsById);
        }

        TranslationModelRespVO m2m100 = new TranslationModelRespVO();
        m2m100.setModelId("text-m2m100-418m-int8");
        m2m100.setFamily("m2m100");
        m2m100.setEngine("M2M100");
        m2m100.setSourceLanguageCode("*");
        m2m100.setTargetLanguageCode("*");
        m2m100.setComponentPackId("text-m2m100-418m-int8");
        m2m100.setArtifactPaths(Map.of(
                "model", "model.int8.onnx",
                "sentencepiece", "sentencepiece.model",
                "metadata", "metadata.json",
                "manifest", "manifest.json",
                "checksum", "checksum.sha256"));
        m2m100.setModelFormat("onnx");
        m2m100.setQuantization("int8");
        m2m100.setSizeBytes(0L);
        m2m100.setSha256("");
        m2m100.setSupportsText(true);
        m2m100.setSupportsOcrBlock(true);
        m2m100.setSupportsRealtime(false);
        m2m100.setSupportsBatch(true);
        m2m100.setPriority(60);
        m2m100.setLicense("TBD");
        m2m100.setAttribution("M2M100 metadata placeholder");
        m2m100.setRequiredPlan("offline_membership");
        m2m100.setRecommendedDeviceLevel("HIGH");
        m2m100.setMinAndroidSdk(23);
        m2m100.setMinIosVersion("13.0");
        m2m100.setCapabilityStatus("planned");
        models.add(m2m100);

        TranslationModelRespVO hymt = new TranslationModelRespVO();
        hymt.setModelId("text-hymt-enhance");
        hymt.setFamily("hy-mt");
        hymt.setEngine("HY-MT");
        hymt.setSourceLanguageCode("*");
        hymt.setTargetLanguageCode("*");
        hymt.setComponentPackId("text-hymt-core");
        hymt.setArtifactPaths(Map.of(
                "model", "Hy-MT1.5-1.8B-2bit.gguf",
                "metadata", "metadata.json",
                "manifest", "manifest.json",
                "checksum", "checksum.sha256"));
        hymt.setModelFormat("gguf");
        hymt.setQuantization("2bit");
        hymt.setSizeBytes(594648072L);
        hymt.setSha256("01abd49939e4c359a46d408614ecc6eabee9ff9e48678088354974ed5652d604");
        hymt.setSupportsText(true);
        hymt.setSupportsOcrBlock(true);
        hymt.setSupportsRealtime(false);
        hymt.setSupportsBatch(false);
        hymt.setPriority(100);
        hymt.setLicense("TBD");
        hymt.setAttribution("HY-MT enhancement package");
        hymt.setRequiredPlan("offline_membership");
        hymt.setRecommendedDeviceLevel("HIGH");
        hymt.setMinAndroidSdk(23);
        hymt.setMinIosVersion("13.0");
        hymt.setCapabilityStatus("manual-enhance");
        models.add(hymt);
        return Collections.unmodifiableList(models);
    }

    private static void addOpusModel(List<TranslationModelRespVO> models,
                                     String sourceLanguage,
                                     String targetLanguage,
                                     Map<String, ComponentPackRespVO> componentsById) {
        String modelId = "text-opus-marian-" + sourceLanguage + "-" + targetLanguage;
        ComponentPackRespVO component = componentsById.get(modelId);
        boolean published = isPublishedComponent(component);
        TranslationModelRespVO model = new TranslationModelRespVO();
        model.setModelId(modelId);
        model.setFamily("opus-marian");
        model.setEngine("OPUS-Marian");
        model.setSourceLanguageCode(sourceLanguage);
        model.setTargetLanguageCode(targetLanguage);
        model.setComponentPackId(modelId);
        model.setArtifactPaths(Map.of(
                "model", "model.int8.onnx",
                "sourceTokenizer", "source.spm",
                "targetTokenizer", "target.spm",
                "vocab", "vocab.json",
                "metadata", "metadata.json",
                "manifest", "manifest.json",
                "checksum", "checksum.sha256"));
        model.setModelFormat("onnx");
        model.setQuantization("int8");
        model.setSizeBytes(component != null && component.getSizeBytes() != null ? component.getSizeBytes() : 0L);
        model.setSha256(component != null && component.getSha256() != null ? component.getSha256() : "");
        model.setSupportsText(true);
        model.setSupportsOcrBlock(true);
        model.setSupportsRealtime(isRealtimeTranslationLanguage(sourceLanguage)
                && isRealtimeTranslationLanguage(targetLanguage));
        model.setSupportsBatch(true);
        model.setPriority(("en".equals(sourceLanguage) || "en".equals(targetLanguage)) ? 10 : 20);
        model.setLicense("TBD");
        model.setAttribution("OPUS-MT / Marian");
        model.setRequiredPlan("offline_membership");
        model.setRecommendedDeviceLevel("LOW");
        model.setMinAndroidSdk(23);
        model.setMinIosVersion("13.0");
        model.setCapabilityStatus(published ? "downloadable" : "planned");
        models.add(model);
    }

    private static boolean isPublishedComponent(ComponentPackRespVO component) {
        return component != null
                && !StrUtil.containsIgnoreCase(component.getReleaseStatus(), "planned")
                && StrUtil.isNotBlank(component.getUrl())
                && StrUtil.isNotBlank(component.getSha256())
                && CollUtil.isNotEmpty(component.getRequiredFiles());
    }

    private static boolean isRealtimeTranslationLanguage(String languageCode) {
        return REALTIME_TRANSLATION_LANGUAGES.contains(languageCode);
    }

    private <T> T readJsonResource(String location, TypeReference<T> typeReference) throws IOException {
        try (InputStream inputStream = resourceLoader.getResource(location).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    @Data
    public static class ModelCatalogRespVO {
        private Integer schemaVersion;
        private String releaseVersion;
        private String baseUrl;
        private boolean downloadRequiresSignedUrl;
        private List<BusinessPackRespVO> businessPacks;
        private List<ComponentPackRespVO> components;
        private List<TranslationModelRespVO> models;
    }

    @Data
    public static class BusinessPackRespVO {
        private String packId;
        private String displayNameZh;
        private String summaryZh;
        private String version;
        private String releaseStatus;
        private List<String> components;
        private Long sizeBytes;
        private String dependencyModel;
    }

    @Data
    public static class ComponentPackRespVO {
        private String packId;
        private String type;
        private String engine;
        private String version;
        private String displayNameZh;
        private String summaryZh;
        private String releaseStatus;
        private Integer minRamGb;
        private String installPath;
        private Long sourceBytes;
        private Long sizeBytes;
        private String sha256;
        private String url;
        private String objectKey;
        private String manifestUrl;
        private String manifestObjectKey;
        private List<String> modelIds;
        private List<RequiredFileRespVO> requiredFiles;
    }

    @Data
    public static class TranslationModelRespVO {
        private String modelId;
        private String family;
        private String engine;
        private String sourceLanguageCode;
        private String targetLanguageCode;
        private String componentPackId;
        private Map<String, String> artifactPaths;
        private String modelFormat;
        private String quantization;
        private Long sizeBytes;
        private String sha256;
        private Boolean supportsText;
        private Boolean supportsOcrBlock;
        private Boolean supportsRealtime;
        private Boolean supportsBatch;
        private Integer priority;
        private String license;
        private String attribution;
        private String requiredPlan;
        private String recommendedDeviceLevel;
        private Integer minAndroidSdk;
        private String minIosVersion;
        private String capabilityStatus;
    }

    @Data
    public static class RequiredFileRespVO {
        private String path;
        private Long sizeBytes;
        private String sha256;
    }

    @Data
    public static class DownloadUrlReqVO {
        private String businessPackId;
        private List<String> componentIds;
    }

    @Data
    public static class DownloadUrlRespVO {
        private LocalDateTime expiresAt;
        private Integer expiresInSeconds;
        private List<SignedComponentRespVO> components;
    }

    @Data
    public static class SignedComponentRespVO {
        private String packId;
        private String type;
        private String version;
        private String installPath;
        private String objectKey;
        private Long sizeBytes;
        private String sha256;
        private String downloadUrl;
        private List<RequiredFileRespVO> requiredFiles;
    }

    @Data
    public static class UploadPublishReqVO {
        private String localRoot;
    }

    @Data
    public static class UploadPublishRespVO {
        private String bucket;
        private String region;
        private String prefix;
        private int uploadedFiles;
        private long uploadedBytes;
        private int verifiedFiles;
        private long verifiedBytes;
        private List<KeyJsonObjectRespVO> keyJsonObjects = new ArrayList<>();
        private LocalDateTime finishedAt;
    }

    @Data
    public static class KeyJsonObjectRespVO {
        private String objectKey;
        private long sizeBytes;
        private String sha256;
    }
}
