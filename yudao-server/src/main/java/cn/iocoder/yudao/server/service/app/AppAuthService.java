package cn.iocoder.yudao.server.service.app;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AppAuthService {

    private static final int ACCESS_TOKEN_DAYS = 7;
    private static final int REFRESH_TOKEN_DAYS = 30;
    private static final String DEBUG_SMS_CHANNEL_CODE = "DEBUG_DING_TALK";

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private SmsCodeApi smsCodeApi;
    @Resource
    private Environment environment;

    public void sendSmsCode(SmsCodeSendReqDTO reqDTO, String ip) {
        validateProductionSmsChannel(reqDTO.getScene());
        reqDTO.setCreateIp(ip);
        smsCodeApi.sendSmsCode(reqDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginRespVO register(RegisterReqVO reqVO, String ip) {
        useSmsCode(reqVO.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_LOGIN.getScene(), ip);
        Long existingUserId = findUserIdByMobile(reqVO.getMobile());
        if (existingUserId != null) {
            throw ServiceExceptionUtil.invalidParamException("手机号已注册");
        }
        String appUserNo = "APP" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 8);
        jdbcTemplate.update("""
                INSERT INTO app_user(app_user_no, nickname, phone_cipher, auth_status, status, source_channel, last_login_at, creator, updater)
                VALUES (?, ?, ?, 10, 1, ?, NOW(), 'app', 'app')
                """, appUserNo, StrUtil.blankToDefault(reqVO.getNickname(), reqVO.getMobile()), reqVO.getMobile(),
                StrUtil.blankToDefault(reqVO.getClientType(), "web"));
        Long userId = findUserIdByMobile(reqVO.getMobile());
        jdbcTemplate.update("""
                INSERT INTO app_user_identity(user_id, identity_type, provider_open_id, credential_hash, enabled, creator, updater)
                VALUES (?, 'password', ?, ?, 1, 'app', 'app')
                """, userId, reqVO.getMobile(), passwordEncoder.encode(reqVO.getPassword()));
        ensureTokenAccount(userId);
        return createSession(userId, reqVO.getClientType());
    }

    public LoginRespVO passwordLogin(PasswordLoginReqVO reqVO) {
        Long userId = findUserIdByMobile(reqVO.getMobile());
        if (userId == null) {
            throw ServiceExceptionUtil.invalidParamException("手机号未注册");
        }
        String hash = jdbcTemplate.queryForObject("""
                SELECT credential_hash FROM app_user_identity
                WHERE identity_type = 'password' AND provider_open_id = ? AND enabled = 1 AND deleted = 0
                ORDER BY id DESC LIMIT 1
                """, String.class, reqVO.getMobile());
        if (!passwordEncoder.matches(reqVO.getPassword(), hash)) {
            throw ServiceExceptionUtil.invalidParamException("手机号或密码不正确");
        }
        jdbcTemplate.update("UPDATE app_user SET last_login_at = NOW(), update_time = NOW() WHERE id = ?", userId);
        return createSession(userId, reqVO.getClientType());
    }

    public LoginRespVO smsLogin(SmsLoginReqVO reqVO, String ip) {
        useSmsCode(reqVO.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_LOGIN.getScene(), ip);
        Long userId = findUserIdByMobile(reqVO.getMobile());
        if (userId == null) {
            throw ServiceExceptionUtil.invalidParamException("手机号未注册");
        }
        jdbcTemplate.update("UPDATE app_user SET last_login_at = NOW(), update_time = NOW() WHERE id = ?", userId);
        return createSession(userId, reqVO.getClientType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordReqVO reqVO, String ip) {
        useSmsCode(reqVO.getMobile(), reqVO.getCode(), SmsSceneEnum.MEMBER_RESET_PASSWORD.getScene(), ip);
        Long userId = findUserIdByMobile(reqVO.getMobile());
        if (userId == null) {
            throw ServiceExceptionUtil.invalidParamException("手机号未注册");
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_user_identity
                WHERE identity_type = 'password' AND provider_open_id = ? AND deleted = 0
                """, Integer.class, reqVO.getMobile());
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE app_user_identity SET credential_hash = ?, updater = 'app', update_time = NOW()
                    WHERE identity_type = 'password' AND provider_open_id = ? AND deleted = 0
                    """, passwordEncoder.encode(reqVO.getPassword()), reqVO.getMobile());
        } else {
            jdbcTemplate.update("""
                    INSERT INTO app_user_identity(user_id, identity_type, provider_open_id, credential_hash, enabled, creator, updater)
                    VALUES (?, 'password', ?, ?, 1, 'app', 'app')
                    """, userId, reqVO.getMobile(), passwordEncoder.encode(reqVO.getPassword()));
        }
    }

    public Long requireUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            token = request.getHeader("X-App-Token");
        }
        if (StrUtil.isNotBlank(token) && StrUtil.startWithIgnoreCase(token, "Bearer ")) {
            token = token.substring("Bearer ".length());
        }
        if (StrUtil.isBlank(token)) {
            throw ServiceExceptionUtil.invalidParamException("缺少登录凭证");
        }
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT user_id FROM app_user_session
                    WHERE access_token = ? AND expires_at > NOW() AND deleted = 0
                    LIMIT 1
                    """, token);
            Long userId = ((Number) row.get("user_id")).longValue();
            jdbcTemplate.update("UPDATE app_user_session SET last_seen_at = NOW(), update_time = NOW() WHERE access_token = ?", token);
            return userId;
        } catch (EmptyResultDataAccessException ex) {
            throw ServiceExceptionUtil.invalidParamException("登录已过期，请重新登录");
        }
    }

    private void useSmsCode(String mobile, String code, Integer scene, String ip) {
        SmsCodeUseReqDTO reqDTO = new SmsCodeUseReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(scene);
        reqDTO.setUsedIp(ip);
        smsCodeApi.useSmsCode(reqDTO);
    }

    private void validateProductionSmsChannel(Integer scene) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        SmsSceneEnum sceneEnum = SmsSceneEnum.getCodeByScene(scene);
        if (sceneEnum == null) {
            return;
        }

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT t.channel_code, c.signature, c.api_key, c.api_secret, c.remark
                    FROM system_sms_template t
                    LEFT JOIN system_sms_channel c ON c.id = t.channel_id AND c.deleted = 0
                    WHERE t.code = ? AND t.deleted = 0
                    LIMIT 1
                    """, sceneEnum.getTemplateCode());
            String channelCode = valueToString(row.get("channel_code"));
            if (StrUtil.equalsIgnoreCase(DEBUG_SMS_CHANNEL_CODE, channelCode)
                    || isPlaceholderSmsValue(row.get("api_key"))
                    || isPlaceholderSmsValue(row.get("api_secret"))
                    || isDemoSmsChannel(row)) {
                throw ServiceExceptionUtil.invalidParamException("短信服务暂未开通，请稍后再试");
            }
        } catch (EmptyResultDataAccessException ex) {
            throw ServiceExceptionUtil.invalidParamException("短信服务暂未开通，请稍后再试");
        }
    }

    private boolean isDemoSmsChannel(Map<String, Object> row) {
        String signature = valueToString(row.get("signature"));
        String remark = valueToString(row.get("remark"));
        return StrUtil.equalsIgnoreCase("Ballcat", signature)
                || StrUtil.containsIgnoreCase(remark, "你要改")
                || StrUtil.containsIgnoreCase(remark, "mock");
    }

    private boolean isPlaceholderSmsValue(Object raw) {
        String value = valueToString(raw);
        return StrUtil.isBlank(value)
                || StrUtil.equalsIgnoreCase("null", value)
                || StrUtil.equalsIgnoreCase("test", value)
                || StrUtil.equalsIgnoreCase("please_change_me", value)
                || StrUtil.equalsIgnoreCase("123", value)
                || StrUtil.equalsIgnoreCase("1 2", value)
                || StrUtil.equalsIgnoreCase("2 3", value)
                || StrUtil.containsIgnoreCase(value, "mock");
    }

    private String valueToString(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private Long findUserIdByMobile(String mobile) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id FROM app_user
                    WHERE phone_cipher = ? AND deleted = 0
                    ORDER BY id DESC LIMIT 1
                    """, Long.class, mobile);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void ensureTokenAccount(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_token_account WHERE user_id = ? AND deleted = 0", Integer.class, userId);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO app_token_account(user_id, balance_tokens, trial_tokens, frozen_tokens,
                                                  text_chars_remaining, image_translate_remaining,
                                                  ocr_translate_remaining, asr_seconds_remaining,
                                                  tts_chars_remaining, trial_claimed, current_package_name,
                                                  creator, updater)
                    VALUES (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 'app', 'app')
                    """, userId);
        }
    }

    private LoginRespVO createSession(Long userId, String clientType) {
        String accessToken = IdUtil.fastSimpleUUID();
        String refreshToken = IdUtil.fastSimpleUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(ACCESS_TOKEN_DAYS);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS);
        jdbcTemplate.update("""
                INSERT INTO app_user_session(user_id, access_token, refresh_token, expires_at, refresh_expires_at,
                                             client_type, last_seen_at, creator, updater)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), 'app', 'app')
                """, userId, accessToken, refreshToken, expiresAt, refreshExpiresAt,
                StrUtil.blankToDefault(clientType, "web"));
        return new LoginRespVO()
                .setUserId(userId)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresTime(expiresAt);
    }

    @Data
    public static class RegisterReqVO {
        private String mobile;
        private String code;
        private String password;
        private String nickname;
        private String clientType;
    }

    @Data
    public static class PasswordLoginReqVO {
        private String mobile;
        private String password;
        private String clientType;
    }

    @Data
    public static class SmsLoginReqVO {
        private String mobile;
        private String code;
        private String clientType;
    }

    @Data
    public static class ResetPasswordReqVO {
        private String mobile;
        private String code;
        private String password;
    }

    @Data
    public static class LoginRespVO {
        private Long userId;
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresTime;

        public LoginRespVO setUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public LoginRespVO setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public LoginRespVO setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public LoginRespVO setExpiresTime(LocalDateTime expiresTime) {
            this.expiresTime = expiresTime;
            return this;
        }
    }
}
