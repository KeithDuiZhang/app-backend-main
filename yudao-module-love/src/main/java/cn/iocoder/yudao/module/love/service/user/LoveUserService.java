package cn.iocoder.yudao.module.love.service.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.user.vo.LoveUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.user.vo.AppLoveUserRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;

public interface LoveUserService {

    AppLoveUserRespVO getMyUser(Long userId);

    PageResult<LoveUserDO> getUserPage(LoveUserPageReqVO reqVO);

    LoveUserDO getUser(Long id);
}
