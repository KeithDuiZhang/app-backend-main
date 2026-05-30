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
    private static final String DEFAULT_LOCAL_PUBLISH_ROOT =
            "D:/Code_Project/md_CN_model_repo/published/model-repo/cn/1.0.0";
    private static final String LOCAL_REPO_GUARD = "D:/Code_Project/md_CN_model_repo";

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
    }

    public ModelCatalogRespVO getCatalog() {
        validateCosRuntimeConfigured();
        CosConfig cosConfig = integrationConfigService.getCosRuntimeConfig();
        ModelCatalogRespVO respVO = new ModelCatalogRespVO();
        respVO.setReleaseVersion("1.0.0");
        respVO.setBaseUrl(buildBaseUrl(cosConfig));
        respVO.setDownloadRequiresSignedUrl(true);
        respVO.setBusinessPacks(businessPacks);
        respVO.setComponents(componentPacks);
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
            if (!componentPacksById.containsKey(componentId)) {
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

    private <T> T readJsonResource(String location, TypeReference<T> typeReference) throws IOException {
        try (InputStream inputStream = resourceLoader.getResource(location).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    @Data
    public static class ModelCatalogRespVO {
        private String releaseVersion;
        private String baseUrl;
        private boolean downloadRequiresSignedUrl;
        private List<BusinessPackRespVO> businessPacks;
        private List<ComponentPackRespVO> components;
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
        private List<RequiredFileRespVO> requiredFiles;
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
