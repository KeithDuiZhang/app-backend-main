package cn.iocoder.yudao.module.love.controller.app.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 婚恋用户摘要 Response VO")
@Data
public class AppLoveUserRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "微信 openid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String openid;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "认证状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer authStatus;

    @Schema(description = "会员等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer memberLevel;

    @Schema(description = "会员到期时间")
    private java.time.LocalDateTime memberExpireTime;

    @Schema(description = "免费牵线次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer freeMatchQuota;

    @Schema(description = "资料是否完成基础 Tab1", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean profileCompleted;

    @Schema(description = "资料完成度", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    private Integer completionRate;
}
