package cn.iocoder.yudao.module.love.service.memberupgrade;

import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradePreviewRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeResultRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;

import java.time.LocalDateTime;
import java.util.List;

public interface LoveMemberUpgradeService {

    Integer calculateUpgradePayAmountFen(Integer targetSalePriceFen,
                                         List<LoveMemberEntitlementSegmentDO> entitlementSegments,
                                         LocalDateTime calculateAt);

    AppLoveMemberUpgradePreviewRespVO preview(Long userId, Long targetSkuId);

    AppLoveMemberUpgradeCreateRespVO createUpgradeOrder(Long userId, Long targetSkuId, String userIp);

    AppLoveMemberUpgradeResultRespVO getUpgradeResult(Long userId, Long orderId);
}
