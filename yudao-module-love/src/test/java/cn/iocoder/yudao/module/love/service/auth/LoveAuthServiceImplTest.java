package cn.iocoder.yudao.module.love.service.auth;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import cn.iocoder.yudao.module.love.service.auth.dto.LoveAuthLoginRespDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.service.social.SocialUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(LoveAuthServiceImpl.class)
@TestPropertySource(properties = {
        "yudao.info.base-package=cn.iocoder.yudao.module.love",
        "spring.sql.init.mode=never"
})
@SqlMergeMode(SqlMergeMode.MergeMode.OVERRIDE)
@Sql(statements = {
        "DROP TABLE IF EXISTS love_user_profile",
        "DROP TABLE IF EXISTS love_user"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class LoveAuthServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LoveAuthServiceImpl loveAuthService;

    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;
    @Resource
    private DataSource dataSource;

    @MockitoBean
    private SocialUserService socialUserService;
    @MockitoBean
    private OAuth2TokenCommonApi oAuth2TokenCommonApi;

    @BeforeEach
    public void setUpSchema() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_user_profile");
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_user");
        jdbcTemplate.execute("""
                CREATE TABLE love_user (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  social_user_id BIGINT DEFAULT NULL,
                  openid VARCHAR(64) NOT NULL,
                  unionid VARCHAR(64) DEFAULT NULL,
                  nickname VARCHAR(64) DEFAULT NULL,
                  avatar VARCHAR(255) DEFAULT NULL,
                  mobile VARCHAR(20) DEFAULT NULL,
                  status TINYINT NOT NULL DEFAULT 0,
                  auth_status TINYINT NOT NULL DEFAULT 0,
                  certified_at TIMESTAMP DEFAULT NULL,
                  member_level TINYINT NOT NULL DEFAULT 0,
                  free_match_quota INT NOT NULL DEFAULT 1,
                  last_quota_reset_at TIMESTAMP DEFAULT NULL,
                  last_login_time TIMESTAMP DEFAULT NULL,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX uk_love_user_openid ON love_user(openid)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE love_user_profile (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  real_name VARCHAR(32) DEFAULT NULL,
                  gender TINYINT DEFAULT 0,
                  birthday DATE DEFAULT NULL,
                  city_code VARCHAR(32) DEFAULT NULL,
                  city_name VARCHAR(64) DEFAULT NULL,
                  marital_status TINYINT DEFAULT NULL,
                  height_cm SMALLINT DEFAULT NULL,
                  weight_kg SMALLINT DEFAULT NULL,
                  profession VARCHAR(64) DEFAULT NULL,
                  education VARCHAR(32) DEFAULT NULL,
                  income_desc VARCHAR(64) DEFAULT NULL,
                  photos CLOB DEFAULT NULL,
                  bio VARCHAR(500) DEFAULT NULL,
                  tags VARCHAR(255) DEFAULT NULL,
                  partner_preference VARCHAR(500) DEFAULT NULL,
                  profile_public BOOLEAN NOT NULL DEFAULT TRUE,
                  completion_rate TINYINT NOT NULL DEFAULT 0,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX uk_love_user_profile_user_id ON love_user_profile(user_id)
                """);
    }

    @Test
    public void testLoginByWechatMiniCode_createUserAndProfile() {
        String code = "mini-code-create";
        String openid = "openid-create";
        when(socialUserService.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq(code), eq("")))
                .thenReturn(new SocialUserRespDTO(openid, "Nick Create", "https://avatar/create.png", null));
        OAuth2AccessTokenRespDTO tokenRespDTO = buildTokenResp();
        when(oAuth2TokenCommonApi.createAccessToken(argThat(req -> hasTokenReq(req, null))))
                .thenReturn(tokenRespDTO);

        LoveAuthLoginRespDTO respDTO = loveAuthService.loginByWechatMiniCode(code);

        LoveUserDO user = loveUserMapper.selectOne(LoveUserDO::getOpenid, openid);
        assertNotNull(user);
        assertEquals("Nick Create", user.getNickname());
        assertEquals("https://avatar/create.png", user.getAvatar());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus());
        assertEquals(0, user.getAuthStatus());
        assertEquals(0, user.getMemberLevel());
        assertEquals(1, user.getFreeMatchQuota());
        assertNull(user.getUnionid());
        assertNotNull(user.getLastLoginTime());

        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, user.getId());
        assertNotNull(profile);
        assertTrue(profile.getProfilePublic());
        assertEquals(0, profile.getCompletionRate());

        assertEquals(tokenRespDTO.getAccessToken(), respDTO.getAccessToken());
        assertEquals(tokenRespDTO.getRefreshToken(), respDTO.getRefreshToken());
        assertEquals(user.getId(), respDTO.getUserId());
        assertEquals(openid, respDTO.getOpenid());
        assertFalse(respDTO.getProfileCompleted());
        assertEquals(user.getAuthStatus(), respDTO.getAuthStatus());

        verify(oAuth2TokenCommonApi).createAccessToken(argThat(req -> hasTokenReq(req, user.getId())));
    }

    @Test
    public void testLoginByWechatMiniCode_reuseExistingUserWithoutDuplicateInsert() {
        String code = "mini-code-reuse";
        String openid = "openid-reuse";
        LoveUserDO user = LoveUserDO.builder()
                .openid(openid)
                .nickname("Old Nick")
                .avatar("https://avatar/old.png")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .authStatus(2)
                .memberLevel(0)
                .freeMatchQuota(1)
                .lastLoginTime(LocalDateTime.now().minusDays(1))
                .build();
        loveUserMapper.insert(user);
        LoveUserProfileDO profile = LoveUserProfileDO.builder()
                .userId(user.getId())
                .gender(1)
                .birthday(LocalDate.of(1995, 6, 18))
                .cityCode("310000")
                .cityName("Shanghai")
                .maritalStatus(1)
                .profilePublic(Boolean.TRUE)
                .completionRate(60)
                .build();
        loveUserProfileMapper.insert(profile);

        when(socialUserService.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq(code), eq("")))
                .thenReturn(new SocialUserRespDTO(openid, "New Nick", "https://avatar/new.png", null));
        OAuth2AccessTokenRespDTO tokenRespDTO = buildTokenResp();
        when(oAuth2TokenCommonApi.createAccessToken(argThat(req -> hasTokenReq(req, user.getId()))))
                .thenReturn(tokenRespDTO);

        LoveAuthLoginRespDTO respDTO = loveAuthService.loginByWechatMiniCode(code);

        assertEquals(1L, loveUserMapper.selectCount(LoveUserDO::getOpenid, openid));
        assertEquals(1L, loveUserProfileMapper.selectCount(LoveUserProfileDO::getUserId, user.getId()));

        LoveUserDO refreshedUser = loveUserMapper.selectById(user.getId());
        assertEquals("New Nick", refreshedUser.getNickname());
        assertEquals("https://avatar/new.png", refreshedUser.getAvatar());
        assertTrue(refreshedUser.getLastLoginTime().isAfter(user.getLastLoginTime()));

        assertEquals(user.getId(), respDTO.getUserId());
        assertEquals(openid, respDTO.getOpenid());
        assertTrue(respDTO.getProfileCompleted());
        assertEquals(2, respDTO.getAuthStatus());

        verify(oAuth2TokenCommonApi).createAccessToken(argThat(req -> hasTokenReq(req, user.getId())));
    }

    private static OAuth2AccessTokenRespDTO buildTokenResp() {
        OAuth2AccessTokenRespDTO respDTO = new OAuth2AccessTokenRespDTO();
        respDTO.setAccessToken("access-token");
        respDTO.setRefreshToken("refresh-token");
        return respDTO;
    }

    private static boolean hasTokenReq(OAuth2AccessTokenCreateReqDTO reqDTO, Long expectedUserId) {
        if (reqDTO == null) {
            return false;
        }
        if (expectedUserId != null && !expectedUserId.equals(reqDTO.getUserId())) {
            return false;
        }
        return UserTypeEnum.MEMBER.getValue().equals(reqDTO.getUserType())
                && LoveBizConstants.APP_KEY.equals(reqDTO.getClientId())
                && List.of("love.read", "love.write").equals(reqDTO.getScopes());
    }

}
