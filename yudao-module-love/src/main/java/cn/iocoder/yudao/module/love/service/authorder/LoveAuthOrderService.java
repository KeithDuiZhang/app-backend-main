package cn.iocoder.yudao.module.love.service.authorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.authorder.vo.LoveAuthOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.auth.LoveAuthOrderDO;

public interface LoveAuthOrderService {

    AppLoveAuthOrderCreateRespVO createAuthOrder(Long userId, String userIp);

    AppLoveAuthOrderRespVO getCurrentOrder(Long userId);

    PageResult<LoveAuthOrderDO> getAuthOrderPage(LoveAuthOrderPageReqVO reqVO);

    LoveAuthOrderDO getAuthOrder(Long id);
}
