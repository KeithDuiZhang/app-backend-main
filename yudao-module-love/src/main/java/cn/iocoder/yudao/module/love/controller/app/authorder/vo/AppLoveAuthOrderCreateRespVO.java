package cn.iocoder.yudao.module.love.controller.app.authorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 认证订单创建 Response VO")
@Data
public class AppLoveAuthOrderCreateRespVO {

    @Schema(description = "业务认证订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long bizOrderId;

    @Schema(description = "支付订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long payOrderId;

    @Schema(description = "支付金额(分)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1990")
    private Integer amount;

    @Schema(description = "认证订单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;
}
