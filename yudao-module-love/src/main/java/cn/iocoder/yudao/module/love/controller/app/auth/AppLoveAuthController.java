package cn.iocoder.yudao.module.love.controller.app.auth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.auth.vo.AppLoveWechatMiniLoginReqVO;
import cn.iocoder.yudao.module.love.controller.app.auth.vo.AppLoveWechatMiniLoginRespVO;
import cn.iocoder.yudao.module.love.service.auth.LoveAuthService;
import cn.iocoder.yudao.module.love.service.auth.dto.LoveAuthLoginRespDTO;
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

@Tag(name = "用户 App - 婚恋认证")
@RestController
@RequestMapping("/love/auth")
@Validated
public class AppLoveAuthController {

    @Resource
    private LoveAuthService loveAuthService;

    @PostMapping("/wechat-mini-login")
    @PermitAll
    @Operation(summary = "微信小程序登录")
    public CommonResult<AppLoveWechatMiniLoginRespVO> wechatMiniLogin(@RequestBody @Valid AppLoveWechatMiniLoginReqVO reqVO) {
        LoveAuthLoginRespDTO loginRespDTO = loveAuthService.loginByWechatMiniCode(reqVO.getCode());
        AppLoveWechatMiniLoginRespVO respVO = new AppLoveWechatMiniLoginRespVO();
        respVO.setAccessToken(loginRespDTO.getAccessToken());
        respVO.setRefreshToken(loginRespDTO.getRefreshToken());
        respVO.setUserId(loginRespDTO.getUserId());
        respVO.setOpenid(loginRespDTO.getOpenid());
        respVO.setProfileCompleted(loginRespDTO.getProfileCompleted());
        respVO.setAuthStatus(loginRespDTO.getAuthStatus());
        return success(respVO);
    }

}
