package cn.iocoder.yudao.module.love.controller.app.membercenter;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.membercenter.vo.AppLoveMemberCenterOverviewRespVO;
import cn.iocoder.yudao.module.love.service.membercenter.LoveMemberCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 会员中心")
@RestController
@RequestMapping("/love/member-center")
@Validated
public class AppLoveMemberCenterController {

    @Resource
    private LoveMemberCenterService loveMemberCenterService;

    @GetMapping("/overview")
    @Operation(summary = "获取会员中心总览")
    public CommonResult<AppLoveMemberCenterOverviewRespVO> getOverview(
            @RequestParam(value = "selectedGroupCode", required = false) String selectedGroupCode,
            @RequestParam(value = "selectedDurationCode", required = false) String selectedDurationCode) {
        return success(loveMemberCenterService.getOverview(getLoginUserId(), selectedGroupCode, selectedDurationCode));
    }
}
