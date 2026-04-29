package cn.iocoder.yudao.module.love.controller.admin.memberorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberOrderPageReqVO extends PageParam {

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "会员等级")
    private Integer memberLevel;

    @Schema(description = "业务订单号")
    private String orderNo;

    @Schema(description = "套餐名称")
    private String packageName;
}
