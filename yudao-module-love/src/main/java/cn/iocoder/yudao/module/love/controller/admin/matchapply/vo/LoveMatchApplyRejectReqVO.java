package cn.iocoder.yudao.module.love.controller.admin.matchapply.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoveMatchApplyRejectReqVO {

    @NotNull(message = "申请编号不能为空")
    private Long id;

    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;
}
