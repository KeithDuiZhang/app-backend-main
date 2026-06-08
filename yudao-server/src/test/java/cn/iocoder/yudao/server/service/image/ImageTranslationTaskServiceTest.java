package cn.iocoder.yudao.server.service.image;

import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.app.AppPaymentService;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UsageConsumeReqVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UserEntitlementRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CreateTaskRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.RetryReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.Status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageTranslationTaskServiceTest {

    private static final Long USER_ID = 1001L;
    private static final String RAW_SHA = "raw-sha";
    private static final String CACHE_KEY = "cache-key";
    private static final String PROVIDER = "qwen_mt_image";

    @Test
    void createTaskConsumesQuotaWhenCacheHitCreatesSuccessTask() throws Exception {
        ImageTranslationJdbcTemplate jdbcTemplate = new ImageTranslationJdbcTemplate(cacheRow());
        AppPaymentService appPaymentService = mock(AppPaymentService.class);
        when(appPaymentService.consumeOnlineUsage(eq(USER_ID), any(UsageConsumeReqVO.class)))
                .thenReturn(new UserEntitlementRespVO());
        ImageTranslationTaskService service = newService(jdbcTemplate, appPaymentService);

        CreateTaskRespVO respVO = service.createTask(USER_ID,
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                "zh", "en", "AUTO", "auto");

        assertEquals(Status.SUCCESS.name(), respVO.getStatus());
        assertTrue(respVO.isCached());
        ArgumentCaptor<UsageConsumeReqVO> captor = ArgumentCaptor.forClass(UsageConsumeReqVO.class);
        verify(appPaymentService).consumeOnlineUsage(eq(USER_ID), captor.capture());
        UsageConsumeReqVO reqVO = captor.getValue();
        assertEquals("image_translation:" + respVO.getTaskId(), reqVO.getClientRequestId());
        assertEquals("online", reqVO.getMode());
        assertEquals("image_translation", reqVO.getScene());
        assertEquals("zh", reqVO.getSourceLanguageCode());
        assertEquals("en", reqVO.getTargetLanguageCode());
        assertEquals(1, reqVO.getImageTranslateCount());
    }

    @Test
    void retryTaskConsumesQuotaWhenCacheHitSucceeds() throws Exception {
        ImageTranslationJdbcTemplate jdbcTemplate = new ImageTranslationJdbcTemplate(cacheRow());
        jdbcTemplate.addTask(existingTask("IT_RETRY"));
        AppPaymentService appPaymentService = mock(AppPaymentService.class);
        when(appPaymentService.consumeOnlineUsage(eq(USER_ID), any(UsageConsumeReqVO.class)))
                .thenReturn(new UserEntitlementRespVO());
        ImageTranslationTaskService service = newService(jdbcTemplate, appPaymentService);

        CreateTaskRespVO respVO = service.retryTask(USER_ID, "IT_RETRY", new RetryReqVO());

        assertEquals(Status.SUCCESS.name(), respVO.getStatus());
        assertTrue(respVO.isCached());
        ArgumentCaptor<UsageConsumeReqVO> captor = ArgumentCaptor.forClass(UsageConsumeReqVO.class);
        verify(appPaymentService).consumeOnlineUsage(eq(USER_ID), captor.capture());
        UsageConsumeReqVO reqVO = captor.getValue();
        assertEquals("image_translation:IT_RETRY", reqVO.getClientRequestId());
        assertEquals("image_translation", reqVO.getScene());
        assertEquals(1, reqVO.getImageTranslateCount());
    }

    private ImageTranslationTaskService newService(ImageTranslationJdbcTemplate jdbcTemplate,
                                                   AppPaymentService appPaymentService) throws Exception {
        ImageTranslationTaskService service = new ImageTranslationTaskService();
        ImageTranslationProperties properties = new ImageTranslationProperties();
        properties.setEnabled(true);
        properties.setCacheEnabled(true);
        properties.setDefaultProvider(PROVIDER);
        ImageHashService hashService = mock(ImageHashService.class);
        when(hashService.rawSha256(any(byte[].class))).thenReturn(RAW_SHA);
        when(hashService.cacheKey(eq(RAW_SHA), eq("zh"), eq("en"), eq(PROVIDER))).thenReturn(CACHE_KEY);
        setField(service, "jdbcTemplate", jdbcTemplate);
        setField(service, "properties", properties);
        setField(service, "hashService", hashService);
        setField(service, "appPaymentService", appPaymentService);
        return service;
    }

    private static Map<String, Object> cacheRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 10L);
        row.put("cache_key", CACHE_KEY);
        row.put("display_mode", "TEXT_LIST");
        row.put("result_image_cos_key", "");
        row.put("result_json_cos_key", "result.json");
        row.put("quality_json_cos_key", "quality.json");
        row.put("text_items_json", "[{\"sourceText\":\"hello\",\"translatedText\":\"hello-cn\"}]");
        row.put("provider", PROVIDER);
        row.put("quality_score", 0.95d);
        row.put("warning_message", "");
        row.put("expire_at", LocalDateTime.now().plusDays(1));
        return row;
    }

    private static Map<String, Object> existingTask(String taskNo) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 20L);
        row.put("user_id", USER_ID);
        row.put("task_no", taskNo);
        row.put("source_lang", "zh");
        row.put("target_lang", "en");
        row.put("prefer_provider", "auto");
        row.put("status", Status.FAILED.name());
        row.put("cache_key", CACHE_KEY);
        row.put("display_mode", "");
        return row;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = ImageTranslationTaskService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class ImageTranslationJdbcTemplate extends JdbcTemplate {

        private final Map<String, Object> cache;
        private final Map<String, Map<String, Object>> tasksByNo = new LinkedHashMap<>();
        private long nextTaskId = 1L;

        private ImageTranslationJdbcTemplate(Map<String, Object> cache) {
            this.cache = cache;
        }

        private void addTask(Map<String, Object> task) {
            tasksByNo.put(String.valueOf(task.get("task_no")), task);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            String normalized = normalize(sql);
            if (normalized.contains("FROM image_translation_cache")) {
                return CACHE_KEY.equals(args[0]) ? List.of(cache) : List.of();
            }
            if (normalized.contains("FROM image_translation_task") && normalized.contains("WHERE task_no = ?")) {
                Map<String, Object> task = tasksByNo.get(String.valueOf(args[0]));
                if (task == null || !USER_ID.equals(task.get("user_id"))) {
                    return List.of();
                }
                return List.of(task);
            }
            if (normalized.contains("FROM image_translation_task") && normalized.contains("WHERE user_id = ?")) {
                return List.of();
            }
            if (normalized.contains("FROM image_translation_task") && normalized.contains("WHERE id = ?")) {
                Long id = ((Number) args[0]).longValue();
                return tasksByNo.values().stream()
                        .filter(task -> id.equals(task.get("id")))
                        .findFirst()
                        .map(List::of)
                        .orElseGet(List::of);
            }
            return List.of();
        }

        @Override
        public int update(String sql, Object... args) {
            String normalized = normalize(sql);
            if (normalized.contains("INSERT INTO image_translation_task")) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", nextTaskId++);
                task.put("user_id", args[0]);
                task.put("task_no", args[1]);
                task.put("trace_id", args[2]);
                task.put("source_lang", args[3]);
                task.put("target_lang", args[4]);
                task.put("mode", args[5]);
                task.put("prefer_provider", args[6]);
                task.put("status", args[7]);
                task.put("display_mode", args[8]);
                task.put("cached", args[9]);
                task.put("raw_sha256", args[10]);
                task.put("cache_key", args[11]);
                task.put("provider", args[12]);
                tasksByNo.put(String.valueOf(args[1]), task);
                return 1;
            }
            if (normalized.contains("UPDATE image_translation_task SET result_image_cos_key")) {
                Map<String, Object> task = taskById(args[8]);
                task.put("result_image_cos_key", args[0]);
                task.put("result_json_cos_key", args[1]);
                task.put("quality_json_cos_key", args[2]);
                task.put("text_items_json", args[3]);
                task.put("provider", args[4]);
                task.put("quality_score", args[5]);
                task.put("warning_message", args[6]);
                task.put("expire_at", args[7]);
                return 1;
            }
            if (normalized.contains("UPDATE image_translation_task SET status = 'SUCCESS'")) {
                Map<String, Object> task = taskById(args[8]);
                task.put("status", Status.SUCCESS.name());
                task.put("display_mode", args[0]);
                task.put("cached", 1);
                task.put("result_image_cos_key", args[1]);
                task.put("result_json_cos_key", args[2]);
                task.put("quality_json_cos_key", args[3]);
                task.put("text_items_json", args[4]);
                task.put("provider", args[5]);
                task.put("quality_score", args[6]);
                task.put("warning_message", args[7]);
                return 1;
            }
            return 1;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            String normalized = normalize(sql);
            if (normalized.contains("SELECT id FROM image_translation_task")) {
                Map<String, Object> task = tasksByNo.get(String.valueOf(args[0]));
                return requiredType.cast(task.get("id"));
            }
            throw new UnsupportedOperationException(sql);
        }

        private Map<String, Object> taskById(Object idValue) {
            Long id = ((Number) idValue).longValue();
            return tasksByNo.values().stream()
                    .filter(task -> id.equals(task.get("id")))
                    .findFirst()
                    .orElseThrow();
        }

        private static String normalize(String sql) {
            return sql.replaceAll("\\s+", " ").trim();
        }
    }
}
