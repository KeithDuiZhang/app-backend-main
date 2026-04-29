package cn.iocoder.yudao.module.love.controller.admin.memberorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.memberorder.vo.LoveMemberOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberorder.vo.LoveMemberOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberOrderDO;
import cn.iocoder.yudao.module.love.service.memberorder.LoveMemberOrderService;
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

@Tag(name = "管理后台 - 婚恋会员订单")
@RestController
@RequestMapping("/love/member-order")
@Validated
public class LoveMemberOrderController {

    @Resource
    private LoveMemberOrderService loveMemberOrderService;

    @GetMapping("/page")
    @Operation(summary = "获取会员订单分页")
    @PreAuthorize("@ss.hasPermission('love:member-order:query')")
    public CommonResult<PageResult<LoveMemberOrderRespVO>> getMemberOrderPage(@Validated LoveMemberOrderPageReqVO reqVO) {
        PageResult<LoveMemberOrderDO> pageResult = loveMemberOrderService.getMemberOrderPage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), LoveMemberOrderRespVO.class), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获取会员订单详情")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-order:query')")
    public CommonResult<LoveMemberOrderRespVO> getMemberOrder(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMemberOrderService.getMemberOrder(id), LoveMemberOrderRespVO.class));
    }
}
