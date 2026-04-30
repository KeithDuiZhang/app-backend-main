package cn.iocoder.yudao.module.love.service.membercenter;

import cn.iocoder.yudao.module.love.controller.app.membercenter.vo.AppLoveMemberCenterOverviewRespVO;

public interface LoveMemberCenterService {

    AppLoveMemberCenterOverviewRespVO getOverview(Long userId, String selectedGroupCode, String selectedDurationCode);
}
