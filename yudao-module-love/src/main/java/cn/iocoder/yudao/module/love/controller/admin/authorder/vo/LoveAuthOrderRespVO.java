package cn.iocoder.yudao.module.love.controller.admin.authorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 认证订单 Response VO")
@Data
public class LoveAuthOrderRespVO {

    private Long id;
    private Long userId;
    private Long payOrderId;
    private String orderNo;
    private Integer amount;
    private Integer status;
    private String verifiedResult;
    private LocalDateTime payTime;
    private LocalDateTime verifiedTime;
    private LocalDateTime createTime;
}
