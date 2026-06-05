package cn.iocoder.yudao.server.service.app;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.BusinessPackRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.ComponentPackRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.DownloadUrlReqVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.ModelCatalogRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.TranslationModelRespVO;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService.CosConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
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
        assertEquals(3, catalog.getBusinessPacks().size());
        assertBusinessPack(catalog.getBusinessPacks(), "offline-text-translation-full",
                expectedBusinessComponents("text-hymt-core"));
        assertBusinessPack(catalog.getBusinessPacks(), "offline-image-translation-full",
                expectedBusinessComponents("text-hymt-core", "ocr-tesseract-core"));
        assertBusinessPack(catalog.getBusinessPacks(), "offline-conversation-translation-full",
                expectedBusinessComponents("text-hymt-core", "asr-sensevoice-core", "asr-whisper-wide", "tts-sherpa-core"));

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
        assertEquals(54, expectedOpusModelIds.size());
        for (String modelId : expectedOpusModelIds) {
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
            assertEquals("downloadable", model.getCapabilityStatus());
            assertTrue(model.getSizeBytes() > 0L);
            assertFalse(model.getSha256().trim().isEmpty());
            assertTrue(model.getSupportsText());
            assertTrue(model.getSupportsOcrBlock());
            assertTrue(model.getSupportsBatch());
            assertNotNull(model.getArtifactPaths().get("model"));
            assertNotNull(model.getArtifactPaths().get("manifest"));

            ComponentPackRespVO component = componentsById.get(modelId);
            assertNotNull(component, modelId);
            assertEquals(List.of(modelId), component.getModelIds());
            assertFalse(component.getReleaseStatus().contains("planned"));
            assertTrue(component.getSizeBytes() > 0L);
            assertFalse(component.getSha256().trim().isEmpty());
            assertFalse(component.getUrl().trim().isEmpty());
            assertFalse(component.getRequiredFiles().isEmpty());
        }
        for (String unpublishedModelId : unpublishedOpusModelIds()) {
            assertFalse(modelsById.containsKey(unpublishedModelId), unpublishedModelId);
            assertFalse(componentsById.containsKey(unpublishedModelId), unpublishedModelId);
        }
        TranslationModelRespVO realtimeDirect = modelsById.get("text-opus-marian-id-en");
        assertNotNull(realtimeDirect);
        assertTrue(realtimeDirect.getSupportsRealtime());
        TranslationModelRespVO nonRealtimeDirect = modelsById.get("text-opus-marian-ms-en");
        assertNotNull(nonRealtimeDirect);
        assertFalse(nonRealtimeDirect.getSupportsRealtime());

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
    void downloadUrlsRejectsUnpublishedOpusComponentsWithoutSignedUrls() {
        DownloadUrlReqVO reqVO = new DownloadUrlReqVO();
        reqVO.setComponentIds(List.of("text-opus-marian-en-ko"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createDownloadUrls(1L, reqVO));

        assertFalse(exception.getMessage().contains("downloadUrl"));
        assertFalse(exception.getMessage().contains("X-Amz-Signature"));
    }

    private void assertBusinessPack(List<BusinessPackRespVO> packs, String packId, List<String> components) {
        BusinessPackRespVO pack = packs.stream()
                .filter(item -> packId.equals(item.getPackId()))
                .findFirst()
                .orElseThrow();
        assertEquals(components, pack.getComponents());
    }

    private List<String> expectedBusinessComponents(String... extraComponents) {
        java.util.ArrayList<String> components = new java.util.ArrayList<>(expectedOpusModelIds());
        components.addAll(List.of(extraComponents));
        return components;
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
                List.of("zh", "vi"),
                List.of("zh", "ms"),
                List.of("zh", "de"),
                List.of("de", "zh"),
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

    private Set<String> unpublishedOpusModelIds() {
        return Set.of(
                modelId("en", "ko"),
                modelId("zh", "th"),
                modelId("th", "zh"),
                modelId("vi", "zh"),
                modelId("zh", "id"),
                modelId("id", "zh"));
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
