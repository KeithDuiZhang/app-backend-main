package cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder.vo.LoveMemberUpgradeOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder.vo.LoveMemberUpgradeOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberUpgradeOrderDO;
import cn.iocoder.yudao.module.love.service.memberupgradeorder.LoveMemberUpgradeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 婚恋会员升级订单")
@RestController
@RequestMapping("/love/member-upgrade-order")
@Validated
public class LoveMemberUpgradeOrderController {

    @Resource
    private LoveMemberUpgradeOrderService loveMemberUpgradeOrderService;

    @GetMapping("/page")
    @Operation(summary = "获取会员升级订单分页")
    @PreAuthorize("@ss.hasPermission('love:member-upgrade-order:query')")
    public CommonResult<PageResult<LoveMemberUpgradeOrderRespVO>> getUpgradeOrderPage(@Validated LoveMemberUpgradeOrderPageReqVO reqVO) {
        PageResult<LoveMemberUpgradeOrderDO> pageResult = loveMemberUpgradeOrderService.getUpgradeOrderPage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), LoveMemberUpgradeOrderRespVO.class), pageResult.getTotal()));
    }
}
