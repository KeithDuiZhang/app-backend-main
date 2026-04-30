package cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 会员升级订单结果 Response VO")
@Data
public class AppLoveMemberUpgradeResultRespVO {

    private Long id;

    private String fromGroupCode;

    private String toGroupCode;

    private String toGroupName;

    private Integer upgradeAmountFen;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;
}
