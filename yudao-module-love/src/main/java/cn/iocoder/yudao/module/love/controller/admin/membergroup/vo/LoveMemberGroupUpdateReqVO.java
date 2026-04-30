package cn.iocoder.yudao.module.love.controller.admin.membergroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员分组更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberGroupUpdateReqVO extends LoveMemberGroupBaseReqVO {

    @Schema(description = "分组编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组编号不能为空")
    private Long id;
}
