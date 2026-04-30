package cn.iocoder.yudao.module.love.controller.app.memberupgrade;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeCreateReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradePreviewRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeResultRespVO;
import cn.iocoder.yudao.module.love.service.memberupgrade.LoveMemberUpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 会员升级")
@RestController
@RequestMapping("/love/member-upgrade")
@Validated
public class AppLoveMemberUpgradeController {

    @Resource
    private LoveMemberUpgradeService loveMemberUpgradeService;

    @GetMapping("/preview")
    @Operation(summary = "获取会员升级预览")
    public CommonResult<AppLoveMemberUpgradePreviewRespVO> preview(@RequestParam("targetSkuId") Long targetSkuId) {
        return success(loveMemberUpgradeService.preview(getLoginUserId(), targetSkuId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建会员升级订单")
    public CommonResult<AppLoveMemberUpgradeCreateRespVO> create(@Valid @RequestBody AppLoveMemberUpgradeCreateReqVO reqVO) {
        return success(loveMemberUpgradeService.createUpgradeOrder(getLoginUserId(), reqVO.getTargetSkuId(), getClientIP()));
    }

    @GetMapping("/result")
    @Operation(summary = "获取会员升级订单结果")
    public CommonResult<AppLoveMemberUpgradeResultRespVO> result(@RequestParam("id") Long id) {
        return success(loveMemberUpgradeService.getUpgradeResult(getLoginUserId(), id));
    }
}
