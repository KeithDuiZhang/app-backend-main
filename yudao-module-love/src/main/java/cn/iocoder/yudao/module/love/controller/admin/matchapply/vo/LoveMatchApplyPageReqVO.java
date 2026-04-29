package cn.iocoder.yudao.module.love.controller.admin.matchapply.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 牵线申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMatchApplyPageReqVO extends PageParam {

    private Long fromUserId;
    private Long toUserId;
    private Long matchmakerId;
    private Integer status;
}
