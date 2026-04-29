package cn.iocoder.yudao.module.love.controller.app.authorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 当前认证订单 Response VO")
@Data
public class AppLoveAuthOrderRespVO {

    @Schema(description = "业务认证订单编号", example = "1")
    private Long id;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "支付订单编号", example = "1001")
    private Long payOrderId;

    @Schema(description = "业务订单号", example = "AUTH202604290001")
    private String orderNo;

    @Schema(description = "金额(分)", example = "1990")
    private Integer amount;

    @Schema(description = "认证订单状态", example = "10")
    private Integer status;

    @Schema(description = "认证结果")
    private String verifiedResult;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "认证完成时间")
    private LocalDateTime verifiedTime;
}
