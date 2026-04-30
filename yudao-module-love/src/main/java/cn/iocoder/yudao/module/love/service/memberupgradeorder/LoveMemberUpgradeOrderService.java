package cn.iocoder.yudao.module.love.service.memberupgradeorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder.vo.LoveMemberUpgradeOrderPageReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberUpgradeOrderDO;

public interface LoveMemberUpgradeOrderService {

    PageResult<LoveMemberUpgradeOrderDO> getUpgradeOrderPage(LoveMemberUpgradeOrderPageReqVO reqVO);
}
