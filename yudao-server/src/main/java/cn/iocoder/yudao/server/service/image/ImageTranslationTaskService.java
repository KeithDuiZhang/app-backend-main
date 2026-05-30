package cn.iocoder.yudao.server.service.image;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.mq.redis.core.RedisMQTemplate;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.app.AppPaymentService;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UsageConsumeReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CreateTaskRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.DisplayMode;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProcessedImage;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRequest;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.QualityResult;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.QualityStatus;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.RetryReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.Status;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TaskStatusRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TextItemRespVO;
import cn.iocoder.yudao.server.service.image.provider.AliyunTranslateImageProvider;
import cn.iocoder.yudao.server.service.image.provider.ImageTranslationProvider;
import cn.iocoder.yudao.server.service.image.provider.QwenMtImageProvider;
import cn.iocoder.yudao.server.service.image.redis.ImageTranslationTaskMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ImageTranslationTaskService {

    private static final String PROVIDER_AUTO = "auto";
    private static final String MESSAGE_SUBMITTED = "已提交图片翻译任务";
    private static final String MESSAGE_CACHED = "已命中图片翻译缓存";
    private static final String MESSAGE_RETRY = "已重新提交图片翻译任务";
    private static final String USER_FAILED_MESSAGE = "图片翻译失败，建议裁剪文字区域或重新拍摄后重试。";

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ImageTranslationProperties properties;
    @Resource
    private ImageHashService hashService;
    @Resource
    private ImageTranslationPreprocessor preprocessor;
    @Resource
    private ImageTranslationStorageService storageService;
    @Resource
    private ImageTranslationQualityChecker qualityChecker;
    @Resource
    private ProviderFallbackPolicy fallbackPolicy;
    @Resource
    private AppPaymentService appPaymentService;
    @Autowired(required = false)
    private RedisMQTemplate redisMQTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService fallbackExecutor = Executors.newFixedThreadPool(2);

    @EventListener(ApplicationReadyEvent.class)
    public void recoverLocalPendingTasks() {
        if (!"local".equalsIgnoreCase(properties.getQueueType())) {
            return;
        }
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT id, task_no
                FROM image_translation_task
                WHERE status = 'PENDING'
                ORDER BY create_time ASC
                LIMIT 20
                """);
        for (Map<String, Object> task : tasks) {
            Long taskId = numberLong(task.get("id"));
            String taskNo = string(task, "task_no");
            log.info("image translation recover pending task taskId={}", taskNo);
            enqueue(taskId, taskNo);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateTaskRespVO createTask(Long userId,
                                       MultipartFile file,
                                       String sourceLang,
                                       String targetLang,
                                       String mode,
                                       String preferProvider) throws IOException {
        if (!properties.isEnabled()) {
            throw ServiceExceptionUtil.invalidParamException("图片翻译暂未开通，请稍后再试");
        }
        validateUpload(file, sourceLang, targetLang);
        String source = normalizeLang(sourceLang);
        String target = normalizeLang(targetLang);
        String provider = resolveProvider(preferProvider);
        byte[] originalBytes = file.getBytes();
        String rawSha256 = hashService.rawSha256(originalBytes);
        String cacheKey = hashService.cacheKey(rawSha256, source, target, provider);
        if (properties.isCacheEnabled()) {
            Map<String, Object> cache = findValidCache(cacheKey);
            if (cache != null) {
                jdbcTemplate.update("UPDATE image_translation_cache SET hit_count = hit_count + 1, updater = 'app', update_time = NOW() WHERE id = ?",
                        cache.get("id"));
                String taskNo = createCachedTask(userId, source, target, mode, preferProvider, rawSha256, cacheKey, cache);
                return taskResponse(taskNo, Status.SUCCESS.name(), true, string(cache, "display_mode"), MESSAGE_CACHED);
            }
        }
        Map<String, Object> existing = findActiveTask(userId, cacheKey);
        if (existing != null) {
            return taskResponse(string(existing, "task_no"), string(existing, "status"), false,
                    string(existing, "display_mode"), MESSAGE_SUBMITTED);
        }
        String taskNo = generateTaskNo();
        String traceId = IdUtil.fastSimpleUUID();
        insertTask(userId, taskNo, traceId, source, target, normalizeMode(mode), normalizePrefer(preferProvider),
                Status.PENDING.name(), null, false, rawSha256, cacheKey);
        Long taskId = queryTaskId(taskNo);
        try {
            ProcessedImage image = preprocessor.process(originalBytes, file.getOriginalFilename(), file.getContentType());
            String originalKey = storageService.originalKey(rawSha256, image.getOriginalExt());
            String enhancedKey = storageService.enhancedKey(rawSha256, image.getEnhancedExt());
            storageService.uploadBytes(originalKey, image.getOriginalBytes(), contentType(image.getOriginalExt()));
            storageService.uploadBytes(enhancedKey, image.getEnhancedBytes(), "image/jpeg");
            jdbcTemplate.update("""
                    UPDATE image_translation_task
                    SET original_cos_key = ?, enhanced_cos_key = ?, updater = 'app', update_time = NOW()
                    WHERE id = ?
                    """, originalKey, enhancedKey, taskId);
            log.info("image translation task created taskId={} user={} rawSha={} source={} target={} provider={} cacheHit=false originalSize={} originalKey={} enhancedKey={}",
                    taskNo, maskUser(userId), rawShaPrefix(rawSha256), source, target, provider,
                    image.getOriginalSizeBytes(), originalKey, enhancedKey);
            enqueueAfterCommit(taskId, taskNo);
        } catch (Exception ex) {
            markFailed(taskId, "cos_or_preprocess_failed", "图片上传失败，请稍后重试");
            throw ex;
        }
        return taskResponse(taskNo, Status.PENDING.name(), false, null, MESSAGE_SUBMITTED);
    }

    public TaskStatusRespVO getTaskStatus(Long userId, String taskNo) {
        Map<String, Object> task = findTaskByNoAndUser(taskNo, userId);
        if (task == null) {
            throw ServiceExceptionUtil.invalidParamException("图片翻译任务不存在");
        }
        TaskStatusRespVO respVO = new TaskStatusRespVO();
        respVO.setTaskId(string(task, "task_no"));
        respVO.setStatus(string(task, "status"));
        respVO.setDisplayMode(string(task, "display_mode"));
        respVO.setCached(bool(task, "cached"));
        respVO.setProvider(string(task, "provider"));
        respVO.setWarningMessage(string(task, "warning_message"));
        respVO.setFailReason(string(task, "fail_reason"));
        respVO.setQualityScore(doubleOrNull(task.get("quality_score")));
        respVO.setFinishedAt(toLocalDateTime(task.get("finished_at")));
        respVO.setExpireAt(toLocalDateTime(task.get("expire_at")));
        respVO.setTextItems(parseTextItems(string(task, "text_items_json")));
        String resultImageKey = string(task, "result_image_cos_key");
        if (StrUtil.isNotBlank(resultImageKey)) {
            respVO.setResultImageUrl(storageService.presignGetUrl(resultImageKey, storageService.presignedUrlExpireMinutes()));
        }
        return respVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateTaskRespVO retryTask(Long userId, String taskNo, RetryReqVO reqVO) {
        Map<String, Object> task = findTaskByNoAndUser(taskNo, userId);
        if (task == null) {
            throw ServiceExceptionUtil.invalidParamException("图片翻译任务不存在");
        }
        boolean forceRefresh = reqVO != null && reqVO.isForceRefreshCache();
        String preferProvider = reqVO != null ? normalizePrefer(reqVO.getPreferProvider()) : string(task, "prefer_provider");
        if (!forceRefresh && properties.isCacheEnabled()) {
            Map<String, Object> cache = findValidCache(string(task, "cache_key"));
            if (cache != null) {
                jdbcTemplate.update("""
                        UPDATE image_translation_task
                        SET status = 'SUCCESS', display_mode = ?, cached = 1,
                            result_image_cos_key = ?, result_json_cos_key = ?, quality_json_cos_key = ?,
                            text_items_json = ?, provider = ?, quality_score = ?, warning_message = ?,
                            fail_reason = '', finished_at = NOW(), updater = 'app', update_time = NOW()
                        WHERE id = ?
                        """, string(cache, "display_mode"), string(cache, "result_image_cos_key"),
                        string(cache, "result_json_cos_key"), string(cache, "quality_json_cos_key"),
                        string(cache, "text_items_json"), string(cache, "provider"),
                        cache.get("quality_score"), string(cache, "warning_message"), task.get("id"));
                jdbcTemplate.update("UPDATE image_translation_cache SET hit_count = hit_count + 1, updater = 'app', update_time = NOW() WHERE id = ?",
                        cache.get("id"));
                return taskResponse(taskNo, Status.SUCCESS.name(), true, string(cache, "display_mode"), MESSAGE_CACHED);
            }
        }
        jdbcTemplate.update("""
                UPDATE image_translation_task
                SET status = 'PENDING', display_mode = NULL, cached = 0, prefer_provider = ?,
                    result_image_cos_key = '', result_json_cos_key = '', quality_json_cos_key = '',
                    text_items_json = NULL, provider_task_id = '', provider_request_id = '',
                    quality_score = NULL, fail_reason = '', warning_message = '',
                    finished_at = NULL, updater = 'app', update_time = NOW()
                WHERE id = ?
                """, preferProvider, task.get("id"));
        enqueueAfterCommit(((Number) task.get("id")).longValue(), taskNo);
        return taskResponse(taskNo, Status.PENDING.name(), false, null, MESSAGE_RETRY);
    }

    public void processQueuedTask(Long taskId) {
        Map<String, Object> task = findTaskById(taskId);
        if (task == null || !Status.PENDING.name().equals(string(task, "status"))) {
            return;
        }
        jdbcTemplate.update("UPDATE image_translation_task SET status = 'PROCESSING', updater = 'worker', update_time = NOW() WHERE id = ? AND status = 'PENDING'",
                taskId);
        task = findTaskById(taskId);
        String taskNo = string(task, "task_no");
        String providerName = resolveProvider(string(task, "prefer_provider"));
        try {
            consumeQuota(task);
            ProviderRequest request = buildProviderRequest(task, providerName);
            ImageTranslationProvider provider = fallbackPolicy.primary(providerName);
            ProviderResult result = runProvider(taskId, provider, request);
            result = ensureFinalImageBytes(result);
            QualityResult quality = qualityChecker.check(result);
            ImageTranslationProvider fallback = fallbackPolicy.fallbackAfter(result.getProvider());
            if (quality.getStatus() == QualityStatus.FAIL && fallback != null) {
                ProviderResult fallbackResult = runProvider(taskId, fallback, request.setScene(properties.getBailianImageTranslate().getScene()));
                fallbackResult = ensureFinalImageBytes(fallbackResult);
                QualityResult fallbackQuality = qualityChecker.check(fallbackResult);
                if (fallbackQuality.getQualityScore() >= quality.getQualityScore()) {
                    result = fallbackResult;
                    quality = fallbackQuality;
                }
            } else if (quality.getStatus() == QualityStatus.FAIL && !fallbackPolicy.bailianEnabled()) {
                log.info("image translation bailian disabled taskId={} provider={}", taskNo, result.getProvider());
            }
            persistProviderOutcome(task, result, quality);
        } catch (Exception ex) {
            log.warn("image translation task failed taskId={} user={} rawSha={} reason={}",
                    taskNo, maskUser(numberLong(task.get("user_id"))), rawShaPrefix(string(task, "raw_sha256")), ex.getClass().getSimpleName());
            markFailed(taskId, "task_processing_failed", USER_FAILED_MESSAGE);
        }
    }

    private ProviderRequest buildProviderRequest(Map<String, Object> task, String providerName) {
        String enhancedKey = string(task, "enhanced_cos_key");
        String source = string(task, "source_lang");
        String target = string(task, "target_lang");
        return new ProviderRequest()
                .setTaskId(numberLong(task.get("id")))
                .setTaskNo(string(task, "task_no"))
                .setUserId(numberLong(task.get("user_id")))
                .setTraceId(string(task, "trace_id"))
                .setRawSha256(string(task, "raw_sha256"))
                .setOriginalCosKey(string(task, "original_cos_key"))
                .setEnhancedCosKey(enhancedKey)
                .setEnhancedImageBytes(storageService.getObjectBytes(enhancedKey))
                .setPresignedImageUrl(storageService.presignGetUrl(enhancedKey, storageService.presignedUrlExpireMinutes()))
                .setSourceLang(source)
                .setTargetLang(target)
                .setField(properties.getAliyunTranslateImage().getField())
                .setScene(properties.getBailianImageTranslate().getScene());
    }

    private ProviderResult runProvider(Long taskId, ImageTranslationProvider provider, ProviderRequest request) {
        String requestDigest = hashService.digest(provider.providerName() + ":" + request.getRawSha256() + ":"
                + request.getSourceLang() + ":" + request.getTargetLang());
        ProviderResult result = provider.translate(request);
        String responseDigest = hashService.digest(result.getRawResponseJson());
        jdbcTemplate.update("""
                INSERT INTO image_translation_provider_log(task_id, provider, request_id, provider_task_id,
                                                           request_payload_digest, response_payload_digest,
                                                           http_status, error_code, error_message, cost_ms,
                                                           creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'worker', 'worker')
                """, taskId, provider.providerName(), StrUtil.blankToDefault(result.getRequestId(), ""),
                StrUtil.blankToDefault(result.getProviderTaskId(), ""), requestDigest, responseDigest,
                result.getHttpStatus() > 0 ? result.getHttpStatus() : null,
                StrUtil.blankToDefault(result.getErrorCode(), ""),
                StrUtil.maxLength(StrUtil.blankToDefault(result.getErrorMessage(), ""), 512),
                result.getCostMs());
        return result;
    }

    private ProviderResult ensureFinalImageBytes(ProviderResult result) {
        if (result == null || !result.isSuccess() || result.getFinalImageBytes() != null || StrUtil.isBlank(result.getFinalImageUrl())) {
            return result;
        }
        try {
            result.setFinalImageBytes(storageService.downloadUrl(result.getFinalImageUrl(),
                    Math.max(properties.getAliyunTranslateImage().getTimeoutSeconds(), 10)));
        } catch (Exception ex) {
            result.setFinalImageUrl("");
            result.setSuccess(false);
            result.setErrorCode("final_image_load_failed");
            result.setErrorMessage("Translated image could not be loaded");
        }
        return result;
    }

    private void persistProviderOutcome(Map<String, Object> task, ProviderResult result, QualityResult quality) throws IOException {
        Long taskId = numberLong(task.get("id"));
        String rawSha256 = string(task, "raw_sha256");
        String source = string(task, "source_lang");
        String target = string(task, "target_lang");
        String provider = StrUtil.blankToDefault(result.getProvider(), resolveProvider(string(task, "prefer_provider")));
        String resultJsonKey = storageService.providerResultJsonKey(rawSha256, provider, source, target);
        String qualityJsonKey = storageService.providerQualityJsonKey(rawSha256, provider, source, target);
        storageService.uploadBytes(resultJsonKey,
                StrUtil.blankToDefault(result.getRawResponseJson(), "{}").getBytes(),
                "application/json; charset=utf-8");
        storageService.uploadBytes(qualityJsonKey,
                StrUtil.blankToDefault(quality.getQualityJson(), "{}").getBytes(),
                "application/json; charset=utf-8");
        String resultImageKey = "";
        if (quality.getDisplayMode() == DisplayMode.FINAL_IMAGE
                && result.getFinalImageBytes() != null
                && result.getFinalImageBytes().length > 0) {
            resultImageKey = storageService.providerResultImageKey(rawSha256, provider, source, target, "jpg");
            storageService.uploadBytes(resultImageKey, result.getFinalImageBytes(), "image/jpeg");
        }
        Status status = toTaskStatus(quality);
        String textItemsJson = objectMapper.writeValueAsString(quality.getTextItems());
        jdbcTemplate.update("""
                UPDATE image_translation_task
                SET status = ?, display_mode = ?, provider = ?, provider_task_id = ?, provider_request_id = ?,
                    result_image_cos_key = ?, result_json_cos_key = ?, quality_json_cos_key = ?,
                    text_items_json = ?, quality_score = ?, fail_reason = ?, warning_message = ?,
                    finished_at = NOW(), expire_at = DATE_ADD(NOW(), INTERVAL ? DAY),
                    updater = 'worker', update_time = NOW()
                WHERE id = ?
                """, status.name(), quality.getDisplayMode().name(), provider,
                StrUtil.blankToDefault(result.getProviderTaskId(), ""), StrUtil.blankToDefault(result.getRequestId(), ""),
                resultImageKey, resultJsonKey, qualityJsonKey, textItemsJson, quality.getQualityScore(),
                StrUtil.maxLength(StrUtil.blankToDefault(quality.getFailReason(), ""), 512),
                StrUtil.maxLength(StrUtil.blankToDefault(quality.getWarningMessage(), ""), 512),
                Math.max(1, properties.getResultRetentionDays()), taskId);
        if (status == Status.SUCCESS || status == Status.DEGRADED) {
            upsertCache(task, provider, quality, resultImageKey, resultJsonKey, qualityJsonKey, textItemsJson);
        }
        log.info("image translation task finished taskId={} user={} rawSha={} provider={} status={} displayMode={} quality={} requestId={} providerTaskId={} costMs={}",
                string(task, "task_no"), maskUser(numberLong(task.get("user_id"))), rawShaPrefix(rawSha256), provider,
                status, quality.getDisplayMode(), quality.getStatus(), result.getRequestId(), result.getProviderTaskId(), result.getCostMs());
    }

    private void upsertCache(Map<String, Object> task,
                             String provider,
                             QualityResult quality,
                             String resultImageKey,
                             String resultJsonKey,
                             String qualityJsonKey,
                             String textItemsJson) {
        jdbcTemplate.update("""
                INSERT INTO image_translation_cache(cache_key, raw_sha256, source_lang, target_lang, provider,
                                                    pipeline_version, preprocess_version, render_version,
                                                    display_mode, result_image_cos_key, result_json_cos_key,
                                                    quality_json_cos_key, text_items_json, quality_score,
                                                    warning_message, hit_count, expire_at, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0,
                        DATE_ADD(NOW(), INTERVAL ? DAY), 'worker', 'worker')
                ON DUPLICATE KEY UPDATE
                    display_mode = VALUES(display_mode),
                    result_image_cos_key = VALUES(result_image_cos_key),
                    result_json_cos_key = VALUES(result_json_cos_key),
                    quality_json_cos_key = VALUES(quality_json_cos_key),
                    text_items_json = VALUES(text_items_json),
                    quality_score = VALUES(quality_score),
                    warning_message = VALUES(warning_message),
                    expire_at = VALUES(expire_at),
                    updater = 'worker',
                    update_time = NOW()
                """, string(task, "cache_key"), string(task, "raw_sha256"), string(task, "source_lang"),
                string(task, "target_lang"), provider, properties.getPipelineVersion(), properties.getPreprocessVersion(),
                properties.getRenderVersion(), quality.getDisplayMode().name(), resultImageKey, resultJsonKey,
                qualityJsonKey, textItemsJson, quality.getQualityScore(), quality.getWarningMessage(),
                Math.max(1, properties.getCacheTtlDays()));
    }

    private void consumeQuota(Map<String, Object> task) {
        UsageConsumeReqVO reqVO = new UsageConsumeReqVO();
        reqVO.setClientRequestId("image_translation:" + string(task, "task_no"));
        reqVO.setMode("online");
        reqVO.setScene("image_translation");
        reqVO.setSourceLanguageCode(string(task, "source_lang"));
        reqVO.setTargetLanguageCode(string(task, "target_lang"));
        reqVO.setImageTranslateCount(1);
        appPaymentService.consumeOnlineUsage(numberLong(task.get("user_id")), reqVO);
    }

    private void validateUpload(MultipartFile file, String sourceLang, String targetLang) {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("请先选择图片");
        }
        long maxBytes = Math.max(1, properties.getUploadMaxSizeMb()) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw ServiceExceptionUtil.invalidParamException("图片过大，请压缩后重试");
        }
        String type = StrUtil.blankToDefault(file.getContentType(), "").toLowerCase();
        if (StrUtil.isNotBlank(type) && !type.startsWith("image/")) {
            throw ServiceExceptionUtil.invalidParamException("图片格式暂不支持，请更换图片");
        }
        if (StrUtil.hasBlank(sourceLang, targetLang)) {
            throw ServiceExceptionUtil.invalidParamException("请选择翻译语言");
        }
    }

    private void enqueueAfterCommit(Long taskId, String taskNo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueue(taskId, taskNo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue(taskId, taskNo);
            }
        });
    }

    private void enqueue(Long taskId, String taskNo) {
        if ("local".equalsIgnoreCase(properties.getQueueType())) {
            fallbackExecutor.submit(() -> processQueuedTask(taskId));
            return;
        }
        if (redisMQTemplate != null) {
            ImageTranslationTaskMessage message = new ImageTranslationTaskMessage();
            message.setTaskId(taskId);
            message.setTaskNo(taskNo);
            redisMQTemplate.send(message);
            return;
        }
        fallbackExecutor.submit(() -> processQueuedTask(taskId));
    }

    private void insertTask(Long userId, String taskNo, String traceId, String source, String target,
                            String mode, String preferProvider, String status, String displayMode,
                            boolean cached, String rawSha256, String cacheKey) {
        jdbcTemplate.update("""
                INSERT INTO image_translation_task(user_id, task_no, trace_id, source_lang, target_lang, mode,
                                                   prefer_provider, status, display_mode, cached, raw_sha256,
                                                   cache_key, provider, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'app', 'app')
                """, userId, taskNo, traceId, source, target, mode, preferProvider, status, displayMode,
                cached ? 1 : 0, rawSha256, cacheKey, resolveProvider(preferProvider));
    }

    private String createCachedTask(Long userId, String source, String target, String mode, String preferProvider,
                                    String rawSha256, String cacheKey, Map<String, Object> cache) {
        String taskNo = generateTaskNo();
        insertTask(userId, taskNo, IdUtil.fastSimpleUUID(), source, target, normalizeMode(mode),
                normalizePrefer(preferProvider), Status.SUCCESS.name(), string(cache, "display_mode"),
                true, rawSha256, cacheKey);
        Long taskId = queryTaskId(taskNo);
        jdbcTemplate.update("""
                UPDATE image_translation_task
                SET result_image_cos_key = ?, result_json_cos_key = ?, quality_json_cos_key = ?,
                    text_items_json = ?, provider = ?, quality_score = ?, warning_message = ?,
                    finished_at = NOW(), expire_at = ?, updater = 'app', update_time = NOW()
                WHERE id = ?
                """, string(cache, "result_image_cos_key"), string(cache, "result_json_cos_key"),
                string(cache, "quality_json_cos_key"), string(cache, "text_items_json"),
                string(cache, "provider"), cache.get("quality_score"), string(cache, "warning_message"),
                cache.get("expire_at"), taskId);
        return taskNo;
    }

    private Map<String, Object> findValidCache(String cacheKey) {
        if (!properties.isCacheEnabled()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM image_translation_cache
                WHERE cache_key = ? AND deleted = 0 AND (expire_at IS NULL OR expire_at > NOW())
                LIMIT 1
                """, cacheKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> findActiveTask(Long userId, String cacheKey) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM image_translation_task
                WHERE user_id = ? AND cache_key = ? AND status IN ('PENDING', 'PROCESSING') AND deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """, userId, cacheKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> findTaskByNoAndUser(String taskNo, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM image_translation_task
                WHERE task_no = ? AND user_id = ? AND deleted = 0
                LIMIT 1
                """, taskNo, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> findTaskById(Long taskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM image_translation_task
                WHERE id = ? AND deleted = 0
                LIMIT 1
                """, taskId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long queryTaskId(String taskNo) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM image_translation_task WHERE task_no = ? AND deleted = 0 LIMIT 1",
                    Long.class, taskNo);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Image translation task was not created");
        }
    }

    private void markFailed(Long taskId, String reason, String warning) {
        jdbcTemplate.update("""
                UPDATE image_translation_task
                SET status = 'FAILED', display_mode = 'FAILED', fail_reason = ?, warning_message = ?,
                    finished_at = NOW(), updater = 'worker', update_time = NOW()
                WHERE id = ?
                """, reason, warning, taskId);
    }

    private Status toTaskStatus(QualityResult quality) {
        if (quality.getStatus() == QualityStatus.PASS || quality.getStatus() == QualityStatus.WARN) {
            return Status.SUCCESS;
        }
        if (quality.getStatus() == QualityStatus.FAIL_BUT_HAS_TEXT) {
            return Status.DEGRADED;
        }
        return Status.FAILED;
    }

    private CreateTaskRespVO taskResponse(String taskNo, String status, boolean cached, String displayMode, String message) {
        CreateTaskRespVO respVO = new CreateTaskRespVO();
        respVO.setTaskId(taskNo);
        respVO.setStatus(status);
        respVO.setCached(cached);
        respVO.setDisplayMode(displayMode);
        respVO.setMessage(message);
        return respVO;
    }

    private List<TextItemRespVO> parseTextItems(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TextItemRespVO>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String resolveProvider(String preferProvider) {
        String prefer = normalizePrefer(preferProvider);
        if (AliyunTranslateImageProvider.NAME.equals(prefer)
                && !properties.getAliyunTranslateImage().isEnabled()) {
            return defaultProvider();
        }
        if (!PROVIDER_AUTO.equals(prefer)) {
            return prefer;
        }
        return defaultProvider();
    }

    private String defaultProvider() {
        String provider = StrUtil.blankToDefault(properties.getDefaultProvider(), QwenMtImageProvider.NAME);
        if (AliyunTranslateImageProvider.NAME.equals(provider)
                && !properties.getAliyunTranslateImage().isEnabled()) {
            return QwenMtImageProvider.NAME;
        }
        return provider;
    }

    private String normalizePrefer(String preferProvider) {
        String prefer = StrUtil.blankToDefault(preferProvider, PROVIDER_AUTO).trim();
        return StrUtil.isBlank(prefer) ? PROVIDER_AUTO : prefer;
    }

    private String normalizeMode(String mode) {
        String value = StrUtil.blankToDefault(mode, "AUTO").trim().toUpperCase();
        if (!Objects.equals(value, "AUTO") && !Objects.equals(value, "FAST") && !Objects.equals(value, "HIGH_QUALITY")) {
            return "AUTO";
        }
        return value;
    }

    private String normalizeLang(String lang) {
        return StrUtil.blankToDefault(lang, "").trim();
    }

    private String generateTaskNo() {
        return "IT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase();
    }

    private String contentType(String ext) {
        String value = StrUtil.blankToDefault(ext, "jpg").toLowerCase();
        if ("png".equals(value)) {
            return "image/png";
        }
        if ("webp".equals(value)) {
            return "image/webp";
        }
        if ("bmp".equals(value)) {
            return "image/bmp";
        }
        return "image/jpeg";
    }

    private String string(Map<String, Object> row, String key) {
        Object value = row != null ? row.get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }

    private boolean bool(Map<String, Object> row, String key) {
        Object value = row != null ? row.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Long numberLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }

    private Double doubleOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        return null;
    }

    private String rawShaPrefix(String rawSha256) {
        return rawSha256 != null && rawSha256.length() >= 8 ? rawSha256.substring(0, 8) : "";
    }

    private String maskUser(Long userId) {
        String value = userId == null ? "" : String.valueOf(userId);
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
