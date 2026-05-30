package cn.iocoder.yudao.server.service.app;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AppPaymentService {

    public static final String PRODUCT_TYPE_ONLINE_PACKAGE = "online_package";
    public static final String PRODUCT_TYPE_OFFLINE_MEMBERSHIP = "offline_membership";
    public static final String LEGACY_PRODUCT_TYPE_TOKEN = "token";
    public static final String LEGACY_PRODUCT_TYPE_OFFLINE_VIP = "offline_vip";
    @Deprecated
    public static final String PRODUCT_TYPE_TOKEN = LEGACY_PRODUCT_TYPE_TOKEN;
    @Deprecated
    public static final String PRODUCT_TYPE_OFFLINE_VIP = LEGACY_PRODUCT_TYPE_OFFLINE_VIP;

    private static final String STATUS_PAYING = "paying";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_FAILED = "failed";

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private AppIntegrationConfigService integrationConfigService;

    public List<TokenProductRespVO> listTokenProducts() {
        return jdbcTemplate.query("""
                SELECT id, sku_code, name,
                       text_chars, image_translate_count, ocr_translate_count,
                       asr_seconds, tts_chars, trial_once + 0 AS trial_once, price_cent, currency
                FROM app_token_product
                WHERE status = 1 AND deleted = 0
                ORDER BY sort ASC, id ASC
                """, (rs, rowNum) -> {
            TokenProductRespVO respVO = new TokenProductRespVO();
            respVO.setId(rs.getLong("id"));
            respVO.setSkuCode(rs.getString("sku_code"));
            respVO.setName(rs.getString("name"));
            respVO.setTextChars(rs.getInt("text_chars"));
            respVO.setImageTranslateCount(rs.getInt("image_translate_count"));
            respVO.setOcrTranslateCount(rs.getInt("ocr_translate_count"));
            respVO.setAsrSeconds(rs.getInt("asr_seconds"));
            respVO.setTtsChars(rs.getInt("tts_chars"));
            respVO.setTrialOnce(rs.getBoolean("trial_once"));
            respVO.setPriceCent(rs.getInt("price_cent"));
            respVO.setCurrency(rs.getString("currency"));
            return respVO;
        });
    }

    public List<OfflineMembershipProductRespVO> listOfflineMembershipProducts() {
        return jdbcTemplate.query("""
                SELECT id, sku_code, name, duration_days, price_cent, currency, description, tag
                FROM app_offline_membership_product
                WHERE status = 1 AND deleted = 0
                ORDER BY sort ASC, id ASC
                """, (rs, rowNum) -> {
            OfflineMembershipProductRespVO respVO = new OfflineMembershipProductRespVO();
            respVO.setId(rs.getLong("id"));
            respVO.setSkuCode(rs.getString("sku_code"));
            respVO.setName(rs.getString("name"));
            respVO.setDurationDays(rs.getInt("duration_days"));
            respVO.setPriceCent(rs.getInt("price_cent"));
            respVO.setCurrency(rs.getString("currency"));
            respVO.setDescription(rs.getString("description"));
            respVO.setTag(rs.getString("tag"));
            return respVO;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateWapPayRespVO createAlipayWapOrder(Long userId, CreateWapPayReqVO reqVO, String userIp) {
        String productType = normalizeProductType(reqVO.getProductType());
        Map<String, Object> product = getProduct(reqVO, productType);
        Integer amountCent = ((Number) product.get("price_cent")).intValue();
        if (amountCent > 0) {
            validateAlipayRuntimeConfigured();
        }
        String orderNo = generateOrderNo();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
        String clientType = StrUtil.blankToDefault(reqVO.getClientType(), "web");

        jdbcTemplate.update("""
                INSERT INTO app_payment_order(order_no, user_id, product_type, product_id, client_type,
                                              pay_channel, pay_channel_code, status, amount_cent, expire_time,
                                              creator, updater)
                VALUES (?, ?, ?, ?, ?, 'alipay', 'alipay_wap', ?, ?, ?, 'app', 'app')
                """, orderNo, userId, productType, product.get("id"), clientType, STATUS_PAYING, amountCent, expireTime);

        String productName = String.valueOf(product.get("name"));
        String payUrl;
        String status = STATUS_PAYING;
        if (amountCent == 0) {
            grantPaidOrderBenefit(findOrderForUpdate(orderNo), "FREE-TRIAL", "FREE_TRIAL");
            status = STATUS_PAID;
            payUrl = StrUtil.blankToDefault(reqVO.getReturnUrl(),
                    StrUtil.blankToDefault(integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_RETURN_URL), ""));
        } else {
            payUrl = requestAlipayWapUrl(orderNo, productName, orderBody(productType),
                    amountCent, expireTime, reqVO.getReturnUrl(), userIp);
        }

        CreateWapPayRespVO respVO = new CreateWapPayRespVO();
        respVO.setOrderNo(orderNo);
        respVO.setPayUrl(payUrl);
        respVO.setDisplayMode("url");
        respVO.setStatus(status);
        respVO.setAmountCent(amountCent);
        respVO.setProductType(productType);
        respVO.setProductName(productName);
        respVO.setExpireTime(expireTime);
        return respVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotify(Map<String, String> params) {
        if (!verifyAlipayNotify(params)) {
            return false;
        }
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = StrUtil.blankToDefault(params.get("trade_no"), "");
        String tradeStatus = StrUtil.blankToDefault(params.get("trade_status"), "");
        String sellerId = StrUtil.blankToDefault(params.get("seller_id"), "");
        String totalAmount = params.get("total_amount");

        Map<String, Object> order;
        try {
            order = jdbcTemplate.queryForMap("""
                    SELECT id, order_no, user_id, product_type, product_id, status, amount_cent
                    FROM app_payment_order
                    WHERE order_no = ? AND deleted = 0
                    LIMIT 1
                    FOR UPDATE
                    """, outTradeNo);
        } catch (EmptyResultDataAccessException ex) {
            log.warn("[handleAlipayNotify][order not found][orderNo={}]", outTradeNo);
            return false;
        }

        if (!isAmountMatched(((Number) order.get("amount_cent")).intValue(), totalAmount)) {
            log.warn("[handleAlipayNotify][amount mismatch][orderNo={}]", outTradeNo);
            return false;
        }
        if (!isSellerMatched(sellerId)) {
            log.warn("[handleAlipayNotify][seller mismatch][orderNo={}]", outTradeNo);
            return false;
        }

        String currentStatus = String.valueOf(order.get("status"));
        if (Objects.equals(STATUS_PAID, currentStatus)) {
            jdbcTemplate.update("""
                    UPDATE app_payment_order
                    SET provider_trade_no = ?, alipay_trade_status = ?, updater = 'alipay', update_time = NOW()
                    WHERE id = ?
                    """, tradeNo, tradeStatus, order.get("id"));
            return true;
        }

        if (Objects.equals("TRADE_SUCCESS", tradeStatus) || Objects.equals("TRADE_FINISHED", tradeStatus)) {
            grantPaidOrderBenefit(order, tradeNo, tradeStatus);
            return true;
        }
        if (Objects.equals("TRADE_CLOSED", tradeStatus)) {
            jdbcTemplate.update("""
                    UPDATE app_payment_order
                    SET status = ?, provider_trade_no = ?, alipay_trade_status = ?, closed_at = NOW(),
                        updater = 'alipay', update_time = NOW()
                    WHERE id = ? AND status <> ?
                    """, STATUS_CLOSED, tradeNo, tradeStatus, order.get("id"), STATUS_PAID);
            return true;
        }
        if (Objects.equals("WAIT_BUYER_PAY", tradeStatus)) {
            jdbcTemplate.update("""
                    UPDATE app_payment_order
                    SET provider_trade_no = ?, alipay_trade_status = ?, updater = 'alipay', update_time = NOW()
                    WHERE id = ?
                    """, tradeNo, tradeStatus, order.get("id"));
            return true;
        }
        log.warn("[handleAlipayNotify][unsupported status][orderNo={}, status={}]", outTradeNo, tradeStatus);
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderStatusRespVO getOrderStatus(Long userId, String orderNo) {
        syncAlipayTradeStatus(userId, orderNo);
        closeExpiredOrder(orderNo);
        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap("""
                    SELECT o.order_no, o.product_type, o.status, o.amount_cent, o.provider_trade_no,
                           o.paid_at, o.expire_time,
                           COALESCE(tp.name, mp.name, '') AS product_name
                    FROM app_payment_order o
                    LEFT JOIN app_token_product tp
                      ON o.product_type IN ('online_package', 'token') AND tp.id = o.product_id AND tp.deleted = 0
                    LEFT JOIN app_offline_membership_product mp
                      ON o.product_type IN ('offline_membership', 'offline_vip') AND mp.id = o.product_id AND mp.deleted = 0
                    WHERE o.order_no = ? AND o.user_id = ? AND o.deleted = 0
                    LIMIT 1
                    """, orderNo, userId);
        } catch (EmptyResultDataAccessException ex) {
            throw ServiceExceptionUtil.invalidParamException("订单不存在");
        }
        OrderStatusRespVO respVO = new OrderStatusRespVO();
        respVO.setOrderNo(String.valueOf(row.get("order_no")));
        respVO.setProductType(String.valueOf(row.get("product_type")));
        respVO.setStatus(String.valueOf(row.get("status")));
        respVO.setAmountCent(((Number) row.get("amount_cent")).intValue());
        respVO.setProviderTradeNo(StrUtil.nullToDefault((String) row.get("provider_trade_no"), ""));
        respVO.setProductName(StrUtil.nullToDefault((String) row.get("product_name"), ""));
        respVO.setPaidAt(toLocalDateTime(row.get("paid_at")));
        respVO.setExpireTime(toLocalDateTime(row.get("expire_time")));
        return respVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserEntitlementRespVO getUserEntitlement(Long userId) {
        UserEntitlementRespVO respVO = new UserEntitlementRespVO();
        try {
            Map<String, Object> account = jdbcTemplate.queryForMap("""
                    SELECT balance_tokens, trial_tokens, frozen_tokens,
                           text_chars_remaining, image_translate_remaining, ocr_translate_remaining,
                           asr_seconds_remaining, tts_chars_remaining, trial_claimed + 0 AS trial_claimed,
                           current_package_name
                    FROM app_token_account
                    WHERE user_id = ? AND deleted = 0
                    LIMIT 1
                    """, userId);
            respVO.setBalanceTokens(((Number) account.get("balance_tokens")).intValue());
            respVO.setTrialTokens(((Number) account.get("trial_tokens")).intValue());
            respVO.setFrozenTokens(((Number) account.get("frozen_tokens")).intValue());
            respVO.setTextCharsRemaining(((Number) account.get("text_chars_remaining")).intValue());
            respVO.setImageTranslateRemaining(((Number) account.get("image_translate_remaining")).intValue());
            respVO.setOcrTranslateRemaining(((Number) account.get("ocr_translate_remaining")).intValue());
            respVO.setAsrSecondsRemaining(((Number) account.get("asr_seconds_remaining")).intValue());
            respVO.setTtsCharsRemaining(((Number) account.get("tts_chars_remaining")).intValue());
            respVO.setTrialClaimed(((Number) account.get("trial_claimed")).intValue() == 1);
            respVO.setCurrentPackageName(StrUtil.nullToDefault((String) account.get("current_package_name"), ""));
        } catch (EmptyResultDataAccessException ignored) {
            respVO.setBalanceTokens(0);
            respVO.setTrialTokens(0);
            respVO.setFrozenTokens(0);
            respVO.setTextCharsRemaining(0);
            respVO.setImageTranslateRemaining(0);
            respVO.setOcrTranslateRemaining(0);
            respVO.setAsrSecondsRemaining(0);
            respVO.setTtsCharsRemaining(0);
            respVO.setTrialClaimed(false);
            respVO.setCurrentPackageName("");
        }
        try {
            Map<String, Object> membership = jdbcTemplate.queryForMap("""
                    SELECT plan_code, status, started_at, expired_at
                    FROM app_offline_membership
                    WHERE user_id = ? AND status = 'active' AND deleted = 0
                      AND (expired_at IS NULL OR expired_at > NOW())
                    ORDER BY expired_at IS NULL DESC, expired_at DESC
                    LIMIT 1
                    """, userId);
            respVO.setHasOfflineMembership(true);
            respVO.setOfflinePlanCode(String.valueOf(membership.get("plan_code")));
            respVO.setOfflineMembershipStatus(String.valueOf(membership.get("status")));
            respVO.setOfflineExpiredAt(toLocalDateTime(membership.get("expired_at")));
        } catch (EmptyResultDataAccessException ignored) {
            respVO.setHasOfflineMembership(false);
            respVO.setOfflineMembershipStatus("none");
        }
        return respVO;
    }

    public List<UsageRecordRespVO> listUsageRecords(Long userId) {
        ArrayList<UsageRecordRespVO> records = new ArrayList<>();
        records.addAll(jdbcTemplate.query("""
                SELECT mode, scene, status, text_chars_used, image_translate_count_used,
                       ocr_translate_count_used, asr_seconds_used, tts_chars_used, create_time
                FROM app_translation_record
                WHERE user_id = ? AND deleted = 0
                ORDER BY create_time DESC, id DESC
                LIMIT 30
                """, (rs, rowNum) -> {
            UsageRecordRespVO respVO = new UsageRecordRespVO();
            respVO.setRecordType("usage");
            respVO.setMode(StrUtil.nullToDefault(rs.getString("mode"), "online"));
            respVO.setScene(StrUtil.nullToDefault(rs.getString("scene"), "online_usage"));
            respVO.setStatus(StrUtil.nullToDefault(rs.getString("status"), "success"));
            respVO.setTextCharsUsed(rs.getInt("text_chars_used"));
            respVO.setImageTranslateCountUsed(rs.getInt("image_translate_count_used"));
            respVO.setOcrTranslateCountUsed(rs.getInt("ocr_translate_count_used"));
            respVO.setAsrSecondsUsed(rs.getInt("asr_seconds_used"));
            respVO.setTtsCharsUsed(rs.getInt("tts_chars_used"));
            respVO.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
            return respVO;
        }, userId));
        records.addAll(jdbcTemplate.query("""
                SELECT o.product_type, o.status, o.amount_cent,
                       COALESCE(tp.name, mp.name, '') AS product_name,
                       COALESCE(o.paid_at, o.create_time) AS record_time
                FROM app_payment_order o
                LEFT JOIN app_token_product tp
                  ON o.product_type IN ('online_package', 'token') AND tp.id = o.product_id AND tp.deleted = 0
                LEFT JOIN app_offline_membership_product mp
                  ON o.product_type IN ('offline_membership', 'offline_vip') AND mp.id = o.product_id AND mp.deleted = 0
                WHERE o.user_id = ? AND o.status = ? AND o.deleted = 0
                ORDER BY COALESCE(o.paid_at, o.create_time) DESC, o.id DESC
                LIMIT 30
                """, (rs, rowNum) -> {
            UsageRecordRespVO respVO = new UsageRecordRespVO();
            respVO.setRecordType("order");
            respVO.setProductType(normalizeStoredProductType(rs.getString("product_type")));
            respVO.setProductName(StrUtil.nullToDefault(rs.getString("product_name"), ""));
            respVO.setStatus(StrUtil.nullToDefault(rs.getString("status"), STATUS_PAID));
            respVO.setAmountCent(rs.getInt("amount_cent"));
            respVO.setCreateTime(toLocalDateTime(rs.getTimestamp("record_time")));
            return respVO;
        }, userId, STATUS_PAID));
        records.sort((left, right) -> {
            LocalDateTime leftTime = left.getCreateTime() == null ? LocalDateTime.MIN : left.getCreateTime();
            LocalDateTime rightTime = right.getCreateTime() == null ? LocalDateTime.MIN : right.getCreateTime();
            return rightTime.compareTo(leftTime);
        });
        if (records.size() <= 30) {
            return records;
        }
        return new ArrayList<>(records.subList(0, 30));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserEntitlementRespVO consumeOnlineUsage(Long userId, UsageConsumeReqVO reqVO) {
        String clientRequestId = StrUtil.trimToEmpty(reqVO.getClientRequestId());
        if (StrUtil.isBlank(clientRequestId)) {
            throw ServiceExceptionUtil.invalidParamException("Missing usage request id");
        }
        Long existing = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_online_quota_ledger
                WHERE user_id = ? AND direction = 'outcome' AND business_id = ? AND deleted = 0
                """, Long.class, userId, clientRequestId);
        if (existing != null && existing > 0) {
            return getUserEntitlement(userId);
        }
        Map<String, Object> account = getOrCreateTokenAccount(userId);
        int text = Math.max(0, reqVO.getTextChars());
        int image = Math.max(0, reqVO.getImageTranslateCount());
        int ocr = Math.max(0, reqVO.getOcrTranslateCount());
        int asr = Math.max(0, reqVO.getAsrSeconds());
        int tts = Math.max(0, reqVO.getTtsChars());
        int textAfter = ((Number) account.get("text_chars_remaining")).intValue() - text;
        int imageAfter = ((Number) account.get("image_translate_remaining")).intValue() - image;
        int ocrAfter = ((Number) account.get("ocr_translate_remaining")).intValue() - ocr;
        int asrAfter = ((Number) account.get("asr_seconds_remaining")).intValue() - asr;
        int ttsAfter = ((Number) account.get("tts_chars_remaining")).intValue() - tts;
        if (textAfter < 0 || imageAfter < 0 || ocrAfter < 0 || asrAfter < 0 || ttsAfter < 0) {
            throw ServiceExceptionUtil.invalidParamException("在线套餐余额不足，请先开通或续费套餐");
        }
        Long accountId = ((Number) account.get("id")).longValue();
        jdbcTemplate.update("""
                UPDATE app_token_account
                SET balance_tokens = ?, text_chars_remaining = ?, image_translate_remaining = ?,
                    ocr_translate_remaining = ?, asr_seconds_remaining = ?, tts_chars_remaining = ?,
                    updater = 'app', update_time = NOW()
                WHERE id = ?
                """, textAfter, textAfter, imageAfter, ocrAfter, asrAfter, ttsAfter, accountId);
        jdbcTemplate.update("""
                INSERT INTO app_online_quota_ledger(account_id, user_id, direction,
                                                     text_chars_delta, image_translate_delta,
                                                     ocr_translate_delta, asr_seconds_delta, tts_chars_delta,
                                                     text_chars_after, image_translate_after,
                                                     ocr_translate_after, asr_seconds_after, tts_chars_after,
                                                     business_type, business_id, remark, creator, updater)
                VALUES (?, ?, 'outcome', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '在线能力使用', 'app', 'app')
                """, accountId, userId, -text, -image, -ocr, -asr, -tts,
                textAfter, imageAfter, ocrAfter, asrAfter, ttsAfter,
                StrUtil.blankToDefault(reqVO.getScene(), "online_usage"), clientRequestId);
        jdbcTemplate.update("""
                INSERT INTO app_translation_record(user_id, device_uuid, mode, scene,
                                                   source_language_code, target_language_code,
                                                   source_text, translated_text, engine_provider,
                                                   cost_tokens, text_chars_used, image_translate_count_used,
                                                   ocr_translate_count_used, asr_seconds_used, tts_chars_used,
                                                   client_request_id, status, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'aliyun', ?, ?, ?, ?, ?, ?, ?, 'success', 'app', 'app')
                """, userId,
                StrUtil.blankToDefault(reqVO.getDeviceUuid(), ""),
                StrUtil.blankToDefault(reqVO.getMode(), "online"),
                StrUtil.blankToDefault(reqVO.getScene(), "online_usage"),
                StrUtil.blankToDefault(reqVO.getSourceLanguageCode(), ""),
                StrUtil.blankToDefault(reqVO.getTargetLanguageCode(), ""),
                StrUtil.blankToDefault(reqVO.getSourceText(), ""),
                StrUtil.blankToDefault(reqVO.getTranslatedText(), ""),
                text, text, image, ocr, asr, tts, clientRequestId);
        return getUserEntitlement(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean submitFeedback(Long userId, FeedbackSubmitReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getContent())) {
            throw ServiceExceptionUtil.invalidParamException("Feedback content is required");
        }
        jdbcTemplate.update("""
                INSERT INTO app_feedback(user_id, device_uuid, category, content, contact, status,
                                         creator, updater)
                VALUES (?, ?, ?, ?, ?, 'open', 'app', 'app')
                """, userId,
                StrUtil.blankToDefault(reqVO.getDeviceUuid(), ""),
                StrUtil.blankToDefault(reqVO.getCategory(), "other"),
                StrUtil.trim(reqVO.getContent()),
                StrUtil.blankToDefault(reqVO.getContact(), ""));
        return true;
    }

    private Map<String, Object> getProduct(CreateWapPayReqVO reqVO, String productType) {
        try {
            if (Objects.equals(PRODUCT_TYPE_OFFLINE_MEMBERSHIP, productType)) {
                return getOfflineMembershipProduct(reqVO);
            }
            return getOnlinePackageProduct(reqVO);
        } catch (EmptyResultDataAccessException ignored) {
            // Fall through to a business error below.
        }
        throw ServiceExceptionUtil.invalidParamException("Product is not available");
    }

    private Map<String, Object> getOnlinePackageProduct(CreateWapPayReqVO reqVO) {
        if (reqVO.getProductId() != null) {
            return jdbcTemplate.queryForMap("""
                    SELECT id, sku_code, name, text_chars, image_translate_count, ocr_translate_count,
                           asr_seconds, tts_chars, trial_once + 0 AS trial_once, price_cent, currency
                    FROM app_token_product
                    WHERE id = ? AND status = 1 AND deleted = 0
                    LIMIT 1
                    """, reqVO.getProductId());
        }
        if (StrUtil.isNotBlank(reqVO.getSkuCode())) {
            return jdbcTemplate.queryForMap("""
                    SELECT id, sku_code, name, text_chars, image_translate_count, ocr_translate_count,
                           asr_seconds, tts_chars, trial_once + 0 AS trial_once, price_cent, currency
                    FROM app_token_product
                    WHERE sku_code = ? AND status = 1 AND deleted = 0
                    LIMIT 1
                    """, reqVO.getSkuCode());
        }
        throw new EmptyResultDataAccessException(1);
    }

    private Map<String, Object> getOfflineMembershipProduct(CreateWapPayReqVO reqVO) {
        if (reqVO.getProductId() != null) {
            return jdbcTemplate.queryForMap("""
                    SELECT id, sku_code, name, duration_days, price_cent, currency, description, tag
                    FROM app_offline_membership_product
                    WHERE id = ? AND status = 1 AND deleted = 0
                    LIMIT 1
                    """, reqVO.getProductId());
        }
        if (StrUtil.isNotBlank(reqVO.getSkuCode())) {
            return jdbcTemplate.queryForMap("""
                    SELECT id, sku_code, name, duration_days, price_cent, currency, description, tag
                    FROM app_offline_membership_product
                    WHERE sku_code = ? AND status = 1 AND deleted = 0
                    LIMIT 1
                    """, reqVO.getSkuCode());
        }
        throw new EmptyResultDataAccessException(1);
    }

    private String requestAlipayWapUrl(String orderNo, String productName, String body, Integer amountCent,
                                       LocalDateTime expireTime, String returnUrl, String userIp) {
        try {
            AlipayConfig alipayConfig = buildAlipayConfig();

            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(orderNo);
            model.setSubject(StrUtil.maxLength(productName, 32));
            model.setBody(body);
            model.setTotalAmount(formatAmount(amountCent));
            model.setProductCode("QUICK_WAP_PAY");
            String resolvedReturnUrl = resolveReturnUrl(returnUrl);
            model.setQuitUrl(resolvedReturnUrl);
            model.setTimeExpire(DatePattern.NORM_DATETIME_FORMATTER.format(expireTime));

            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setBizModel(model);
            request.setNotifyUrl(integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_NOTIFY_URL));
            request.setReturnUrl(resolvedReturnUrl);

            AlipayTradeWapPayResponse response = new DefaultAlipayClient(alipayConfig).pageExecute(request, "GET");
            if (!response.isSuccess()) {
                throw ServiceExceptionUtil.invalidParamException("创建支付宝订单失败：{}", response.getSubMsg());
            }
            return response.getBody();
        } catch (Exception ex) {
            if (ex instanceof cn.iocoder.yudao.framework.common.exception.ServiceException) {
                throw (cn.iocoder.yudao.framework.common.exception.ServiceException) ex;
            }
            log.warn("[requestAlipayWapUrl][failed][orderNo={}, userIp={}]", orderNo, userIp, ex);
            throw ServiceExceptionUtil.invalidParamException("创建支付宝订单失败，请稍后重试");
        }
    }

    private void syncAlipayTradeStatus(Long userId, String orderNo) {
        Map<String, Object> order;
        try {
            order = jdbcTemplate.queryForMap("""
                    SELECT id, order_no, user_id, product_type, product_id, status, amount_cent
                    FROM app_payment_order
                    WHERE order_no = ? AND user_id = ? AND deleted = 0
                    LIMIT 1
                    FOR UPDATE
                    """, orderNo, userId);
        } catch (EmptyResultDataAccessException ex) {
            return;
        }
        String currentStatus = String.valueOf(order.get("status"));
        if (Objects.equals(STATUS_PAID, currentStatus) || Objects.equals(STATUS_CLOSED, currentStatus)
                || Objects.equals(STATUS_FAILED, currentStatus)) {
            return;
        }
        int amountCent = ((Number) order.get("amount_cent")).intValue();
        if (amountCent <= 0 || !integrationConfigService.isAlipayRuntimeConfigured()) {
            return;
        }
        try {
            AlipayTradeQueryResponse response = queryAlipayTrade(orderNo);
            if (response == null || !response.isSuccess()) {
                return;
            }
            String tradeStatus = StrUtil.blankToDefault(response.getTradeStatus(), "");
            String tradeNo = StrUtil.blankToDefault(response.getTradeNo(), "");
            if (!isAmountMatched(amountCent, response.getTotalAmount())) {
                log.warn("[syncAlipayTradeStatus][amount mismatch][orderNo={}]", orderNo);
                return;
            }
            if (Objects.equals("TRADE_SUCCESS", tradeStatus) || Objects.equals("TRADE_FINISHED", tradeStatus)) {
                grantPaidOrderBenefit(order, tradeNo, tradeStatus);
                return;
            }
            if (Objects.equals("TRADE_CLOSED", tradeStatus)) {
                jdbcTemplate.update("""
                        UPDATE app_payment_order
                        SET status = ?, provider_trade_no = ?, alipay_trade_status = ?, closed_at = NOW(),
                            updater = 'alipay-query', update_time = NOW()
                        WHERE id = ? AND status <> ?
                        """, STATUS_CLOSED, tradeNo, tradeStatus, order.get("id"), STATUS_PAID);
                return;
            }
            if (Objects.equals("WAIT_BUYER_PAY", tradeStatus)) {
                jdbcTemplate.update("""
                        UPDATE app_payment_order
                        SET provider_trade_no = ?, alipay_trade_status = ?, updater = 'alipay-query', update_time = NOW()
                        WHERE id = ?
                        """, tradeNo, tradeStatus, order.get("id"));
            }
        } catch (Exception ex) {
            log.warn("[syncAlipayTradeStatus][failed][orderNo={}]", orderNo, ex);
        }
    }

    private AlipayTradeQueryResponse queryAlipayTrade(String orderNo) throws Exception {
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);
        return new DefaultAlipayClient(buildAlipayConfig()).execute(request);
    }

    private AlipayConfig buildAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(StrUtil.blankToDefault(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_GATEWAY),
                "https://openapi.alipay.com/gateway.do"));
        alipayConfig.setAppId(integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_APP_ID));
        alipayConfig.setPrivateKey(integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_PRIVATE_KEY));
        alipayConfig.setAlipayPublicKey(integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_PUBLIC_KEY));
        alipayConfig.setCharset(StandardCharsets.UTF_8.name());
        alipayConfig.setFormat("json");
        alipayConfig.setSignType(StrUtil.blankToDefault(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_SIGN_TYPE), "RSA2"));
        return alipayConfig;
    }

    private boolean verifyAlipayNotify(Map<String, String> params) {
        try {
            String appId = params.get("app_id");
            if (!Objects.equals(appId, integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_APP_ID))) {
                log.warn("[verifyAlipayNotify][app id mismatch]");
                return false;
            }
            return AlipaySignature.rsaCheckV1(params,
                    integrationConfigService.requirePlain(AppIntegrationConfigService.ALIPAY_PUBLIC_KEY),
                    StandardCharsets.UTF_8.name(),
                    StrUtil.blankToDefault(integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_SIGN_TYPE), "RSA2"));
        } catch (Exception ex) {
            log.warn("[verifyAlipayNotify][failed]", ex);
            return false;
        }
    }

    private boolean isSellerMatched(String sellerId) {
        String configuredSellerId = integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_SELLER_ID);
        return StrUtil.isBlank(configuredSellerId) || Objects.equals(configuredSellerId, sellerId);
    }

    private boolean isAmountMatched(Integer amountCent, String totalAmount) {
        if (StrUtil.isBlank(totalAmount)) {
            return false;
        }
        try {
            return new BigDecimal(totalAmount).compareTo(BigDecimal.valueOf(amountCent, 2)) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void grantPaidOrderBenefit(Map<String, Object> order, String tradeNo, String tradeStatus) {
        String productType = String.valueOf(order.get("product_type"));
        if (Objects.equals(PRODUCT_TYPE_OFFLINE_MEMBERSHIP, productType)
                || Objects.equals(LEGACY_PRODUCT_TYPE_OFFLINE_VIP, productType)) {
            grantOfflineMembership(order, tradeNo, tradeStatus);
            return;
        }
        grantOnlinePackage(order, tradeNo, tradeStatus);
    }

    private void grantOnlinePackage(Map<String, Object> order, String tradeNo, String tradeStatus) {
        Long orderId = ((Number) order.get("id")).longValue();
        Long userId = ((Number) order.get("user_id")).longValue();
        Long productId = ((Number) order.get("product_id")).longValue();
        String orderNo = String.valueOf(order.get("order_no"));
        Map<String, Object> product = jdbcTemplate.queryForMap("""
                SELECT name, text_chars, image_translate_count, ocr_translate_count,
                       asr_seconds, tts_chars, trial_once + 0 AS trial_once
                FROM app_token_product
                WHERE id = ? AND deleted = 0
                LIMIT 1
                """, productId);
        Map<String, Object> account = getOrCreateTokenAccount(userId);
        if (((Number) product.get("trial_once")).intValue() == 1
                && ((Number) account.get("trial_claimed")).intValue() == 1) {
            throw ServiceExceptionUtil.invalidParamException("免费体验包每个账号只能领取一次");
        }
        Long accountId = ((Number) account.get("id")).longValue();
        int textAfter = ((Number) account.get("text_chars_remaining")).intValue()
                + ((Number) product.get("text_chars")).intValue();
        int imageAfter = ((Number) account.get("image_translate_remaining")).intValue()
                + ((Number) product.get("image_translate_count")).intValue();
        int ocrAfter = ((Number) account.get("ocr_translate_remaining")).intValue()
                + ((Number) product.get("ocr_translate_count")).intValue();
        int asrAfter = ((Number) account.get("asr_seconds_remaining")).intValue()
                + ((Number) product.get("asr_seconds")).intValue();
        int ttsAfter = ((Number) account.get("tts_chars_remaining")).intValue()
                + ((Number) product.get("tts_chars")).intValue();
        jdbcTemplate.update("""
                UPDATE app_token_account
                SET balance_tokens = ?, text_chars_remaining = ?, image_translate_remaining = ?,
                    ocr_translate_remaining = ?, asr_seconds_remaining = ?, tts_chars_remaining = ?,
                    current_package_name = ?, trial_claimed = GREATEST(trial_claimed, ?),
                    updater = 'alipay', update_time = NOW()
                WHERE id = ?
                """, textAfter, textAfter, imageAfter, ocrAfter, asrAfter, ttsAfter,
                String.valueOf(product.get("name")),
                ((Number) product.get("trial_once")).intValue(),
                accountId);
        jdbcTemplate.update("""
                INSERT INTO app_online_quota_ledger(account_id, user_id, direction,
                                                     text_chars_delta, image_translate_delta,
                                                     ocr_translate_delta, asr_seconds_delta, tts_chars_delta,
                                                     text_chars_after, image_translate_after,
                                                     ocr_translate_after, asr_seconds_after, tts_chars_after,
                                                     business_type, business_id, remark, creator, updater)
                VALUES (?, ?, 'income', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'payment', ?, '在线套餐开通', 'alipay', 'alipay')
                """, accountId, userId,
                ((Number) product.get("text_chars")).intValue(),
                ((Number) product.get("image_translate_count")).intValue(),
                ((Number) product.get("ocr_translate_count")).intValue(),
                ((Number) product.get("asr_seconds")).intValue(),
                ((Number) product.get("tts_chars")).intValue(),
                textAfter, imageAfter, ocrAfter, asrAfter, ttsAfter, orderNo);
        markOrderPaid(orderId, tradeNo, tradeStatus);
    }

    private void grantOfflineMembership(Map<String, Object> order, String tradeNo, String tradeStatus) {
        Long orderId = ((Number) order.get("id")).longValue();
        Long userId = ((Number) order.get("user_id")).longValue();
        Long productId = ((Number) order.get("product_id")).longValue();
        String orderNo = String.valueOf(order.get("order_no"));
        Map<String, Object> product = jdbcTemplate.queryForMap("""
                SELECT sku_code, duration_days
                FROM app_offline_membership_product
                WHERE id = ? AND deleted = 0
                LIMIT 1
                """, productId);
        String planCode = String.valueOf(product.get("sku_code"));
        Integer durationDays = ((Number) product.get("duration_days")).intValue();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> membership = findActiveMembershipForUpdate(userId);
        if (durationDays == 0) {
            upsertMembership(userId, membership, planCode, now, null, orderNo);
            markOrderPaid(orderId, tradeNo, tradeStatus);
            return;
        }

        if (membership != null && membership.get("expired_at") == null) {
            jdbcTemplate.update("""
                    UPDATE app_offline_membership
                    SET source_order_no = ?, updater = 'alipay', update_time = NOW()
                    WHERE id = ?
                    """, orderNo, membership.get("id"));
            markOrderPaid(orderId, tradeNo, tradeStatus);
            return;
        }

        LocalDateTime base = now;
        if (membership != null) {
            LocalDateTime currentExpire = toLocalDateTime(membership.get("expired_at"));
            if (currentExpire != null && currentExpire.isAfter(now)) {
                base = currentExpire;
            }
        }
        LocalDateTime expiredAt = base.plusDays(durationDays);
        upsertMembership(userId, membership, planCode, now, expiredAt, orderNo);
        markOrderPaid(orderId, tradeNo, tradeStatus);
    }

    private Map<String, Object> findActiveMembershipForUpdate(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, plan_code, expired_at
                FROM app_offline_membership
                WHERE user_id = ? AND status = 'active' AND deleted = 0
                  AND (expired_at IS NULL OR expired_at > NOW())
                ORDER BY expired_at IS NULL DESC, expired_at DESC
                LIMIT 1
                FOR UPDATE
                """, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void upsertMembership(Long userId, Map<String, Object> membership, String planCode,
                                  LocalDateTime startedAt, LocalDateTime expiredAt, String orderNo) {
        if (membership == null) {
            jdbcTemplate.update("""
                    INSERT INTO app_offline_membership(user_id, plan_code, status, started_at, expired_at,
                                                       source_order_no, creator, updater)
                    VALUES (?, ?, 'active', ?, ?, ?, 'alipay', 'alipay')
                    """, userId, planCode, startedAt, expiredAt, orderNo);
            return;
        }
        jdbcTemplate.update("""
                UPDATE app_offline_membership
                SET plan_code = ?, status = 'active', expired_at = ?, source_order_no = ?,
                    updater = 'alipay', update_time = NOW()
                WHERE id = ?
                """, planCode, expiredAt, orderNo, membership.get("id"));
    }

    private void markOrderPaid(Long orderId, String tradeNo, String tradeStatus) {
        jdbcTemplate.update("""
                UPDATE app_payment_order
                SET status = ?, provider_trade_no = ?, alipay_trade_status = ?, paid_at = NOW(),
                    updater = 'alipay', update_time = NOW()
                WHERE id = ?
                """, STATUS_PAID, tradeNo, tradeStatus, orderId);
    }

    private Map<String, Object> getOrCreateTokenAccount(Long userId) {
        try {
            return jdbcTemplate.queryForMap("""
                    SELECT id, balance_tokens, text_chars_remaining, image_translate_remaining,
                           ocr_translate_remaining, asr_seconds_remaining, tts_chars_remaining,
                           trial_claimed + 0 AS trial_claimed
                    FROM app_token_account
                    WHERE user_id = ? AND deleted = 0
                    LIMIT 1
                    FOR UPDATE
                    """, userId);
        } catch (EmptyResultDataAccessException ex) {
            jdbcTemplate.update("""
                    INSERT INTO app_token_account(user_id, balance_tokens, trial_tokens, frozen_tokens,
                                                  text_chars_remaining, image_translate_remaining,
                                                  ocr_translate_remaining, asr_seconds_remaining,
                                                  tts_chars_remaining, trial_claimed, current_package_name,
                                                  creator, updater)
                    VALUES (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 'app', 'app')
                    """, userId);
            return jdbcTemplate.queryForMap("""
                    SELECT id, balance_tokens, text_chars_remaining, image_translate_remaining,
                           ocr_translate_remaining, asr_seconds_remaining, tts_chars_remaining,
                           trial_claimed + 0 AS trial_claimed
                    FROM app_token_account
                    WHERE user_id = ? AND deleted = 0
                    LIMIT 1
                    FOR UPDATE
                    """, userId);
        }
    }

    private Map<String, Object> findOrderForUpdate(String orderNo) {
        try {
            return jdbcTemplate.queryForMap("""
                    SELECT id, order_no, user_id, product_type, product_id, status, amount_cent
                    FROM app_payment_order
                    WHERE order_no = ? AND deleted = 0
                    LIMIT 1
                    FOR UPDATE
                    """, orderNo);
        } catch (EmptyResultDataAccessException ex) {
            throw ServiceExceptionUtil.invalidParamException("订单不存在");
        }
    }

    private void closeExpiredOrder(String orderNo) {
        jdbcTemplate.update("""
                UPDATE app_payment_order
                SET status = ?, closed_at = NOW(), fail_reason = 'payment expired',
                    updater = 'system', update_time = NOW()
                WHERE order_no = ? AND status IN ('created', 'paying') AND expire_time < NOW() AND deleted = 0
                """, STATUS_CLOSED, orderNo);
    }

    private String resolveReturnUrl(String returnUrl) {
        String resolvedReturnUrl = StrUtil.blankToDefault(returnUrl,
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIPAY_RETURN_URL));
        if (StrUtil.isBlank(resolvedReturnUrl)) {
            throw ServiceExceptionUtil.invalidParamException("支付回跳地址暂未配置，请稍后再试");
        }
        return resolvedReturnUrl;
    }

    private void validateAlipayRuntimeConfigured() {
        if (!integrationConfigService.isAlipayRuntimeConfigured()) {
            throw ServiceExceptionUtil.invalidParamException("支付服务暂未开通，请稍后再试");
        }
    }

    private String normalizeProductType(String productType) {
        String normalized = StrUtil.blankToDefault(productType, PRODUCT_TYPE_ONLINE_PACKAGE);
        if (Objects.equals(PRODUCT_TYPE_ONLINE_PACKAGE, normalized) || Objects.equals(LEGACY_PRODUCT_TYPE_TOKEN, normalized)) {
            return PRODUCT_TYPE_ONLINE_PACKAGE;
        }
        if (Objects.equals(PRODUCT_TYPE_OFFLINE_MEMBERSHIP, normalized) || Objects.equals(LEGACY_PRODUCT_TYPE_OFFLINE_VIP, normalized)) {
            return PRODUCT_TYPE_OFFLINE_MEMBERSHIP;
        }
        throw ServiceExceptionUtil.invalidParamException("Unsupported product type");
    }

    private String normalizeStoredProductType(String productType) {
        if (Objects.equals(productType, LEGACY_PRODUCT_TYPE_TOKEN)) {
            return PRODUCT_TYPE_ONLINE_PACKAGE;
        }
        if (Objects.equals(productType, LEGACY_PRODUCT_TYPE_OFFLINE_VIP)) {
            return PRODUCT_TYPE_OFFLINE_MEMBERSHIP;
        }
        return StrUtil.blankToDefault(productType, PRODUCT_TYPE_ONLINE_PACKAGE);
    }

    private String orderBody(String productType) {
        if (Objects.equals(PRODUCT_TYPE_OFFLINE_MEMBERSHIP, productType)) {
            return "Offline membership";
        }
        return "Online package";
    }

    private String formatAmount(Integer amountCent) {
        return BigDecimal.valueOf(amountCent, 2).setScale(2).toPlainString();
    }

    private String generateOrderNo() {
        return "AP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    @Data
    public static class TokenProductRespVO {
        private Long id;
        private String skuCode;
        private String name;
        private Integer textChars;
        private Integer imageTranslateCount;
        private Integer ocrTranslateCount;
        private Integer asrSeconds;
        private Integer ttsChars;
        private Boolean trialOnce;
        private Integer priceCent;
        private String currency;
    }

    @Data
    public static class OfflineMembershipProductRespVO {
        private Long id;
        private String skuCode;
        private String name;
        private Integer durationDays;
        private Integer priceCent;
        private String currency;
        private String description;
        private String tag;
    }

    @Data
    public static class CreateWapPayReqVO {
        private Long productId;
        private String productType;
        private String skuCode;
        private String clientType;
        private String returnUrl;
    }

    @Data
    public static class CreateWapPayRespVO {
        private String orderNo;
        private String payUrl;
        private String displayMode;
        private String status;
        private String productType;
        private String productName;
        private Integer amountCent;
        private LocalDateTime expireTime;
    }

    @Data
    public static class OrderStatusRespVO {
        private String orderNo;
        private String productType;
        private String status;
        private Integer amountCent;
        private String providerTradeNo;
        private String productName;
        private LocalDateTime paidAt;
        private LocalDateTime expireTime;
    }

    @Data
    public static class UserEntitlementRespVO {
        private Integer balanceTokens;
        private Integer trialTokens;
        private Integer frozenTokens;
        private Integer textCharsRemaining;
        private Integer imageTranslateRemaining;
        private Integer ocrTranslateRemaining;
        private Integer asrSecondsRemaining;
        private Integer ttsCharsRemaining;
        private Boolean trialClaimed;
        private String currentPackageName;
        private boolean hasOfflineMembership;
        private String offlinePlanCode;
        private String offlineMembershipStatus;
        private LocalDateTime offlineExpiredAt;
    }

    @Data
    public static class UsageRecordRespVO {
        private String recordType;
        private String mode;
        private String scene;
        private String productType;
        private String productName;
        private String status;
        private Integer amountCent;
        private Integer textCharsUsed;
        private Integer imageTranslateCountUsed;
        private Integer ocrTranslateCountUsed;
        private Integer asrSecondsUsed;
        private Integer ttsCharsUsed;
        private LocalDateTime createTime;
    }

    @Data
    public static class UsageConsumeReqVO {
        private String clientRequestId;
        private String deviceUuid;
        private String mode;
        private String scene;
        private String sourceLanguageCode;
        private String targetLanguageCode;
        private String sourceText;
        private String translatedText;
        private int textChars;
        private int imageTranslateCount;
        private int ocrTranslateCount;
        private int asrSeconds;
        private int ttsChars;
    }

    @Data
    public static class FeedbackSubmitReqVO {
        private String deviceUuid;
        private String category;
        private String content;
        private String contact;
    }
}
