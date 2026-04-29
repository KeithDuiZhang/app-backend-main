package cn.iocoder.yudao.module.love.controller.admin.authorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 认证订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveAuthOrderPageReqVO extends PageParam {

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "业务订单号")
    private String orderNo;
}
