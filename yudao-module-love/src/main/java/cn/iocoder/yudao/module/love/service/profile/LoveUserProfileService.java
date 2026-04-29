package cn.iocoder.yudao.module.love.service.profile;

import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileRespVO;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileUpdateBasicReqVO;

public interface LoveUserProfileService {

    AppLoveUserProfileRespVO getProfile(Long userId);

    void updateBasicProfile(Long userId, AppLoveUserProfileUpdateBasicReqVO reqVO);
}
