package cn.iocoder.yudao.module.love.service.memberorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.memberorder.vo.LoveMemberOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberOrderDO;

public interface LoveMemberOrderService {

    AppLoveMemberOrderCreateRespVO createMemberOrder(Long userId, Long packageId, String userIp);

    AppLoveMemberOrderRespVO getCurrentOrder(Long userId);

    PageResult<LoveMemberOrderDO> getMemberOrderPage(LoveMemberOrderPageReqVO reqVO);

    LoveMemberOrderDO getMemberOrder(Long id);
}
