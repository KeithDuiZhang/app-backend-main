package cn.iocoder.yudao.module.love.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class LoveAuthServiceImpl implements LoveAuthService {

    private static final List<String> LOGIN_SCOPES = List.of("love.read", "love.write");

    @Resource
    private SocialUserService socialUserService;
    @Resource
    private OAuth2TokenCommonApi oAuth2TokenCommonApi;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveAuthLoginRespDTO loginByWechatMiniCode(String code) {
        SocialUserRespDTO socialUser = socialUserService.getSocialUserByCode(
                UserTypeEnum.MEMBER.getValue(), SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), code, "");
        LoveUserDO user = upsertLoveUser(socialUser);
        LoveUserProfileDO profile = getOrCreateProfile(user.getId());

        OAuth2AccessTokenCreateReqDTO tokenCreateReqDTO = new OAuth2AccessTokenCreateReqDTO();
        tokenCreateReqDTO.setUserId(user.getId());
        tokenCreateReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        tokenCreateReqDTO.setClientId(LoveBizConstants.APP_KEY);
        tokenCreateReqDTO.setScopes(LOGIN_SCOPES);
        OAuth2AccessTokenRespDTO tokenRespDTO = oAuth2TokenCommonApi.createAccessToken(tokenCreateReqDTO);

        return new LoveAuthLoginRespDTO()
                .setAccessToken(tokenRespDTO.getAccessToken())
                .setRefreshToken(tokenRespDTO.getRefreshToken())
                .setUserId(user.getId())
                .setOpenid(user.getOpenid())
                .setProfileCompleted(isProfileCompleted(profile))
                .setAuthStatus(user.getAuthStatus());
    }

    private LoveUserDO upsertLoveUser(SocialUserRespDTO socialUser) {
        LoveUserDO user = loveUserMapper.selectOne(LoveUserDO::getOpenid, socialUser.getOpenid());
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = LoveUserDO.builder()
                    .openid(socialUser.getOpenid())
                    .unionid(null)
                    .nickname(socialUser.getNickname())
                    .avatar(socialUser.getAvatar())
                    .status(CommonStatusEnum.ENABLE.getStatus())
                    .authStatus(0)
                    .memberLevel(0)
                    .freeMatchQuota(1)
                    .lastLoginTime(now)
                    .build();
            loveUserMapper.insert(user);
            return user;
        }

        user.setNickname(socialUser.getNickname());
        user.setAvatar(socialUser.getAvatar());
        user.setLastLoginTime(now);
        loveUserMapper.updateById(user);
        return user;
    }

    private LoveUserProfileDO getOrCreateProfile(Long userId) {
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, userId);
        if (profile != null) {
            return profile;
        }
        profile = LoveUserProfileDO.builder()
                .userId(userId)
                .profilePublic(Boolean.TRUE)
                .completionRate(0)
                .build();
        loveUserProfileMapper.insert(profile);
        return profile;
    }

    private boolean isProfileCompleted(LoveUserProfileDO profile) {
        return profile != null
                && profile.getGender() != null
                && profile.getBirthday() != null
                && StrUtil.isNotBlank(profile.getCityCode())
                && StrUtil.isNotBlank(profile.getCityName())
                && profile.getMaritalStatus() != null;
    }

}
