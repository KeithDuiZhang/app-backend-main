package cn.iocoder.yudao.server.controller.app.auth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.server.service.app.AppAuthService;
import cn.iocoder.yudao.server.service.app.AppAuthService.LoginRespVO;
import cn.iocoder.yudao.server.service.app.AppAuthService.PasswordLoginReqVO;
import cn.iocoder.yudao.server.service.app.AppAuthService.RegisterReqVO;
import cn.iocoder.yudao.server.service.app.AppAuthService.ResetPasswordReqVO;
import cn.iocoder.yudao.server.service.app.AppAuthService.SmsLoginReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Auth")
@RestController
@RequestMapping("/auth")
@Validated
public class AppAuthController {

    @Resource
    private AppAuthService appAuthService;

    @PostMapping("/sms-code/send")
    @PermitAll
    @Operation(summary = "Send SMS code")
    public CommonResult<Boolean> sendSmsCode(@Valid @RequestBody SmsCodeSendReqDTO reqDTO) {
        appAuthService.sendSmsCode(reqDTO, ServletUtils.getClientIP());
        return success(true);
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "Register app user")
    public CommonResult<LoginRespVO> register(@Valid @RequestBody RegisterReqVO reqVO) {
        return success(appAuthService.register(reqVO, ServletUtils.getClientIP()));
    }

    @PostMapping("/password-login")
    @PermitAll
    @Operation(summary = "Login by mobile and password")
    public CommonResult<LoginRespVO> passwordLogin(@Valid @RequestBody PasswordLoginReqVO reqVO) {
        return success(appAuthService.passwordLogin(reqVO));
    }

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "Login by SMS code")
    public CommonResult<LoginRespVO> smsLogin(@Valid @RequestBody SmsLoginReqVO reqVO) {
        return success(appAuthService.smsLogin(reqVO, ServletUtils.getClientIP()));
    }

    @PostMapping("/password/reset")
    @PermitAll
    @Operation(summary = "Reset password by SMS code")
    public CommonResult<Boolean> resetPassword(@Valid @RequestBody ResetPasswordReqVO reqVO) {
        appAuthService.resetPassword(reqVO, ServletUtils.getClientIP());
        return success(true);
    }
}
