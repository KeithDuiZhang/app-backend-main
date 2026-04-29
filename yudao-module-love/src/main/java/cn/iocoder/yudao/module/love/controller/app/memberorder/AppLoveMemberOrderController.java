package cn.iocoder.yudao.module.love.controller.app.memberorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderCreateReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderRespVO;
import cn.iocoder.yudao.module.love.service.memberorder.LoveMemberOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 会员订单")
@RestController
@RequestMapping("/love/member-order")
@Validated
public class AppLoveMemberOrderController {

    @Resource
    private LoveMemberOrderService loveMemberOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建会员订单")
    public CommonResult<AppLoveMemberOrderCreateRespVO> createMemberOrder(@Valid @RequestBody AppLoveMemberOrderCreateReqVO reqVO) {
        return success(loveMemberOrderService.createMemberOrder(getLoginUserId(), reqVO.getPackageId(), getClientIP()));
    }

    @GetMapping("/get-current")
    @Operation(summary = "获取当前会员订单")
    public CommonResult<AppLoveMemberOrderRespVO> getCurrentOrder() {
        return success(loveMemberOrderService.getCurrentOrder(getLoginUserId()));
    }
}
