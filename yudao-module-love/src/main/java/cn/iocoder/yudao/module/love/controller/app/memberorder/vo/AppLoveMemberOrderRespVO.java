package cn.iocoder.yudao.module.love.controller.app.memberorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 会员订单 Response VO")
@Data
public class AppLoveMemberOrderRespVO {

    private Long id;

    private Long userId;

    private Long packageId;

    private Long payOrderId;

    private String orderNo;

    private String packageName;

    private Integer memberLevel;

    private Integer priceFen;

    private Integer durationMonths;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime memberStartTime;

    private LocalDateTime memberEndTime;
}
