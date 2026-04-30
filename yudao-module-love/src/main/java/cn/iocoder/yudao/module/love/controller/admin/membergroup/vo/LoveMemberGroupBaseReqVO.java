package cn.iocoder.yudao.module.love.controller.admin.membergroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoveMemberGroupBaseReqVO {

    @Schema(description = "分组编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组编码不能为空")
    private String code;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    private String name;

    @Schema(description = "会员等级", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会员等级不能为空")
    private Integer level;

    @Schema(description = "主题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "主题不能为空")
    private String theme;

    @Schema(description = "权益 JSON")
    private String benefitsJson;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
