package cn.iocoder.yudao.module.love.controller.app.matchapply.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 牵线申请 Response VO")
@Data
public class AppLoveMatchApplyRespVO {

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private Long matchmakerId;

    private Integer status;

    private String rejectReason;

    private String applyReason;

    private LocalDateTime submittedAt;

    private LocalDateTime processedAt;
}
