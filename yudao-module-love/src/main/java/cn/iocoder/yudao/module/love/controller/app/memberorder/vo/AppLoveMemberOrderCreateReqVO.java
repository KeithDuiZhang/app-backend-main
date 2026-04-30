package cn.iocoder.yudao.module.love.controller.app.memberorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 会员订单创建 Request VO")
@Data
public class AppLoveMemberOrderCreateReqVO {

    @Schema(description = "会员套餐编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long packageId;

    @Schema(description = "会员 SKU 编号")
    private Long skuId;

    public Long resolveSkuId() {
        return skuId != null ? skuId : packageId;
    }
}
