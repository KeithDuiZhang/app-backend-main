package cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员套餐更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberPackageUpdateReqVO extends LoveMemberPackageBaseReqVO {

    @Schema(description = "套餐编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "套餐编号不能为空")
    private Long id;
}
