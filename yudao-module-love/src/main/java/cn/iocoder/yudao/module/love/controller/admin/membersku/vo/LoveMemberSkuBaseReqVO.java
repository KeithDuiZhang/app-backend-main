package cn.iocoder.yudao.module.love.controller.admin.membersku.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoveMemberSkuBaseReqVO {

    @Schema(description = "会员分组编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员分组编号不能为空")
    private Long groupId;

    @Schema(description = "时长类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时长类型不能为空")
    private Integer durationType;

    @Schema(description = "时长天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时长天数不能为空")
    private Integer durationDays;

    @Schema(description = "销售价(分)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "销售价不能为空")
    private Integer salePriceFen;

    @Schema(description = "原价(分)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原价不能为空")
    private Integer originalPriceFen;

    @Schema(description = "标签文案")
    private String tagText;

    @Schema(description = "是否推荐", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "推荐标识不能为空")
    private Boolean recommend;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
