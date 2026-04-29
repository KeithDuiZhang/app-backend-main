package cn.iocoder.yudao.module.love.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 微信小程序登录 Response VO")
@Data
public class AppLoveWechatMiniLoginRespVO {

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "access-token")
    private String accessToken;

    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "refresh-token")
    private String refreshToken;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "微信 openid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String openid;

    @Schema(description = "资料是否完成 Tab1", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean profileCompleted;

    @Schema(description = "认证状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer authStatus;

}
