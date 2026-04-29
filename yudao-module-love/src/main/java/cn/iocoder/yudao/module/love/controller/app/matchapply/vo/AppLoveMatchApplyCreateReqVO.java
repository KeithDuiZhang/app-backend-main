package cn.iocoder.yudao.module.love.controller.app.matchapply.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - 牵线申请创建 Request VO")
@Data
public class AppLoveMatchApplyCreateReqVO {

    @Schema(description = "目标用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;

    @Schema(description = "申请原因")
    private String applyReason;
}
