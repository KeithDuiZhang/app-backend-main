package cn.iocoder.yudao.module.love.controller.app.matchapply.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户 App - 我的牵线申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppLoveMatchApplyPageReqVO extends PageParam {

    @Schema(description = "申请状态", example = "10")
    private Integer status;
}
