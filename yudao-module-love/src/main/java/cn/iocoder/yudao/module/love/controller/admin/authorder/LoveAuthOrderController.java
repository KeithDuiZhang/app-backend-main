package cn.iocoder.yudao.module.love.controller.admin.authorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.authorder.vo.LoveAuthOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.authorder.vo.LoveAuthOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.auth.LoveAuthOrderDO;
import cn.iocoder.yudao.module.love.service.authorder.LoveAuthOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 认证订单")
@RestController
@RequestMapping("/love/auth-order")
@Validated
public class LoveAuthOrderController {

    @Resource
    private LoveAuthOrderService loveAuthOrderService;

    @GetMapping("/page")
    @Operation(summary = "获取认证订单分页")
    @PreAuthorize("@ss.hasPermission('love:auth-order:query')")
    public CommonResult<PageResult<LoveAuthOrderRespVO>> getPage(@Validated LoveAuthOrderPageReqVO reqVO) {
        PageResult<LoveAuthOrderDO> pageResult = loveAuthOrderService.getAuthOrderPage(reqVO);
        return success(BeanUtils.toBean(pageResult, LoveAuthOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取认证订单详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('love:auth-order:query')")
    public CommonResult<LoveAuthOrderRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveAuthOrderService.getAuthOrder(id), LoveAuthOrderRespVO.class));
    }
}
