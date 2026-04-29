package cn.iocoder.yudao.module.love.controller.admin.matchmaker;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerRespVO;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerSaveReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchmakerDO;
import cn.iocoder.yudao.module.love.service.matchmaker.LoveMatchmakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 红娘管理")
@RestController
@RequestMapping("/love/matchmaker")
@Validated
public class LoveMatchmakerController {

    @Resource
    private LoveMatchmakerService loveMatchmakerService;

    @GetMapping("/page")
    @Operation(summary = "获取红娘分页")
    @PreAuthorize("@ss.hasPermission('love:matchmaker:update')")
    public CommonResult<PageResult<LoveMatchmakerRespVO>> getPage(@Validated LoveMatchmakerPageReqVO reqVO) {
        PageResult<LoveMatchmakerDO> pageResult = loveMatchmakerService.getMatchmakerPage(reqVO);
        return success(BeanUtils.toBean(pageResult, LoveMatchmakerRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取红娘详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('love:matchmaker:update')")
    public CommonResult<LoveMatchmakerRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMatchmakerService.getMatchmaker(id), LoveMatchmakerRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建红娘")
    @PreAuthorize("@ss.hasPermission('love:matchmaker:create')")
    public CommonResult<Long> create(@Valid @RequestBody LoveMatchmakerSaveReqVO reqVO) {
        return success(loveMatchmakerService.createMatchmaker(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改红娘")
    @PreAuthorize("@ss.hasPermission('love:matchmaker:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody LoveMatchmakerSaveReqVO reqVO) {
        loveMatchmakerService.updateMatchmaker(reqVO);
        return success(true);
    }

    @PutMapping("/update-default")
    @Operation(summary = "设置默认红娘")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('love:matchmaker:update')")
    public CommonResult<Boolean> updateDefault(@RequestParam("id") Long id) {
        loveMatchmakerService.updateDefaultMatchmaker(id);
        return success(true);
    }
}
