package cn.iocoder.yudao.module.love.controller.app.matchapply;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyCreateReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyRespVO;
import cn.iocoder.yudao.module.love.service.matchapply.LoveMatchApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 牵线申请")
@RestController
@RequestMapping("/love/match-apply")
@Validated
public class AppLoveMatchApplyController {

    @Resource
    private LoveMatchApplyService loveMatchApplyService;

    @PostMapping("/create")
    @Operation(summary = "创建牵线申请")
    public CommonResult<Long> createApply(@RequestBody @Valid AppLoveMatchApplyCreateReqVO reqVO) {
        return success(loveMatchApplyService.createApply(getLoginUserId(), reqVO));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我的牵线申请分页")
    public CommonResult<PageResult<AppLoveMatchApplyRespVO>> getMyPage(AppLoveMatchApplyPageReqVO reqVO) {
        return success(loveMatchApplyService.getMyApplyPage(getLoginUserId(), reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取牵线申请详情")
    @Parameter(name = "id", required = true, example = "1")
    public CommonResult<AppLoveMatchApplyRespVO> getApply(@RequestParam("id") Long id) {
        return success(loveMatchApplyService.getMyApply(getLoginUserId(), id));
    }
}
