package cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoveMemberPackageBaseReqVO {

    @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @Schema(description = "会员等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会员等级不能为空")
    private Integer level;

    @Schema(description = "支付金额(分)", requiredMode = Schema.RequiredMode.REQUIRED, example = "99800")
    @NotNull(message = "支付金额不能为空")
    private Integer priceFen;

    @Schema(description = "有效月数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "有效月数不能为空")
    private Integer durationMonths;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "权益 JSON 数组")
    private String featuresJson;

    @Schema(description = "展示主题", example = "silver")
    private String theme;

    @Schema(description = "是否推荐", example = "false")
    private Boolean popular;

    @Schema(description = "排序值", example = "10")
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
