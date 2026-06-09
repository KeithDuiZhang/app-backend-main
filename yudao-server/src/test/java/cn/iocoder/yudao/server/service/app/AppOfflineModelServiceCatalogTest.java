package cn.iocoder.yudao.server.service.app;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.BusinessPackRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.ComponentPackRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.DownloadUrlReqVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.ModelCatalogRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.RequiredFileRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.TranslationModelRespVO;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService.CosConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppOfflineModelServiceCatalogTest {

    private AppOfflineModelService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AppOfflineModelService();
        setField("resourceLoader", new DefaultResourceLoader());
        setField("integrationConfigService", new AppIntegrationConfigService() {
            @Override
            public boolean isCosRuntimeConfigured() {
                return true;
            }

            @Override
            public CosConfig getCosRuntimeConfig() {
                CosConfig config = new CosConfig();
                config.setDomain("https://kqtranslate-1300276385.cos.ap-shanghai.myqcloud.com");
                config.setPrefix("model-repo/cn/1.0.0/");
                return config;
            }
        });
        setField("jdbcTemplate", new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
                return requiredType.cast(1);
            }
        });
        service.init();
    }

    @Test
    void catalogV2KeepsLegacyPacksAndDoesNotExposeSignedUrls() throws Exception {
        ModelCatalogRespVO catalog = service.getCatalog();

        assertEquals(2, catalog.getSchemaVersion());
        assertEquals("1.0.0", catalog.getReleaseVersion());
        assertTrue(catalog.isDownloadRequiresSignedUrl());
        assertEquals(4, catalog.getBusinessPacks().size());
        assertRecommendedZhCentricTextPack(catalog);
        assertBusinessPack(catalog.getBusinessPacks(), "offline-text-translation-full",
                expectedBusinessComponents("text-hymt-core"));
        assertBusinessPack(catalog.getBusinessPacks(), "offline-image-translation-full",
                List.of(), 0L);
        assertBusinessPack(catalog.getBusinessPacks(), "offline-conversation-translation-full",
                List.of("asr-sensevoice-core", "asr-whisper-wide", "tts-sherpa-core"), 902_832_184L);

        String catalogJson = new ObjectMapper().writeValueAsString(catalog);
        assertFalse(catalogJson.contains("downloadUrl"));
        assertFalse(catalogJson.contains("X-Amz-Signature"));
    }

    @Test
    void catalogV2CoversExpectedOpusMarianModelsAndComponentModelIds() {
        ModelCatalogRespVO catalog = service.getCatalog();
        Map<String, TranslationModelRespVO> modelsById = catalog.getModels().stream()
                .collect(Collectors.toMap(TranslationModelRespVO::getModelId, Function.identity()));
        Map<String, ComponentPackRespVO> componentsById = catalog.getComponents().stream()
                .collect(Collectors.toMap(ComponentPackRespVO::getPackId, Function.identity()));

        Set<String> expectedOpusModelIds = expectedOpusModelIds();
        Set<String> expectedPlannedOpusModelIds = expectedPlannedOpusModelIds();
        assertEquals(66, expectedOpusModelIds.size());
        assertEquals(12, expectedPlannedOpusModelIds.size());
        for (String modelId : expectedOpusModelIds) {
            boolean planned = expectedPlannedOpusModelIds.contains(modelId);
            TranslationModelRespVO model = modelsById.get(modelId);
            assertNotNull(model, modelId);
            String[] parts = modelId.replace("text-opus-marian-", "").split("-");
            assertEquals("opus-marian", model.getFamily());
            assertEquals("OPUS-Marian", model.getEngine());
            assertEquals(parts[0], model.getSourceLanguageCode());
            assertEquals(parts[1], model.getTargetLanguageCode());
            assertEquals(modelId, model.getComponentPackId());
            assertEquals("onnx", model.getModelFormat());
            assertEquals("int8", model.getQuantization());
            assertEquals(planned ? "planned" : "downloadable", model.getCapabilityStatus());
            assertTrue(model.getSupportsText());
            assertTrue(model.getSupportsOcrBlock());
            assertTrue(model.getSupportsBatch());
            assertNotNull(model.getArtifactPaths().get("model"));
            assertNotNull(model.getArtifactPaths().get("manifest"));

            ComponentPackRespVO component = componentsById.get(modelId);
            assertNotNull(component, modelId);
            assertEquals(List.of(modelId), component.getModelIds());
            if (planned) {
                assertTrue(component.getReleaseStatus().contains("planned"));
                assertEquals(0L, model.getSizeBytes());
                assertTrue(model.getSha256().isBlank());
                assertEquals(0L, component.getSizeBytes());
                assertTrue(component.getSha256().isBlank());
                assertTrue(component.getUrl().isBlank());
                assertTrue(component.getRequiredFiles().isEmpty());
            } else {
                assertFalse(component.getReleaseStatus().contains("planned"));
                assertTrue(model.getSizeBytes() > 0L);
                assertFalse(model.getSha256().trim().isEmpty());
                assertTrue(component.getSizeBytes() > 0L);
                assertFalse(component.getSha256().trim().isEmpty());
                assertFalse(component.getUrl().trim().isEmpty());
                assertFalse(component.getRequiredFiles().isEmpty());
            }
        }
        for (String unpublishedModelId : unpublishedOpusModelIds()) {
            assertFalse(modelsById.containsKey(unpublishedModelId), unpublishedModelId);
            assertFalse(componentsById.containsKey(unpublishedModelId), unpublishedModelId);
        }
        TranslationModelRespVO realtimeDirect = modelsById.get("text-opus-marian-id-en");
        assertNotNull(realtimeDirect);
        assertTrue(realtimeDirect.getSupportsRealtime());
        TranslationModelRespVO zhDeRealtime = modelsById.get("text-opus-marian-zh-de");
        assertNotNull(zhDeRealtime);
        assertTrue(zhDeRealtime.getSupportsRealtime());
        TranslationModelRespVO plannedZhFrRealtime = modelsById.get("text-opus-marian-zh-fr");
        assertNotNull(plannedZhFrRealtime);
        assertEquals("planned", plannedZhFrRealtime.getCapabilityStatus());
        assertTrue(plannedZhFrRealtime.getSupportsRealtime());

        TranslationModelRespVO m2m100 = modelsById.get("text-m2m100-418m-int8");
        assertNotNull(m2m100);
        assertEquals("m2m100", m2m100.getFamily());
        assertEquals("text-m2m100-418m-int8", m2m100.getComponentPackId());
        assertFalse(m2m100.getSupportsRealtime());
        assertEquals("planned", m2m100.getCapabilityStatus());

        TranslationModelRespVO hymt = modelsById.get("text-hymt-enhance");
        assertNotNull(hymt);
        assertEquals("hy-mt", hymt.getFamily());
        assertEquals("text-hymt-core", hymt.getComponentPackId());
        assertFalse(hymt.getSupportsRealtime());
        assertEquals("manual-enhance", hymt.getCapabilityStatus());
        assertEquals(List.of("text-hymt-enhance"), componentsById.get("text-hymt-core").getModelIds());
    }

    @Test
    void downloadUrlsRejectsPlannedOpusComponentsWithoutSignedUrls() {
        DownloadUrlReqVO reqVO = new DownloadUrlReqVO();
        reqVO.setComponentIds(List.of("text-opus-marian-zh-th"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createDownloadUrls(1L, reqVO));

        assertFalse(exception.getMessage().contains("downloadUrl"));
        assertFalse(exception.getMessage().contains("X-Amz-Signature"));
    }

    @Test
    void downloadUrlsRejectsPlannedComponentsEvenIfUrlIsFilled() throws Exception {
        ComponentPackRespVO component = new ComponentPackRespVO();
        component.setPackId("text-opus-marian-test-planned");
        component.setType("translation");
        component.setVersion("1.0.0");
        component.setReleaseStatus("planned");
        component.setUrl("packs/translation/text-opus-marian-test-planned.zip");
        component.setSha256("abc123");
        RequiredFileRespVO requiredFile = new RequiredFileRespVO();
        requiredFile.setPath("model.int8.onnx");
        requiredFile.setSizeBytes(1L);
        requiredFile.setSha256("def456");
        component.setRequiredFiles(List.of(requiredFile));

        Field field = AppOfflineModelService.class.getDeclaredField("componentPacksById");
        field.setAccessible(true);
        Map<String, ComponentPackRespVO> components = new LinkedHashMap<>(
                (Map<String, ComponentPackRespVO>) field.get(service));
        components.put(component.getPackId(), component);
        field.set(service, components);

        DownloadUrlReqVO reqVO = new DownloadUrlReqVO();
        reqVO.setComponentIds(List.of(component.getPackId()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createDownloadUrls(1L, reqVO));

        assertFalse(exception.getMessage().contains("downloadUrl"));
        assertFalse(exception.getMessage().contains("X-Amz-Signature"));
    }

    private void assertBusinessPack(List<BusinessPackRespVO> packs, String packId, List<String> components) {
        assertBusinessPack(packs, packId, components, null);
    }

    private void assertBusinessPack(List<BusinessPackRespVO> packs, String packId, List<String> components, Long sizeBytes) {
        BusinessPackRespVO pack = packs.stream()
                .filter(item -> packId.equals(item.getPackId()))
                .findFirst()
                .orElseThrow();
        assertEquals(components, pack.getComponents());
        if (sizeBytes != null) {
            assertEquals(sizeBytes, pack.getSizeBytes());
        }
    }

    private void assertRecommendedZhCentricTextPack(ModelCatalogRespVO catalog) {
        BusinessPackRespVO pack = catalog.getBusinessPacks().stream()
                .filter(item -> "offline-text-zh-centric-12".equals(item.getPackId()))
                .findFirst()
                .orElseThrow();
        List<String> expectedComponents = expectedRecommendedZhCentricComponents();
        assertEquals(expectedComponents, pack.getComponents());
        assertEquals(22, pack.getComponents().size());
        assertFalse(pack.getComponents().contains("text-hymt-core"));
        assertFalse(pack.getComponents().contains("text-m2m100-418m-int8"));
        assertFalse(pack.getComponents().contains("ocr-tesseract-core"));
        assertFalse(pack.getComponents().contains("asr-whisper-wide"));

        Map<String, ComponentPackRespVO> componentsById = catalog.getComponents().stream()
                .collect(Collectors.toMap(ComponentPackRespVO::getPackId, Function.identity()));
        long expectedSizeBytes = expectedComponents.stream()
                .map(componentsById::get)
                .peek(component -> assertNotNull(component, "missing component in recommended pack"))
                .mapToLong(ComponentPackRespVO::getSizeBytes)
                .sum();
        assertEquals(expectedSizeBytes, pack.getSizeBytes());
        assertEquals("planned", pack.getReleaseStatus());
        assertEquals(751_968_666L, pack.getSizeBytes());
        for (String pivotComponent : List.of(
                "text-opus-marian-vi-en",
                "text-opus-marian-en-id",
                "text-opus-marian-ms-en",
                "text-opus-marian-en-th",
                "text-opus-marian-en-fr",
                "text-opus-marian-en-es",
                "text-opus-marian-en-ru")) {
            assertFalse(pack.getComponents().contains(pivotComponent), pivotComponent);
        }
    }

    private List<String> expectedBusinessComponents(String... extraComponents) {
        java.util.ArrayList<String> components = new java.util.ArrayList<>(expectedDownloadableOpusModelIds());
        components.addAll(List.of(extraComponents));
        return components;
    }

    private List<String> expectedRecommendedZhCentricComponents() {
        return List.of(
                "text-opus-marian-zh-en",
                "text-opus-marian-en-zh",
                "text-opus-marian-zh-ja",
                "text-opus-marian-ja-zh",
                "text-opus-marian-zh-ko",
                "text-opus-marian-ko-zh",
                "text-opus-marian-zh-th",
                "text-opus-marian-th-zh",
                "text-opus-marian-zh-vi",
                "text-opus-marian-vi-zh",
                "text-opus-marian-zh-id",
                "text-opus-marian-id-zh",
                "text-opus-marian-zh-ms",
                "text-opus-marian-ms-zh",
                "text-opus-marian-zh-de",
                "text-opus-marian-de-zh",
                "text-opus-marian-zh-fr",
                "text-opus-marian-fr-zh",
                "text-opus-marian-zh-es",
                "text-opus-marian-es-zh",
                "text-opus-marian-zh-ru",
                "text-opus-marian-ru-zh");
    }

    private Set<String> expectedOpusModelIds() {
        return List.of(
                List.of("en", "zh"),
                List.of("zh", "en"),
                List.of("en", "ja"),
                List.of("zh", "ja"),
                List.of("ja", "zh"),
                List.of("ja", "en"),
                List.of("ko", "en"),
                List.of("th", "en"),
                List.of("en", "th"),
                List.of("vi", "en"),
                List.of("en", "vi"),
                List.of("id", "en"),
                List.of("en", "id"),
                List.of("ms", "en"),
                List.of("en", "ms"),
                List.of("fr", "en"),
                List.of("en", "fr"),
                List.of("de", "en"),
                List.of("en", "de"),
                List.of("es", "en"),
                List.of("en", "es"),
                List.of("ru", "en"),
                List.of("en", "ru"),
                List.of("zh", "ko"),
                List.of("ko", "zh"),
                List.of("zh", "th"),
                List.of("th", "zh"),
                List.of("zh", "vi"),
                List.of("vi", "zh"),
                List.of("zh", "id"),
                List.of("id", "zh"),
                List.of("zh", "ms"),
                List.of("ms", "zh"),
                List.of("zh", "de"),
                List.of("de", "zh"),
                List.of("zh", "fr"),
                List.of("fr", "zh"),
                List.of("zh", "es"),
                List.of("es", "zh"),
                List.of("zh", "ru"),
                List.of("ru", "zh"),
                List.of("ja", "vi"),
                List.of("ja", "es"),
                List.of("ja", "fr"),
                List.of("ja", "ru"),
                List.of("de", "vi"),
                List.of("ru", "vi"),
                List.of("fr", "de"),
                List.of("de", "fr"),
                List.of("fr", "es"),
                List.of("ru", "fr"),
                List.of("de", "es"),
                List.of("es", "de"),
                List.of("ru", "es"),
                List.of("ja", "ms"),
                List.of("ko", "fr"),
                List.of("th", "fr"),
                List.of("id", "fr"),
                List.of("ms", "fr"),
                List.of("ms", "de"),
                List.of("fr", "vi"),
                List.of("fr", "id"),
                List.of("fr", "ms"),
                List.of("de", "ms"),
                List.of("es", "id"),
                List.of("id", "es"))
                .stream()
                .map(pair -> modelId(pair.get(0), pair.get(1)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<String> expectedDownloadableOpusModelIds() {
        Set<String> planned = expectedPlannedOpusModelIds();
        return expectedOpusModelIds().stream()
                .filter(modelId -> !planned.contains(modelId))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<String> expectedPlannedOpusModelIds() {
        return List.of(
                List.of("zh", "th"),
                List.of("th", "zh"),
                List.of("vi", "zh"),
                List.of("zh", "id"),
                List.of("id", "zh"),
                List.of("ms", "zh"),
                List.of("zh", "fr"),
                List.of("fr", "zh"),
                List.of("zh", "es"),
                List.of("es", "zh"),
                List.of("zh", "ru"),
                List.of("ru", "zh"))
                .stream()
                .map(pair -> modelId(pair.get(0), pair.get(1)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<String> unpublishedOpusModelIds() {
        return Set.of(modelId("en", "ko"));
    }

    private String modelId(String sourceLanguage, String targetLanguage) {
        return "text-opus-marian-" + sourceLanguage + "-" + targetLanguage;
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = AppOfflineModelService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }
}
