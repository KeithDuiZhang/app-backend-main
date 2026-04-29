package cn.iocoder.yudao.module.love.controller.admin.matchapply;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyOperateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyRejectReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchApplyDO;
import cn.iocoder.yudao.module.love.service.matchapply.LoveMatchApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 牵线申请")
@RestController
@RequestMapping("/love/match-apply")
@Validated
public class LoveMatchApplyController {

    @Resource
    private LoveMatchApplyService loveMatchApplyService;

    @GetMapping("/page")
    @Operation(summary = "获取牵线申请分页")
    @PreAuthorize("@ss.hasPermission('love:match-apply:query')")
    public CommonResult<PageResult<LoveMatchApplyRespVO>> getPage(@Validated LoveMatchApplyPageReqVO reqVO) {
        PageResult<LoveMatchApplyDO> pageResult = loveMatchApplyService.getApplyPage(reqVO);
        return success(BeanUtils.toBean(pageResult, LoveMatchApplyRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取牵线申请详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('love:match-apply:query')")
    public CommonResult<LoveMatchApplyRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMatchApplyService.getApply(id), LoveMatchApplyRespVO.class));
    }

    @PostMapping("/start-contact")
    @Operation(summary = "开始联系")
    @PreAuthorize("@ss.hasPermission('love:match-apply:update')")
    public CommonResult<Boolean> startContact(@Valid @RequestBody LoveMatchApplyOperateReqVO reqVO) {
        loveMatchApplyService.startContact(reqVO.getId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/success")
    @Operation(summary = "标记成功")
    @PreAuthorize("@ss.hasPermission('love:match-apply:update')")
    public CommonResult<Boolean> successApply(@Valid @RequestBody LoveMatchApplyOperateReqVO reqVO) {
        loveMatchApplyService.markSuccess(reqVO.getId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "标记拒绝")
    @PreAuthorize("@ss.hasPermission('love:match-apply:update')")
    public CommonResult<Boolean> rejectApply(@Valid @RequestBody LoveMatchApplyRejectReqVO reqVO) {
        loveMatchApplyService.markRejected(reqVO.getId(), getLoginUserId(), reqVO.getRejectReason());
        return success(true);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭申请")
    @PreAuthorize("@ss.hasPermission('love:match-apply:update')")
    public CommonResult<Boolean> closeApply(@Valid @RequestBody LoveMatchApplyOperateReqVO reqVO) {
        loveMatchApplyService.closeApply(reqVO.getId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }
}
