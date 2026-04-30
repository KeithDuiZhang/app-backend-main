package cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 会员升级订单创建 Response VO")
@Data
public class AppLoveMemberUpgradeCreateRespVO {

    private Long bizOrderId;

    private Long payOrderId;

    private Integer payAmountFen;

    private String targetGroupCode;
}
