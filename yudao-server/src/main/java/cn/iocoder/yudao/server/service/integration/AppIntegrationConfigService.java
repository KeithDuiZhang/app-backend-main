package cn.iocoder.yudao.server.service.integration;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import lombok.Data;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppIntegrationConfigService {

    private static final String ENC_PREFIX = "ENC:";

    public static final String ALIPAY_APP_ID = "alipay.app-id";
    public static final String ALIPAY_PRIVATE_KEY = "alipay.private-key";
    public static final String ALIPAY_PUBLIC_KEY = "alipay.public-key";
    public static final String ALIPAY_GATEWAY = "alipay.gateway";
    public static final String ALIPAY_NOTIFY_URL = "alipay.notify-url";
    public static final String ALIPAY_RETURN_URL = "alipay.return-url";
    public static final String ALIPAY_SIGN_TYPE = "alipay.sign-type";
    public static final String ALIPAY_SELLER_ID = "alipay.seller-id";

    public static final String SMS_SECRET_ID = "tencent.sms.secret-id";
    public static final String SMS_SECRET_KEY = "tencent.sms.secret-key";
    public static final String SMS_SDK_APP_ID = "tencent.sms.sdk-app-id";
    public static final String SMS_SIGN_NAME = "tencent.sms.sign-name";
    public static final String SMS_TEMPLATE_LOGIN = "tencent.sms.template.login";
    public static final String SMS_TEMPLATE_UPDATE_MOBILE = "tencent.sms.template.update-mobile";
    public static final String SMS_TEMPLATE_UPDATE_PASSWORD = "tencent.sms.template.update-password";
    public static final String SMS_TEMPLATE_RESET_PASSWORD = "tencent.sms.template.reset-password";
    public static final String SMS_DAILY_SEND_LIMIT = "yudao.sms-code.send-maximum-quantity-per-day";

    public static final String COS_SECRET_ID = "tencent.cos.secret-id";
    public static final String COS_SECRET_KEY = "tencent.cos.secret-key";
    public static final String COS_BUCKET = "tencent.cos.bucket";
    public static final String COS_REGION = "tencent.cos.region";
    public static final String COS_ENDPOINT = "tencent.cos.endpoint";
    public static final String COS_DOMAIN = "tencent.cos.domain";
    public static final String COS_PREFIX = "tencent.cos.prefix";
    public static final String COS_SIGNED_URL_TTL_SECONDS = "tencent.cos.signed-url-ttl-seconds";

    public static final String ALIYUN_ACCESS_KEY_ID = "aliyun.access-key-id";
    public static final String ALIYUN_ACCESS_KEY_SECRET = "aliyun.access-key-secret";
    public static final String ALIYUN_TRANSLATION_REGION = "aliyun.translation.region";
    public static final String ALIYUN_OCR_REGION = "aliyun.ocr.region";
    public static final String ALIYUN_SPEECH_REGION = "aliyun.speech.region";
    public static final String ALIYUN_SPEECH_APP_KEY = "aliyun.speech.app-key";
    public static final String ALIYUN_TTS_APP_KEY = "aliyun.tts.app-key";
    public static final String ALIYUN_IMAGE_TRANSLATE_SCENE = "aliyun.image-translate.scene";
    public static final String ALIYUN_ENDPOINT = "aliyun.endpoint";
    public static final String ALIYUN_DASHSCOPE_API_KEY = "aliyun.dashscope.api-key";

    private static final String DEFAULT_COS_BUCKET = "kqtranslate-1300276385";
    private static final String DEFAULT_COS_REGION = "ap-shanghai";
    private static final String DEFAULT_COS_ENDPOINT = "https://cos.ap-shanghai.myqcloud.com";
    private static final String DEFAULT_COS_DOMAIN = "https://kqtranslate-1300276385.cos.ap-shanghai.myqcloud.com";
    private static final String DEFAULT_COS_PREFIX = "model-repo/cn/1.0.0/";
    private static final String DEFAULT_COS_SIGNED_URL_TTL_SECONDS = "3600";
    private static final String DEFAULT_ALIYUN_TRANSLATION_REGION = "cn-hangzhou";
    private static final String DEFAULT_ALIYUN_OCR_REGION = "cn-shanghai";
    private static final String DEFAULT_ALIYUN_SPEECH_REGION = "cn-shanghai";
    private static final String DEFAULT_ALIYUN_IMAGE_TRANSLATE_SCENE = "general";
    private static final int DEFAULT_SMS_DAILY_SEND_LIMIT = 30;

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private CacheManager cacheManager;

    public IntegrationConfigRespVO getEditableConfig() {
        IntegrationConfigRespVO respVO = new IntegrationConfigRespVO();
        respVO.getAlipay().setAppId(getPlain(ALIPAY_APP_ID));
        respVO.getAlipay().setGateway(getPlain(ALIPAY_GATEWAY));
        respVO.getAlipay().setNotifyUrl(getPlain(ALIPAY_NOTIFY_URL));
        respVO.getAlipay().setReturnUrl(getPlain(ALIPAY_RETURN_URL));
        respVO.getAlipay().setSignType(StrUtil.blankToDefault(getPlain(ALIPAY_SIGN_TYPE), "RSA2"));
        respVO.getAlipay().setSellerId(getPlain(ALIPAY_SELLER_ID));
        respVO.getAlipay().setPrivateKey(getPlain(ALIPAY_PRIVATE_KEY));
        respVO.getAlipay().setPublicKey(getPlain(ALIPAY_PUBLIC_KEY));
        respVO.getAlipay().setPrivateKeyConfigured(StrUtil.isNotBlank(getPlain(ALIPAY_PRIVATE_KEY)));
        respVO.getAlipay().setPublicKeyConfigured(StrUtil.isNotBlank(getPlain(ALIPAY_PUBLIC_KEY)));

        respVO.getSms().setSecretId(getPlain(SMS_SECRET_ID));
        respVO.getSms().setSecretKey(getPlain(SMS_SECRET_KEY));
        respVO.getSms().setSecretKeyConfigured(StrUtil.isNotBlank(getPlain(SMS_SECRET_KEY)));
        respVO.getSms().setSmsSdkAppId(getPlain(SMS_SDK_APP_ID));
        respVO.getSms().setSignName(getPlain(SMS_SIGN_NAME));
        respVO.getSms().setLoginTemplateId(getPlain(SMS_TEMPLATE_LOGIN));
        respVO.getSms().setUpdateMobileTemplateId(getPlain(SMS_TEMPLATE_UPDATE_MOBILE));
        respVO.getSms().setUpdatePasswordTemplateId(getPlain(SMS_TEMPLATE_UPDATE_PASSWORD));
        respVO.getSms().setResetPasswordTemplateId(getPlain(SMS_TEMPLATE_RESET_PASSWORD));
        respVO.getSms().setDailySendLimit(getInfraConfigInt(SMS_DAILY_SEND_LIMIT, DEFAULT_SMS_DAILY_SEND_LIMIT));

        respVO.getCos().setSecretId(getPlain(COS_SECRET_ID));
        respVO.getCos().setSecretKey(getPlain(COS_SECRET_KEY));
        respVO.getCos().setSecretKeyConfigured(StrUtil.isNotBlank(getPlain(COS_SECRET_KEY)));
        respVO.getCos().setBucket(StrUtil.blankToDefault(getPlain(COS_BUCKET), DEFAULT_COS_BUCKET));
        respVO.getCos().setRegion(StrUtil.blankToDefault(getPlain(COS_REGION), DEFAULT_COS_REGION));
        respVO.getCos().setEndpoint(StrUtil.blankToDefault(getPlain(COS_ENDPOINT), DEFAULT_COS_ENDPOINT));
        respVO.getCos().setDomain(StrUtil.blankToDefault(getPlain(COS_DOMAIN), DEFAULT_COS_DOMAIN));
        respVO.getCos().setPrefix(StrUtil.blankToDefault(getPlain(COS_PREFIX), DEFAULT_COS_PREFIX));
        respVO.getCos().setSignedUrlTtlSeconds(StrUtil.blankToDefault(
                getPlain(COS_SIGNED_URL_TTL_SECONDS), DEFAULT_COS_SIGNED_URL_TTL_SECONDS));
        respVO.getAliyun().setAccessKeyId(getPlain(ALIYUN_ACCESS_KEY_ID));
        respVO.getAliyun().setAccessKeySecret(getPlain(ALIYUN_ACCESS_KEY_SECRET));
        respVO.getAliyun().setTranslationRegion(StrUtil.blankToDefault(
                getPlain(ALIYUN_TRANSLATION_REGION), DEFAULT_ALIYUN_TRANSLATION_REGION));
        respVO.getAliyun().setOcrRegion(StrUtil.blankToDefault(
                getPlain(ALIYUN_OCR_REGION), DEFAULT_ALIYUN_OCR_REGION));
        respVO.getAliyun().setSpeechRegion(StrUtil.blankToDefault(
                getPlain(ALIYUN_SPEECH_REGION), DEFAULT_ALIYUN_SPEECH_REGION));
        respVO.getAliyun().setSpeechAppKey(getPlain(ALIYUN_SPEECH_APP_KEY));
        respVO.getAliyun().setTtsAppKey(getPlain(ALIYUN_TTS_APP_KEY));
        respVO.getAliyun().setImageTranslateScene(StrUtil.blankToDefault(
                getPlain(ALIYUN_IMAGE_TRANSLATE_SCENE), DEFAULT_ALIYUN_IMAGE_TRANSLATE_SCENE));
        respVO.getAliyun().setEndpoint(getPlain(ALIYUN_ENDPOINT));
        respVO.getAliyun().setDashscopeApiKey(getPlain(ALIYUN_DASHSCOPE_API_KEY));
        respVO.getAliyun().setAccessKeySecretConfigured(StrUtil.isNotBlank(getPlain(ALIYUN_ACCESS_KEY_SECRET)));
        respVO.getAliyun().setDashscopeApiKeyConfigured(StrUtil.isNotBlank(getPlain(ALIYUN_DASHSCOPE_API_KEY)));
        return respVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(IntegrationConfigSaveReqVO reqVO) {
        AlipayConfig alipayConfig = reqVO.getAlipay() == null ? new AlipayConfig() : reqVO.getAlipay();
        SmsConfig smsConfig = reqVO.getSms() == null ? new SmsConfig() : reqVO.getSms();
        CosConfig cosConfig = reqVO.getCos() == null ? new CosConfig() : reqVO.getCos();
        AliyunConfig aliyunConfig = reqVO.getAliyun() == null ? new AliyunConfig() : reqVO.getAliyun();

        upsertPlain(ALIPAY_APP_ID, alipayConfig.getAppId(), "Alipay app id", false);
        upsertPlain(ALIPAY_GATEWAY, alipayConfig.getGateway(), "Alipay gateway", false);
        upsertPlain(ALIPAY_NOTIFY_URL, alipayConfig.getNotifyUrl(), "Alipay notify url", false);
        upsertPlain(ALIPAY_RETURN_URL, alipayConfig.getReturnUrl(), "Alipay return url", false);
        upsertPlain(ALIPAY_SIGN_TYPE, StrUtil.blankToDefault(alipayConfig.getSignType(), "RSA2"), "Alipay sign type", false);
        upsertPlain(ALIPAY_SELLER_ID, alipayConfig.getSellerId(), "Alipay seller id", false);
        upsertSecret(ALIPAY_PRIVATE_KEY, alipayConfig.getPrivateKey(), "Alipay private key");
        upsertSecret(ALIPAY_PUBLIC_KEY, alipayConfig.getPublicKey(), "Alipay public key");

        upsertPlain(SMS_SECRET_ID, smsConfig.getSecretId(), "Tencent SMS secret id", false);
        upsertPlain(SMS_SECRET_KEY, smsConfig.getSecretKey(), "Tencent SMS secret key", false);
        upsertPlain(SMS_SDK_APP_ID, smsConfig.getSmsSdkAppId(), "Tencent SMS SDK app id", false);
        upsertPlain(SMS_SIGN_NAME, smsConfig.getSignName(), "Tencent SMS sign name", false);
        upsertPlain(SMS_TEMPLATE_LOGIN, smsConfig.getLoginTemplateId(), "Tencent SMS login template", false);
        upsertPlain(SMS_TEMPLATE_UPDATE_MOBILE, smsConfig.getUpdateMobileTemplateId(), "Tencent SMS update mobile template", false);
        upsertPlain(SMS_TEMPLATE_UPDATE_PASSWORD, smsConfig.getUpdatePasswordTemplateId(), "Tencent SMS update password template", false);
        upsertPlain(SMS_TEMPLATE_RESET_PASSWORD, smsConfig.getResetPasswordTemplateId(), "Tencent SMS reset password template", false);
        upsertInfraConfig(SMS_DAILY_SEND_LIMIT,
                String.valueOf(normalizePositiveInt(smsConfig.getDailySendLimit(), DEFAULT_SMS_DAILY_SEND_LIMIT)),
                "短信验证码每日发送次数", "sms",
                "单个手机号每天可发送验证码次数，默认 30 次");

        upsertSecret(COS_SECRET_ID, cosConfig.getSecretId(), "Tencent COS secret id");
        upsertSecret(COS_SECRET_KEY, cosConfig.getSecretKey(), "Tencent COS secret key");
        upsertPlain(COS_BUCKET, StrUtil.blankToDefault(cosConfig.getBucket(), DEFAULT_COS_BUCKET), "Tencent COS model bucket", false);
        upsertPlain(COS_REGION, StrUtil.blankToDefault(cosConfig.getRegion(), DEFAULT_COS_REGION), "Tencent COS model region", false);
        upsertPlain(COS_ENDPOINT, StrUtil.blankToDefault(cosConfig.getEndpoint(), DEFAULT_COS_ENDPOINT), "Tencent COS model endpoint", false);
        upsertPlain(COS_DOMAIN, StrUtil.blankToDefault(cosConfig.getDomain(), DEFAULT_COS_DOMAIN), "Tencent COS model public domain", false);
        upsertPlain(COS_PREFIX, StrUtil.blankToDefault(cosConfig.getPrefix(), DEFAULT_COS_PREFIX), "Tencent COS model object prefix", false);
        upsertPlain(COS_SIGNED_URL_TTL_SECONDS,
                StrUtil.blankToDefault(cosConfig.getSignedUrlTtlSeconds(), DEFAULT_COS_SIGNED_URL_TTL_SECONDS),
                "Tencent COS signed URL TTL seconds", false);
        upsertPlain(ALIYUN_ACCESS_KEY_ID, aliyunConfig.getAccessKeyId(), "Aliyun access key id", false);
        upsertSecret(ALIYUN_ACCESS_KEY_SECRET, aliyunConfig.getAccessKeySecret(), "Aliyun access key secret");
        upsertPlain(ALIYUN_TRANSLATION_REGION,
                StrUtil.blankToDefault(aliyunConfig.getTranslationRegion(), DEFAULT_ALIYUN_TRANSLATION_REGION),
                "Aliyun translation region", false);
        upsertPlain(ALIYUN_OCR_REGION,
                StrUtil.blankToDefault(aliyunConfig.getOcrRegion(), DEFAULT_ALIYUN_OCR_REGION),
                "Aliyun OCR region", false);
        upsertPlain(ALIYUN_SPEECH_REGION,
                StrUtil.blankToDefault(aliyunConfig.getSpeechRegion(), DEFAULT_ALIYUN_SPEECH_REGION),
                "Aliyun speech region", false);
        upsertPlain(ALIYUN_SPEECH_APP_KEY, aliyunConfig.getSpeechAppKey(), "Aliyun ASR speech app key", false);
        upsertPlain(ALIYUN_TTS_APP_KEY, aliyunConfig.getTtsAppKey(), "Aliyun TTS app key", false);
        upsertPlain(ALIYUN_IMAGE_TRANSLATE_SCENE,
                StrUtil.blankToDefault(aliyunConfig.getImageTranslateScene(), DEFAULT_ALIYUN_IMAGE_TRANSLATE_SCENE),
                "Aliyun image translate scene", false);
        upsertPlain(ALIYUN_ENDPOINT, aliyunConfig.getEndpoint(), "Aliyun API endpoint", false);
        upsertSecret(ALIYUN_DASHSCOPE_API_KEY, aliyunConfig.getDashscopeApiKey(), "Aliyun DashScope API key");
        syncTencentSmsChannel();
    }

    public String requirePlain(String key) {
        String value = getPlain(key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("Missing backend integration config: " + key);
        }
        return value;
    }

    public boolean isAlipayRuntimeConfigured() {
        return hasPlain(ALIPAY_APP_ID)
                && hasPlain(ALIPAY_PRIVATE_KEY)
                && hasPlain(ALIPAY_PUBLIC_KEY)
                && hasPlain(ALIPAY_NOTIFY_URL)
                && hasPlain(ALIPAY_RETURN_URL);
    }

    public boolean isCosRuntimeConfigured() {
        return hasPlain(COS_SECRET_ID) && hasPlain(COS_SECRET_KEY);
    }

    public boolean isTencentSmsRuntimeConfigured() {
        return hasPlain(SMS_SECRET_ID)
                && hasPlain(SMS_SECRET_KEY)
                && hasPlain(SMS_SDK_APP_ID)
                && hasPlain(SMS_SIGN_NAME)
                && hasPlain(SMS_TEMPLATE_LOGIN);
    }

    public CosConfig getCosRuntimeConfig() {
        CosConfig config = new CosConfig();
        config.setSecretId(requirePlain(COS_SECRET_ID));
        config.setSecretKey(requirePlain(COS_SECRET_KEY));
        config.setBucket(StrUtil.blankToDefault(getPlain(COS_BUCKET), DEFAULT_COS_BUCKET));
        config.setRegion(StrUtil.blankToDefault(getPlain(COS_REGION), DEFAULT_COS_REGION));
        config.setEndpoint(StrUtil.blankToDefault(getPlain(COS_ENDPOINT), DEFAULT_COS_ENDPOINT));
        config.setDomain(StrUtil.blankToDefault(getPlain(COS_DOMAIN), DEFAULT_COS_DOMAIN));
        config.setPrefix(StrUtil.blankToDefault(getPlain(COS_PREFIX), DEFAULT_COS_PREFIX));
        config.setSignedUrlTtlSeconds(StrUtil.blankToDefault(
                getPlain(COS_SIGNED_URL_TTL_SECONDS), DEFAULT_COS_SIGNED_URL_TTL_SECONDS));
        return config;
    }

    private boolean hasPlain(String key) {
        return StrUtil.isNotBlank(getPlain(key));
    }

    public String getPlain(String key) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT config_value, encrypted FROM app_integration_secret WHERE config_key = ? AND deleted = 0 LIMIT 1", key);
            String value = (String) row.get("config_value");
            Number encrypted = (Number) row.get("encrypted");
            if (StrUtil.isBlank(value)) {
                return "";
            }
            return encrypted != null && encrypted.intValue() == 1 ? decryptMarked(value) : value;
        } catch (EmptyResultDataAccessException ex) {
            return "";
        }
    }

    public String encryptMarked(String value) {
        return ENC_PREFIX + EncryptTypeHandler.encrypt(value);
    }

    public String decryptMarked(String value) {
        if (value == null || !value.startsWith(ENC_PREFIX)) {
            return value;
        }
        return EncryptTypeHandler.decrypt(value.substring(ENC_PREFIX.length()));
    }

    private void upsertSecret(String key, String value, String remark) {
        if (StrUtil.isBlank(value) || isMaskedSecret(value)) {
            return;
        }
        upsertRaw(key, value.trim(), 0, remark);
    }

    private void upsertPlain(String key, String value, String remark, boolean encrypted) {
        if (value == null || isMaskedSecret(value)) {
            return;
        }
        upsertRaw(key, encrypted ? encryptMarked(value.trim()) : value.trim(), encrypted ? 1 : 0, remark);
    }

    private boolean isMaskedSecret(String value) {
        return StrUtil.isNotBlank(value) && value.trim().contains("****");
    }

    private void upsertRaw(String key, String value, int encrypted, String remark) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_integration_secret WHERE config_key = ? AND deleted = 0", Integer.class, key);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE app_integration_secret
                    SET config_value = ?, encrypted = ?, remark = ?, updater = 'admin', update_time = NOW()
                    WHERE config_key = ? AND deleted = 0
                    """, value, encrypted, remark, key);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO app_integration_secret(config_key, config_value, encrypted, remark, creator, updater)
                    VALUES (?, ?, ?, ?, 'admin', 'admin')
                    """, key, value, encrypted, remark);
        }
    }

    private int getInfraConfigInt(String key, int defaultValue) {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT value FROM infra_config WHERE config_key = ? AND deleted = 0 LIMIT 1", String.class, key);
            return parsePositiveInt(value, defaultValue);
        } catch (EmptyResultDataAccessException ex) {
            return defaultValue;
        }
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private int normalizePositiveInt(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private void upsertInfraConfig(String key, String value, String name, String category, String remark) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM infra_config WHERE config_key = ? AND deleted = 0", Integer.class, key);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE infra_config
                    SET value = ?, name = ?, category = ?, visible = 1, remark = ?, updater = 'admin', update_time = NOW()
                    WHERE config_key = ? AND deleted = 0
                    """, value, name, category, remark, key);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO infra_config(category, type, name, config_key, value, visible, remark, creator, updater)
                    VALUES (?, 2, ?, ?, ?, 1, ?, 'admin', 'admin')
                    """, category, name, key, value, remark);
        }
    }

    private void syncTencentSmsChannel() {
        String secretId = getPlain(SMS_SECRET_ID);
        String secretKey = getPlain(SMS_SECRET_KEY);
        String sdkAppId = getPlain(SMS_SDK_APP_ID);
        String signName = getPlain(SMS_SIGN_NAME);
        if (StrUtil.hasBlank(secretId, secretKey, sdkAppId, signName)) {
            return;
        }
        Long channelId = queryLong("SELECT id FROM system_sms_channel WHERE code = 'TENCENT' AND deleted = 0 ORDER BY id LIMIT 1");
        String apiKey = secretId + " " + sdkAppId;
        String apiSecret = secretKey;
        if (channelId == null) {
            jdbcTemplate.update("""
                    INSERT INTO system_sms_channel(signature, code, status, remark, api_key, api_secret, callback_url, creator, updater)
                    VALUES (?, 'TENCENT', 0, 'Tencent SMS configured by integration menu', ?, ?, '', 'admin', 'admin')
                    """, signName, apiKey, apiSecret);
            channelId = queryLong("SELECT id FROM system_sms_channel WHERE code = 'TENCENT' AND deleted = 0 ORDER BY id DESC LIMIT 1");
        } else {
            jdbcTemplate.update("""
                    UPDATE system_sms_channel
                    SET signature = ?, status = 0, remark = 'Tencent SMS configured by integration menu',
                        api_key = ?, api_secret = ?, updater = 'admin', update_time = NOW()
                    WHERE id = ?
                    """, signName, apiKey, apiSecret, channelId);
        }
        if (channelId == null) {
            return;
        }
        Map<String, String> templates = new LinkedHashMap<>();
        putTemplate(templates, "user-sms-login", getPlain(SMS_TEMPLATE_LOGIN));
        putTemplate(templates, "user-update-mobile", getPlain(SMS_TEMPLATE_UPDATE_MOBILE));
        putTemplate(templates, "user-update-password", getPlain(SMS_TEMPLATE_UPDATE_PASSWORD));
        putTemplate(templates, "user-reset-password", getPlain(SMS_TEMPLATE_RESET_PASSWORD));
        putTemplate(templates, "admin-sms-login", getPlain(SMS_TEMPLATE_LOGIN));
        putTemplate(templates, "admin-reset-password", getPlain(SMS_TEMPLATE_RESET_PASSWORD));
        putTemplate(templates, "admin-sms-register", getPlain(SMS_TEMPLATE_LOGIN));
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            syncSmsTemplate(entry.getKey(), entry.getValue(), channelId);
        }
        clearSmsTemplateCache();
    }

    private void putTemplate(Map<String, String> templates, String code, String templateId) {
        if (StrUtil.isNotBlank(templateId)) {
            templates.put(code, templateId);
        }
    }

    private void syncSmsTemplate(String code, String templateId, Long channelId) {
        Long templateDbId = queryLong("SELECT id FROM system_sms_template WHERE code = ? AND deleted = 0 ORDER BY id LIMIT 1", code);
        if (templateDbId == null) {
            jdbcTemplate.update("""
                    INSERT INTO system_sms_template(type, status, code, name, content, params, remark, api_template_id,
                                                    channel_id, channel_code, creator, updater)
                    VALUES (1, 0, ?, ?, 'Your verification code is {code}. It is valid for 5 minutes.', '[\"code\"]',
                            'Configured by integration menu', ?, ?, 'TENCENT', 'admin', 'admin')
                    """, code, code, templateId, channelId);
        } else {
            jdbcTemplate.update("""
                    UPDATE system_sms_template
                    SET status = 0, api_template_id = ?, channel_id = ?, channel_code = 'TENCENT',
                        params = '[\"code\"]', updater = 'admin', update_time = NOW()
                    WHERE id = ?
                    """, templateId, channelId, templateDbId);
        }
    }

    private Long queryLong(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void clearSmsTemplateCache() {
        Cache cache = cacheManager.getCache(RedisKeyConstants.SMS_TEMPLATE);
        if (cache != null) {
            cache.clear();
        }
    }

    private String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    @Data
    public static class IntegrationConfigSaveReqVO {
        private AlipayConfig alipay = new AlipayConfig();
        private SmsConfig sms = new SmsConfig();
        private CosConfig cos = new CosConfig();
        private AliyunConfig aliyun = new AliyunConfig();
    }

    @Data
    public static class IntegrationConfigRespVO {
        private AlipayConfigStatus alipay = new AlipayConfigStatus();
        private SmsConfigStatus sms = new SmsConfigStatus();
        private CosConfigStatus cos = new CosConfigStatus();
        private AliyunConfigStatus aliyun = new AliyunConfigStatus();
    }

    @Data
    public static class AlipayConfig {
        private String appId;
        private String privateKey;
        private String publicKey;
        private String gateway;
        private String notifyUrl;
        private String returnUrl;
        private String signType;
        private String sellerId;
    }

    @Data
    public static class SmsConfig {
        private String secretId;
        private String secretKey;
        private String smsSdkAppId;
        private String signName;
        private String loginTemplateId;
        private String updateMobileTemplateId;
        private String updatePasswordTemplateId;
        private String resetPasswordTemplateId;
        private Integer dailySendLimit;
    }

    @Data
    public static class CosConfig {
        private String secretId;
        private String secretKey;
        private String bucket;
        private String region;
        private String endpoint;
        private String domain;
        private String prefix;
        private String signedUrlTtlSeconds;
    }

    @Data
    public static class AliyunConfig {
        private String accessKeyId;
        private String accessKeySecret;
        private String translationRegion;
        private String ocrRegion;
        private String speechRegion;
        private String speechAppKey;
        private String ttsAppKey;
        private String imageTranslateScene;
        private String endpoint;
        private String dashscopeApiKey;
    }

    @Data
    public static class AlipayConfigStatus {
        private String appId;
        private String privateKey;
        private String publicKey;
        private String gateway;
        private String notifyUrl;
        private String returnUrl;
        private String signType;
        private String sellerId;
        private boolean privateKeyConfigured;
        private boolean publicKeyConfigured;
    }

    @Data
    public static class SmsConfigStatus {
        private String secretId;
        private String secretKey;
        private boolean secretKeyConfigured;
        private String smsSdkAppId;
        private String signName;
        private String loginTemplateId;
        private String updateMobileTemplateId;
        private String updatePasswordTemplateId;
        private String resetPasswordTemplateId;
        private Integer dailySendLimit;
    }

    @Data
    public static class CosConfigStatus {
        private String secretId;
        private String secretKey;
        private boolean secretKeyConfigured;
        private String bucket;
        private String region;
        private String endpoint;
        private String domain;
        private String prefix;
        private String signedUrlTtlSeconds;
    }

    @Data
    public static class AliyunConfigStatus {
        private String accessKeyId;
        private String accessKeySecret;
        private boolean accessKeySecretConfigured;
        private String translationRegion;
        private String ocrRegion;
        private String speechRegion;
        private String speechAppKey;
        private String ttsAppKey;
        private String imageTranslateScene;
        private String endpoint;
        private String dashscopeApiKey;
        private boolean dashscopeApiKeyConfigured;
    }
}
