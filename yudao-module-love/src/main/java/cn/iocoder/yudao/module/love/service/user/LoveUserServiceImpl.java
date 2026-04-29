package cn.iocoder.yudao.module.love.service.user;

import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.user.vo.LoveUserPageReqVO;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.love.controller.app.user.vo.AppLoveUserRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class LoveUserServiceImpl implements LoveUserService {

    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;

    @Override
    public AppLoveUserRespVO getMyUser(Long userId) {
        LoveUserDO user = loveUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        normalizeMemberStatus(user);
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, userId);
        AppLoveUserRespVO respVO = new AppLoveUserRespVO();
        respVO.setId(user.getId());
        respVO.setOpenid(user.getOpenid());
        respVO.setNickname(user.getNickname());
        respVO.setAvatar(user.getAvatar());
        respVO.setMobile(user.getMobile());
        respVO.setAuthStatus(user.getAuthStatus());
        respVO.setMemberLevel(user.getMemberLevel());
        respVO.setMemberExpireTime(user.getMemberExpireTime());
        respVO.setFreeMatchQuota(user.getFreeMatchQuota());
        respVO.setCompletionRate(profile != null ? profile.getCompletionRate() : 0);
        respVO.setProfileCompleted(isBasicProfileCompleted(profile));
        return respVO;
    }

    @Override
    public PageResult<LoveUserDO> getUserPage(LoveUserPageReqVO reqVO) {
        return loveUserMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveUserDO>()
                .likeIfPresent(LoveUserDO::getNickname, reqVO.getNickname())
                .likeIfPresent(LoveUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(LoveUserDO::getAuthStatus, reqVO.getAuthStatus())
                .orderByDesc(LoveUserDO::getId));
    }

    @Override
    public LoveUserDO getUser(Long id) {
        return loveUserMapper.selectById(id);
    }

    private boolean isBasicProfileCompleted(LoveUserProfileDO profile) {
        return profile != null
                && profile.getGender() != null
                && profile.getBirthday() != null
                && StrUtil.isNotBlank(profile.getCityCode())
                && StrUtil.isNotBlank(profile.getCityName())
                && profile.getMaritalStatus() != null;
    }

    private void normalizeMemberStatus(LoveUserDO user) {
        if (user.getMemberLevel() == null || user.getMemberLevel() <= LoveBizConstants.MEMBER_LEVEL_NONE) {
            return;
        }
        if (user.getMemberExpireTime() == null || !user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
            user.setMemberLevel(LoveBizConstants.MEMBER_LEVEL_NONE);
            user.setMemberExpireTime(null);
            loveUserMapper.updateById(user);
        }
    }
}
