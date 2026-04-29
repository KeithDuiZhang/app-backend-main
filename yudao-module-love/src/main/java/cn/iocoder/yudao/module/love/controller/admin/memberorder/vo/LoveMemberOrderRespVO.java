package cn.iocoder.yudao.module.love.controller.admin.memberorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员订单 Response VO")
@Data
public class LoveMemberOrderRespVO {

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

    private LocalDateTime createTime;
}
