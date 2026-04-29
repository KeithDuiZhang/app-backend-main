package cn.iocoder.yudao.module.love.controller.app.user;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.user.vo.AppLoveUserRespVO;
import cn.iocoder.yudao.module.love.service.user.LoveUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 婚恋用户")
@RestController
@RequestMapping("/love/user")
@Validated
public class AppLoveUserController {

    @Resource
    private LoveUserService loveUserService;

    @GetMapping("/get-my")
    @Operation(summary = "获取当前登录用户摘要")
    public CommonResult<AppLoveUserRespVO> getMy() {
        return success(loveUserService.getMyUser(getLoginUserId()));
    }
}
