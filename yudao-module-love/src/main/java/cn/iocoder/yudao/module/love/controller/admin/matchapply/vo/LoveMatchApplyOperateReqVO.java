package cn.iocoder.yudao.module.love.controller.admin.matchapply.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoveMatchApplyOperateReqVO {

    @NotNull(message = "申请编号不能为空")
    private Long id;

    private String remark;
}
