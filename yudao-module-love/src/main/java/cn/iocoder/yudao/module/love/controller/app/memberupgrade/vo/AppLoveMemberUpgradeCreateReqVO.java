package cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 会员升级订单创建 Request VO")
@Data
public class AppLoveMemberUpgradeCreateReqVO {

    @Schema(description = "目标会员 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标会员 SKU 编号不能为空")
    private Long targetSkuId;
}
