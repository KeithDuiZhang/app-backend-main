package cn.iocoder.yudao.module.love.service.profile;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileRespVO;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileUpdateBasicReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class LoveUserProfileServiceImpl implements LoveUserProfileService {

    private static final int BASIC_COMPLETION_RATE = 40;

    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;

    @Override
    public AppLoveUserProfileRespVO getProfile(Long userId) {
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, userId);
        if (profile == null) {
            return null;
        }
        AppLoveUserProfileRespVO respVO = new AppLoveUserProfileRespVO();
        respVO.setUserId(profile.getUserId());
        respVO.setRealName(profile.getRealName());
        respVO.setGender(profile.getGender());
        respVO.setBirthday(profile.getBirthday());
        respVO.setCityCode(profile.getCityCode());
        respVO.setCityName(profile.getCityName());
        respVO.setMaritalStatus(profile.getMaritalStatus());
        respVO.setHeightCm(profile.getHeightCm());
        respVO.setWeightKg(profile.getWeightKg());
        respVO.setProfession(profile.getProfession());
        respVO.setEducation(profile.getEducation());
        respVO.setIncomeDesc(profile.getIncomeDesc());
        respVO.setPhotos(profile.getPhotos());
        respVO.setBio(profile.getBio());
        respVO.setTags(profile.getTags());
        respVO.setPartnerPreference(profile.getPartnerPreference());
        respVO.setProfilePublic(profile.getProfilePublic());
        respVO.setCompletionRate(profile.getCompletionRate());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicProfile(Long userId, AppLoveUserProfileUpdateBasicReqVO reqVO) {
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, userId);
        if (profile == null) {
            profile = LoveUserProfileDO.builder()
                    .userId(userId)
                    .profilePublic(Boolean.TRUE)
                    .completionRate(0)
                    .build();
            loveUserProfileMapper.insert(profile);
        }
        profile.setRealName(reqVO.getRealName());
        profile.setGender(reqVO.getGender());
        profile.setBirthday(reqVO.getBirthday());
        profile.setCityCode(reqVO.getCityCode());
        profile.setCityName(reqVO.getCityName());
        profile.setMaritalStatus(reqVO.getMaritalStatus());
        profile.setCompletionRate(isBasicProfileCompleted(profile) ? BASIC_COMPLETION_RATE : 0);
        loveUserProfileMapper.updateById(profile);
    }

    private boolean isBasicProfileCompleted(LoveUserProfileDO profile) {
        return profile != null
                && profile.getGender() != null
                && profile.getBirthday() != null
                && StrUtil.isNotBlank(profile.getCityCode())
                && StrUtil.isNotBlank(profile.getCityName())
                && profile.getMaritalStatus() != null;
    }
}
