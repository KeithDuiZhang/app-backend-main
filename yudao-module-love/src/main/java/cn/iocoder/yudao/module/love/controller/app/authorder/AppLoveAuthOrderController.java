package cn.iocoder.yudao.module.love.controller.app.authorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderRespVO;
import cn.iocoder.yudao.module.love.service.authorder.LoveAuthOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 婚恋认证订单")
@RestController
@RequestMapping("/love/auth-order")
@Validated
public class AppLoveAuthOrderController {

    @Resource
    private LoveAuthOrderService loveAuthOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建认证订单")
    public CommonResult<AppLoveAuthOrderCreateRespVO> createAuthOrder() {
        return success(loveAuthOrderService.createAuthOrder(getLoginUserId(), getClientIP()));
    }

    @GetMapping("/get-current")
    @Operation(summary = "获取当前认证订单")
    public CommonResult<AppLoveAuthOrderRespVO> getCurrentOrder() {
        return success(loveAuthOrderService.getCurrentOrder(getLoginUserId()));
    }
}
