package cn.iocoder.yudao.module.love.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 App - 微信小程序登录 Request VO")
@Data
public class AppLoveWechatMiniLoginReqVO {

    @Schema(description = "微信小程序登录 code", requiredMode = Schema.RequiredMode.REQUIRED, example = "0dA1B2C3")
    @NotEmpty(message = "code 不能为空")
    private String code;

}
