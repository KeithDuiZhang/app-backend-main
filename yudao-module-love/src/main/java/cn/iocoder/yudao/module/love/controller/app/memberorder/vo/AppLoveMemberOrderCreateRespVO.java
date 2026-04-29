package cn.iocoder.yudao.module.love.controller.app.memberorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 会员订单创建 Response VO")
@Data
public class AppLoveMemberOrderCreateRespVO {

    private Long bizOrderId;

    private Long payOrderId;

    private Long packageId;

    private String packageName;

    private Integer priceFen;

    private Integer status;
}
