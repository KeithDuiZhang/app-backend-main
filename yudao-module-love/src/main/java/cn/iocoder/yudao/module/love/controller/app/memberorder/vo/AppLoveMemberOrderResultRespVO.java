package cn.iocoder.yudao.module.love.controller.app.memberorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 会员订单结果 Response VO")
@Data
public class AppLoveMemberOrderResultRespVO {

    private Long id;

    private Long skuId;

    private String groupCode;

    private String groupName;

    private String packageName;

    private Integer memberLevel;

    private Integer paidAmountFen;

    private LocalDateTime payTime;

    private LocalDateTime startTime;

    private LocalDateTime expireTime;
}
