package cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员升级订单 Response VO")
@Data
public class LoveMemberUpgradeOrderRespVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long fromGroupId;
    private Long toGroupId;
    private Long targetSkuId;
    private Integer remainingDays;
    private Integer fullDiffAmountFen;
    private Integer upgradeAmountFen;
    private Long payOrderId;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}
