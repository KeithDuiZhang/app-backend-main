package cn.iocoder.yudao.server.controller.admin.translation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.service.app.AppClientCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - Translation App")
@RestController
@RequestMapping("/translation/admin")
@Validated
public class AppTranslationAdminController {

    private static final Map<String, ResourceConfig> RESOURCES = createResources();

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private AppClientCommandService clientCommandService;

    @GetMapping("/{resource}/page")
    @Operation(summary = "Page translation app resources")
    public CommonResult<PageResult<Map<String, Object>>> page(@PathVariable String resource,
                                                              @Validated PageReqVO reqVO) {
        ResourceConfig config = getResourceConfig(resource);
        List<Object> args = new ArrayList<>();
        String whereSql = buildWhereSql(config, reqVO, args);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + config.fromSql + whereSql,
                Long.class,
                args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add((reqVO.getPageNo() - 1) * reqVO.getPageSize());
        listArgs.add(reqVO.getPageSize());
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                config.selectSql + whereSql + config.orderSql + " LIMIT ?, ?",
                listArgs.toArray());
        return success(new PageResult<>(list, total == null ? 0L : total));
    }

    @PutMapping("/{resource}/status")
    @Operation(summary = "Update visible status")
    public CommonResult<Boolean> updateStatus(@PathVariable String resource,
                                              @RequestParam Long id,
                                              @RequestParam String status) {
        ResourceConfig config = getResourceConfig(resource);
        if (!config.statusUpdatable) {
            throw new IllegalArgumentException("Resource status is not updatable");
        }
        jdbcTemplate.update("UPDATE " + config.baseTable + " SET status = ?, updater = 'admin', update_time = NOW() WHERE id = ? AND deleted = 0",
                normalizeStatus(config, status), id);
        return success(true);
    }

    @GetMapping("/{resource}/{id}")
    @Operation(summary = "Get translation app resource detail")
    public CommonResult<Map<String, Object>> detail(@PathVariable String resource,
                                                    @PathVariable Long id) {
        ResourceConfig config = getResourceConfig(resource);
        return success(jdbcTemplate.queryForMap(
                config.selectSql + " WHERE " + config.deletedAlias + ".deleted = 0 AND " + config.deletedAlias + ".id = ?",
                id));
    }

    @PostMapping("/{resource}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "Create translation app resource")
    public CommonResult<Boolean> create(@PathVariable String resource,
                                        @RequestBody Map<String, Object> body) {
        if (Objects.equals("users", resource)) {
            createUser(body);
            return success(true);
        }
        if (Objects.equals("token-products", resource)) {
            createOnlinePackage(body);
            return success(true);
        }
        if (Objects.equals("offline-products", resource)) {
            createOfflineProduct(body);
            return success(true);
        }
        throw ServiceExceptionUtil.invalidParamException("当前资源暂不支持新增");
    }

    @PutMapping("/{resource}/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "Update translation app resource")
    public CommonResult<Boolean> update(@PathVariable String resource,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        if (Objects.equals("users", resource)) {
            updateUser(id, body);
            return success(true);
        }
        if (Objects.equals("token-products", resource)) {
            updateOnlinePackage(id, body);
            return success(true);
        }
        if (Objects.equals("offline-products", resource)) {
            updateOfflineProduct(id, body);
            return success(true);
        }
        throw ServiceExceptionUtil.invalidParamException("当前资源暂不支持编辑");
    }

    @DeleteMapping("/{resource}/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "Delete translation app resource")
    public CommonResult<Boolean> delete(@PathVariable String resource,
                                        @PathVariable Long id) {
        if (Objects.equals("users", resource)) {
            softDeleteUser(id);
            return success(true);
        }
        if (Objects.equals("token-products", resource)) {
            jdbcTemplate.update("UPDATE app_token_product SET deleted = 1, status = 0, updater = 'admin', update_time = NOW() WHERE id = ? AND deleted = 0", id);
            return success(true);
        }
        if (Objects.equals("offline-products", resource)) {
            jdbcTemplate.update("UPDATE app_offline_membership_product SET deleted = 1, status = 0, updater = 'admin', update_time = NOW() WHERE id = ? AND deleted = 0", id);
            return success(true);
        }
        throw ServiceExceptionUtil.invalidParamException("当前资源暂不支持删除");
    }

    private void createUser(Map<String, Object> body) {
        String phone = requireString(body, "phone");
        assertPhoneUnique(phone, null);
        String appUserNo = str(body, "appUserNo", "app_user_no");
        if (StrUtil.isBlank(appUserNo)) {
            appUserNo = "APP" + System.currentTimeMillis();
        }
        assertUserNoUnique(appUserNo, null);
        String nickname = StrUtil.blankToDefault(str(body, "nickname"), phone);
        jdbcTemplate.update("""
                INSERT INTO app_user(app_user_no, nickname, phone_cipher, email, auth_status, status,
                                     source_channel, last_login_at, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 'admin', 'admin')
                """, appUserNo, nickname, phone, str(body, "email"),
                intValue(body, 10, "authStatus", "auth_status"),
                statusValue(body, 1),
                StrUtil.blankToDefault(str(body, "sourceChannel", "source_channel"), "admin"));
        Long userId = jdbcTemplate.queryForObject("""
                SELECT id FROM app_user WHERE app_user_no = ? AND deleted = 0 ORDER BY id DESC LIMIT 1
                """, Long.class, appUserNo);
        ensureUserAccount(userId, body);
        upsertPasswordIdentity(userId, phone, str(body, "initialPassword", "password"));
    }

    private void updateUser(Long id, Map<String, Object> body) {
        assertUserExists(id);
        String phone = requireString(body, "phone", "phone_cipher");
        assertPhoneUnique(phone, id);
        String appUserNo = requireString(body, "appUserNo", "app_user_no");
        assertUserNoUnique(appUserNo, id);
        jdbcTemplate.update("""
                UPDATE app_user
                SET app_user_no = ?, nickname = ?, phone_cipher = ?, email = ?, auth_status = ?,
                    status = ?, source_channel = ?, updater = 'admin', update_time = NOW()
                WHERE id = ? AND deleted = 0
                """, appUserNo,
                StrUtil.blankToDefault(str(body, "nickname"), phone),
                phone,
                str(body, "email"),
                intValue(body, 10, "authStatus", "auth_status"),
                statusValue(body, 1),
                StrUtil.blankToDefault(str(body, "sourceChannel", "source_channel"), "admin"),
                id);
        ensureUserAccount(id, body);
        upsertPasswordIdentity(id, phone, str(body, "initialPassword", "password"));
    }

    private void createOnlinePackage(Map<String, Object> body) {
        String skuCode = requireString(body, "skuCode", "sku_code");
        String name = requireString(body, "name");
        jdbcTemplate.update("""
                INSERT INTO app_token_product(sku_code, name, token_amount, bonus_tokens,
                                              text_chars, image_translate_count, ocr_translate_count,
                                              asr_seconds, tts_chars, trial_once, price_cent, currency,
                                              status, sort, creator, updater)
                VALUES (?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'admin', 'admin')
                """, skuCode, name,
                intValue(body, 0, "textChars", "text_chars"),
                intValue(body, 0, "textChars", "text_chars"),
                intValue(body, 0, "imageTranslateCount", "image_translate_count"),
                intValue(body, 0, "ocrTranslateCount", "ocr_translate_count"),
                secondsFromMinutesAware(body),
                intValue(body, 0, "ttsChars", "tts_chars"),
                boolInt(body, "trialOnce", "trial_once"),
                priceCent(body),
                StrUtil.blankToDefault(str(body, "currency"), "CNY"),
                statusValue(body, 1),
                intValue(body, 0, "sort"));
    }

    private void updateOnlinePackage(Long id, Map<String, Object> body) {
        assertExists("app_token_product", id);
        jdbcTemplate.update("""
                UPDATE app_token_product
                SET sku_code = ?, name = ?, token_amount = ?, text_chars = ?, image_translate_count = ?,
                    ocr_translate_count = ?, asr_seconds = ?, tts_chars = ?, trial_once = ?,
                    price_cent = ?, currency = ?, status = ?, sort = ?,
                    updater = 'admin', update_time = NOW()
                WHERE id = ? AND deleted = 0
                """,
                requireString(body, "skuCode", "sku_code"),
                requireString(body, "name"),
                intValue(body, 0, "textChars", "text_chars"),
                intValue(body, 0, "textChars", "text_chars"),
                intValue(body, 0, "imageTranslateCount", "image_translate_count"),
                intValue(body, 0, "ocrTranslateCount", "ocr_translate_count"),
                secondsFromMinutesAware(body),
                intValue(body, 0, "ttsChars", "tts_chars"),
                boolInt(body, "trialOnce", "trial_once"),
                priceCent(body),
                StrUtil.blankToDefault(str(body, "currency"), "CNY"),
                statusValue(body, 1),
                intValue(body, 0, "sort"),
                id);
    }

    private void createOfflineProduct(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO app_offline_membership_product(sku_code, name, duration_days, price_cent,
                                                           currency, description, tag, status, sort,
                                                           creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'admin', 'admin')
                """,
                requireString(body, "skuCode", "sku_code"),
                requireString(body, "name"),
                intValue(body, 0, "durationDays", "duration_days"),
                priceCent(body),
                StrUtil.blankToDefault(str(body, "currency"), "CNY"),
                str(body, "description"),
                str(body, "tag"),
                statusValue(body, 1),
                intValue(body, 0, "sort"));
    }

    private void updateOfflineProduct(Long id, Map<String, Object> body) {
        assertExists("app_offline_membership_product", id);
        jdbcTemplate.update("""
                UPDATE app_offline_membership_product
                SET sku_code = ?, name = ?, duration_days = ?, price_cent = ?, currency = ?,
                    description = ?, tag = ?, status = ?, sort = ?, updater = 'admin', update_time = NOW()
                WHERE id = ? AND deleted = 0
                """,
                requireString(body, "skuCode", "sku_code"),
                requireString(body, "name"),
                intValue(body, 0, "durationDays", "duration_days"),
                priceCent(body),
                StrUtil.blankToDefault(str(body, "currency"), "CNY"),
                str(body, "description"),
                str(body, "tag"),
                statusValue(body, 1),
                intValue(body, 0, "sort"),
                id);
    }

    private void softDeleteUser(Long id) {
        assertUserExists(id);
        List<String> deviceUuids = listActiveDeviceUuids(id);
        clientCommandService.createClearUserCacheCommands(id, deviceUuids);
        softDeleteAppUser(id);
        softDeleteUserIdentity(id);
        softDeleteByUserId("app_user_session", id);
        softDeleteByUserId("app_device", id);
        softDeleteTokenAccount(id);
        softDeleteByUserId("app_token_ledger", id);
        softDeleteByUserId("app_online_quota_ledger", id);
        softDeleteByUserId("app_payment_order", id);
        softDeleteByUserId("app_offline_membership", id);
        softDeleteByUserId("app_translation_record", id);
        softDeleteByUserId("app_favorite", id);
        softDeleteByUserId("app_feedback", id);
    }

    private void softDeleteAppUser(Long userId) {
        StringBuilder sql = new StringBuilder("UPDATE app_user SET deleted = 1");
        if (columnExists("app_user", "status")) {
            sql.append(", status = 0");
        }
        appendTombstoneColumn("app_user", sql, "phone_cipher", 40);
        appendTombstoneColumn("app_user", sql, "app_user_no", 40);
        appendAuditColumns("app_user", sql);
        sql.append(" WHERE id = ? AND deleted = 0");
        jdbcTemplate.update(sql.toString(), userId);
    }

    private List<String> listActiveDeviceUuids(Long userId) {
        if (!hasColumns("app_device", "user_id", "deleted", "device_uuid")) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                SELECT device_uuid FROM app_device
                WHERE user_id = ? AND deleted = 0 AND device_uuid <> ''
                """, String.class, userId);
    }

    private void softDeleteUserIdentity(Long userId) {
        if (!hasColumns("app_user_identity", "user_id", "deleted")) {
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE app_user_identity SET deleted = 1");
        if (columnExists("app_user_identity", "enabled")) {
            sql.append(", enabled = 0");
        }
        appendTombstoneColumn("app_user_identity", sql, "provider_open_id", 80);
        appendAuditColumns("app_user_identity", sql);
        sql.append(" WHERE user_id = ? AND deleted = 0");
        jdbcTemplate.update(sql.toString(), userId);
    }

    private void softDeleteTokenAccount(Long userId) {
        if (!hasColumns("app_token_account", "user_id", "deleted")) {
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE app_token_account SET deleted = 1");
        appendAuditColumns("app_token_account", sql);
        sql.append(" WHERE user_id = ? AND deleted = 0");
        jdbcTemplate.update(sql.toString(), userId);
    }

    private void softDeleteByUserId(String table, Long userId) {
        if (!hasColumns(table, "user_id", "deleted")) {
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET deleted = 1");
        if (Objects.equals(table, "app_device")) {
            appendTombstoneColumn(table, sql, "device_uuid", 80);
        }
        appendAuditColumns(table, sql);
        sql.append(" WHERE user_id = ? AND deleted = 0");
        jdbcTemplate.update(sql.toString(), userId);
    }

    private void appendTombstoneColumn(String table, StringBuilder sql, String column, int prefixLength) {
        if (columnExists(table, column) && columnExists(table, "id")) {
            sql.append(", ").append(column)
                    .append(" = CONCAT(LEFT(").append(column).append(", ")
                    .append(prefixLength).append("), '#D', id)");
        }
    }

    private void appendAuditColumns(String table, StringBuilder sql) {
        if (columnExists(table, "updater")) {
            sql.append(", updater = 'admin'");
        }
        if (columnExists(table, "update_time")) {
            sql.append(", update_time = NOW()");
        }
    }

    private boolean hasColumns(String table, String... columns) {
        if (!tableExists(table)) {
            return false;
        }
        for (String column : columns) {
            if (!columnExists(table, column)) {
                return false;
            }
        }
        return true;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private void ensureUserAccount(Long userId, Map<String, Object> body) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_token_account WHERE user_id = ? AND deleted = 0
                """, Long.class, userId);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO app_token_account(user_id, balance_tokens, trial_tokens, frozen_tokens,
                                                  text_chars_remaining, image_translate_remaining,
                                                  ocr_translate_remaining, asr_seconds_remaining,
                                                  tts_chars_remaining, trial_claimed, current_package_name,
                                                  creator, updater)
                    VALUES (?, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, 'admin', 'admin')
                    """, userId,
                    intValue(body, 0, "textCharsRemaining", "text_chars_remaining"),
                    intValue(body, 0, "textCharsRemaining", "text_chars_remaining"),
                    intValue(body, 0, "imageTranslateRemaining", "image_translate_remaining"),
                    intValue(body, 0, "ocrTranslateRemaining", "ocr_translate_remaining"),
                    secondsRemainingFromMinutesAware(body),
                    intValue(body, 0, "ttsCharsRemaining", "tts_chars_remaining"),
                    boolInt(body, "trialClaimed", "trial_claimed"),
                    str(body, "currentPackageName", "current_package_name"));
            return;
        }
        jdbcTemplate.update("""
                UPDATE app_token_account
                SET balance_tokens = ?, text_chars_remaining = ?, image_translate_remaining = ?,
                    ocr_translate_remaining = ?, asr_seconds_remaining = ?, tts_chars_remaining = ?,
                    trial_claimed = ?, current_package_name = ?, updater = 'admin', update_time = NOW()
                WHERE user_id = ? AND deleted = 0
                """,
                intValue(body, 0, "textCharsRemaining", "text_chars_remaining"),
                intValue(body, 0, "textCharsRemaining", "text_chars_remaining"),
                intValue(body, 0, "imageTranslateRemaining", "image_translate_remaining"),
                intValue(body, 0, "ocrTranslateRemaining", "ocr_translate_remaining"),
                secondsRemainingFromMinutesAware(body),
                intValue(body, 0, "ttsCharsRemaining", "tts_chars_remaining"),
                boolInt(body, "trialClaimed", "trial_claimed"),
                str(body, "currentPackageName", "current_package_name"),
                userId);
    }

    private void upsertPasswordIdentity(Long userId, String phone, String password) {
        if (StrUtil.isBlank(password)) {
            return;
        }
        if (password.trim().length() < 6 || password.trim().length() > 32) {
            throw ServiceExceptionUtil.invalidParamException("初始密码需为 6-32 位");
        }
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_user_identity
                WHERE user_id = ? AND identity_type = 'password' AND provider_open_id = ? AND deleted = 0
                """, Long.class, userId, phone);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE app_user_identity
                    SET credential_hash = ?, enabled = 1, updater = 'admin', update_time = NOW()
                    WHERE user_id = ? AND identity_type = 'password' AND provider_open_id = ? AND deleted = 0
                    """, passwordEncoder.encode(password.trim()), userId, phone);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO app_user_identity(user_id, identity_type, provider_open_id, credential_hash, enabled, creator, updater)
                VALUES (?, 'password', ?, ?, 1, 'admin', 'admin')
                """, userId, phone, passwordEncoder.encode(password.trim()));
    }

    private void assertUserExists(Long id) {
        assertExists("app_user", id);
    }

    private void assertExists(String table, Long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + table + " WHERE id = ? AND deleted = 0",
                Long.class,
                id);
        if (count == null || count == 0) {
            throw ServiceExceptionUtil.invalidParamException("数据不存在或已删除");
        }
    }

    private void assertPhoneUnique(String phone, Long exceptUserId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_user
                WHERE phone_cipher = ? AND deleted = 0 AND (? IS NULL OR id <> ?)
                """, Long.class, phone, exceptUserId, exceptUserId);
        if (count != null && count > 0) {
            throw ServiceExceptionUtil.invalidParamException("手机号已存在");
        }
    }

    private void assertUserNoUnique(String appUserNo, Long exceptUserId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_user
                WHERE app_user_no = ? AND deleted = 0 AND (? IS NULL OR id <> ?)
                """, Long.class, appUserNo, exceptUserId, exceptUserId);
        if (count != null && count > 0) {
            throw ServiceExceptionUtil.invalidParamException("用户编号已存在");
        }
    }

    private static String requireString(Map<String, Object> body, String... keys) {
        String value = str(body, keys);
        if (StrUtil.isBlank(value)) {
            throw ServiceExceptionUtil.invalidParamException("请填写必填字段");
        }
        return value.trim();
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null) {
            return "";
        }
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private static int intValue(Map<String, Object> body, int defaultValue, String... keys) {
        if (body == null) {
            return defaultValue;
        }
        for (String key : keys) {
            Object value = body.get(key);
            if (value == null || String.valueOf(value).trim().length() == 0) {
                continue;
            }
            if (value instanceof Number number) {
                return Math.max(0, number.intValue());
            }
            try {
                return Math.max(0, (int) Math.round(Double.parseDouble(String.valueOf(value).trim())));
            } catch (NumberFormatException ignored) {
                throw ServiceExceptionUtil.invalidParamException("数字字段格式不正确");
            }
        }
        return defaultValue;
    }

    private static int statusValue(Map<String, Object> body, int defaultValue) {
        int status = intValue(body, defaultValue, "status");
        return status == 0 ? 0 : 1;
    }

    private static int boolInt(Map<String, Object> body, String... keys) {
        if (body == null) {
            return 0;
        }
        for (String key : keys) {
            Object value = body.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Boolean bool) {
                return bool ? 1 : 0;
            }
            String text = String.valueOf(value).trim();
            return ("1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) ? 1 : 0;
        }
        return 0;
    }

    private static int priceCent(Map<String, Object> body) {
        String priceYuan = str(body, "priceYuan", "price_yuan");
        if (StrUtil.isNotBlank(priceYuan)) {
            try {
                return Math.max(0, (int) Math.round(Double.parseDouble(priceYuan) * 100));
            } catch (NumberFormatException ignored) {
                throw ServiceExceptionUtil.invalidParamException("价格格式不正确");
            }
        }
        return intValue(body, 0, "priceCent", "price_cent");
    }

    private static int secondsFromMinutesAware(Map<String, Object> body) {
        String minutes = str(body, "asrMinutes", "asr_minutes");
        if (StrUtil.isNotBlank(minutes)) {
            try {
                return Math.max(0, (int) Math.round(Double.parseDouble(minutes) * 60));
            } catch (NumberFormatException ignored) {
                throw ServiceExceptionUtil.invalidParamException("语音识别分钟数格式不正确");
            }
        }
        return intValue(body, 0, "asrSeconds", "asr_seconds");
    }

    private static int secondsRemainingFromMinutesAware(Map<String, Object> body) {
        String minutes = str(body, "asrMinutesRemaining", "asr_minutes_remaining");
        if (StrUtil.isNotBlank(minutes)) {
            try {
                return Math.max(0, (int) Math.round(Double.parseDouble(minutes) * 60));
            } catch (NumberFormatException ignored) {
                throw ServiceExceptionUtil.invalidParamException("语音识别剩余分钟数格式不正确");
            }
        }
        return intValue(body, 0, "asrSecondsRemaining", "asr_seconds_remaining");
    }

    private static Object normalizeStatus(ResourceConfig config, String status) {
        if (config.numericStatus) {
            return Integer.parseInt(status);
        }
        return status;
    }

    private static ResourceConfig getResourceConfig(String resource) {
        ResourceConfig config = RESOURCES.get(resource);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported resource: " + resource);
        }
        return config;
    }

    private static String buildWhereSql(ResourceConfig config, PageReqVO reqVO, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE ").append(config.deletedAlias).append(".deleted = 0");
        if (StrUtil.isNotBlank(reqVO.getKeyword()) && StrUtil.isNotBlank(config.keywordSql)) {
            where.append(" AND (").append(config.keywordSql).append(")");
            for (int i = 0; i < config.keywordFieldCount; i++) {
                args.add("%" + reqVO.getKeyword().trim() + "%");
            }
        }
        if (StrUtil.isNotBlank(reqVO.getStatus()) && StrUtil.isNotBlank(config.statusColumn)) {
            where.append(" AND ").append(config.statusColumn).append(" = ?");
            args.add(normalizeStatus(config, reqVO.getStatus()));
        }
        if (reqVO.getUserId() != null && StrUtil.isNotBlank(config.userIdColumn)) {
            where.append(" AND ").append(config.userIdColumn).append(" = ?");
            args.add(reqVO.getUserId());
        }
        return where.toString();
    }

    private static Map<String, ResourceConfig> createResources() {
        Map<String, ResourceConfig> map = new LinkedHashMap<>();
        map.put("users", ResourceConfig.of("app_user", "u", """
                SELECT u.id, u.app_user_no, u.nickname, u.phone_cipher, u.email, u.auth_status, u.status,
                       u.source_channel, u.last_login_at, u.create_time, u.update_time,
                       COALESCE(a.text_chars_remaining, 0) AS text_chars_remaining,
                       COALESCE(a.image_translate_remaining, 0) AS image_translate_remaining,
                       COALESCE(a.ocr_translate_remaining, 0) AS ocr_translate_remaining,
                       COALESCE(a.asr_seconds_remaining, 0) AS asr_seconds_remaining,
                       COALESCE(a.tts_chars_remaining, 0) AS tts_chars_remaining,
                       COALESCE(a.current_package_name, '') AS current_package_name,
                       COALESCE(a.trial_claimed, 0) AS trial_claimed
                FROM app_user u
                LEFT JOIN app_token_account a ON a.user_id = u.id AND a.deleted = 0
                """, "u.app_user_no LIKE ? OR u.nickname LIKE ? OR u.phone_cipher LIKE ? OR u.email LIKE ?", 4,
                "u.status", true, "u.id", false, false));
        map.put("token-products", ResourceConfig.of("app_token_product", "p", """
                SELECT p.id, p.sku_code, p.name, p.text_chars, p.image_translate_count,
                       p.ocr_translate_count, p.asr_seconds, p.tts_chars, p.trial_once,
                       p.price_cent, p.currency, p.status, p.sort, p.create_time, p.update_time
                FROM app_token_product p
                """, "p.sku_code LIKE ? OR p.name LIKE ?", 2,
                "p.status", true, null, true, true));
        map.put("offline-products", ResourceConfig.of("app_offline_membership_product", "p", """
                SELECT p.id, p.sku_code, p.name, p.duration_days, p.price_cent, p.currency, p.description,
                       p.tag, p.status, p.sort, p.create_time, p.update_time
                FROM app_offline_membership_product p
                """, "p.sku_code LIKE ? OR p.name LIKE ? OR p.tag LIKE ?", 3,
                "p.status", true, null, true, true));
        map.put("orders", ResourceConfig.of("app_payment_order", "o", """
                SELECT o.id, o.order_no, o.user_id, u.nickname, o.product_type, o.product_id,
                       COALESCE(tp.name, mp.name, '') AS product_name,
                       o.pay_channel, o.status, o.amount_cent, o.provider_trade_no,
                       o.paid_at, o.create_time, o.update_time
                FROM app_payment_order o
                LEFT JOIN app_user u ON u.id = o.user_id AND u.deleted = 0
                LEFT JOIN app_token_product tp ON o.product_type IN ('online_package', 'token') AND tp.id = o.product_id AND tp.deleted = 0
                LEFT JOIN app_offline_membership_product mp ON o.product_type IN ('offline_membership', 'offline_vip') AND mp.id = o.product_id AND mp.deleted = 0
                """, "o.order_no LIKE ? OR u.nickname LIKE ? OR o.provider_trade_no LIKE ?", 3,
                "o.status", false, "o.user_id", false, false));
        map.put("memberships", ResourceConfig.of("app_offline_membership", "m", """
                SELECT m.id, m.user_id, u.nickname, m.plan_code, m.status, m.started_at,
                       m.expired_at, m.source_order_no, m.create_time, m.update_time
                FROM app_offline_membership m
                LEFT JOIN app_user u ON u.id = m.user_id AND u.deleted = 0
                """, "m.plan_code LIKE ? OR m.source_order_no LIKE ? OR u.nickname LIKE ?", 3,
                "m.status", false, "m.user_id", false, false));
        map.put("records", ResourceConfig.of("app_translation_record", "r", """
                SELECT r.id, r.user_id, u.nickname, r.device_uuid, r.mode, r.scene,
                       r.source_language_code, r.target_language_code, r.source_text, r.translated_text,
                       r.engine_provider, r.text_chars_used, r.image_translate_count_used,
                       r.ocr_translate_count_used, r.asr_seconds_used, r.tts_chars_used,
                       r.status, r.error_code, r.create_time
                FROM app_translation_record r
                LEFT JOIN app_user u ON u.id = r.user_id AND u.deleted = 0
                """, "u.nickname LIKE ? OR r.device_uuid LIKE ? OR r.source_text LIKE ? OR r.translated_text LIKE ?", 4,
                "r.status", false, "r.user_id", false, false));
        map.put("favorites", ResourceConfig.of("app_favorite", "f", """
                SELECT f.id, f.user_id, u.nickname, f.record_id, f.title, f.source_language_code,
                       f.target_language_code, f.source_text, f.translated_text, f.create_time
                FROM app_favorite f
                LEFT JOIN app_user u ON u.id = f.user_id AND u.deleted = 0
                """, "u.nickname LIKE ? OR f.title LIKE ? OR f.source_text LIKE ? OR f.translated_text LIKE ?", 4,
                null, false, "f.user_id", false, false));
        map.put("feedback", ResourceConfig.of("app_feedback", "f", """
                SELECT f.id, f.user_id, u.nickname, f.device_uuid, f.category, f.content, f.contact,
                       f.status, f.handler_remark, f.create_time, f.update_time
                FROM app_feedback f
                LEFT JOIN app_user u ON u.id = f.user_id AND u.deleted = 0
                """, "u.nickname LIKE ? OR f.device_uuid LIKE ? OR f.content LIKE ? OR f.contact LIKE ?", 4,
                "f.status", false, "f.user_id", true, false));
        map.put("language-packs", ResourceConfig.of("app_language_pack", "p", """
                SELECT p.id, p.language_code, p.language_name_zh, p.language_name_en, p.pack_type,
                       p.engine_group, p.version, p.status, p.size_bytes, p.download_url,
                       p.min_ram_mb, p.device_advice, p.verified_at, p.create_time, p.update_time
                FROM app_language_pack p
                """, "p.language_code LIKE ? OR p.language_name_zh LIKE ? OR p.language_name_en LIKE ? OR p.engine_group LIKE ?", 4,
                "p.status", false, null, false, false));
        map.put("capabilities", ResourceConfig.of("app_capability_matrix", "c", """
                SELECT c.id, c.mode, c.scene, c.source_language_code, c.target_language_code, c.status,
                       c.provider, c.display_badge, c.show_in_app, c.tts_available, c.priority,
                       c.notes, c.verified_at, c.create_time, c.update_time
                FROM app_capability_matrix c
                """, "c.mode LIKE ? OR c.scene LIKE ? OR c.source_language_code LIKE ? OR c.target_language_code LIKE ? OR c.provider LIKE ?", 5,
                "c.status", false, null, false, false));
        return map;
    }

    @Data
    public static class PageReqVO {
        @Min(1)
        private Integer pageNo = 1;
        @Min(1)
        @Max(100)
        private Integer pageSize = 10;
        private String keyword;
        private String status;
        private Long userId;
    }

    private record ResourceConfig(String baseTable, String deletedAlias, String selectSql, String fromSql,
                                  String keywordSql, int keywordFieldCount, String statusColumn,
                                  boolean numericStatus, String userIdColumn, boolean statusUpdatable,
                                  String orderSql) {

        static ResourceConfig of(String baseTable, String deletedAlias, String selectSql, String keywordSql,
                                 int keywordFieldCount, String statusColumn, boolean numericStatus,
                                 String userIdColumn, boolean statusUpdatable, boolean sortFirst) {
            String orderSql = sortFirst
                    ? " ORDER BY " + deletedAlias + ".sort ASC, " + deletedAlias + ".id DESC"
                    : " ORDER BY " + deletedAlias + ".create_time DESC, " + deletedAlias + ".id DESC";
            return new ResourceConfig(baseTable, deletedAlias, selectSql, selectSql.substring(selectSql.indexOf("FROM")),
                    keywordSql, keywordFieldCount, statusColumn, numericStatus, userIdColumn, statusUpdatable, orderSql);
        }
    }

}
