package cn.iocoder.yudao.module.love.controller.admin.membergroup;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupRespVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;
import cn.iocoder.yudao.module.love.service.membergroup.LoveMemberGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 婚恋会员分组")
@RestController
@RequestMapping("/love/member-group")
@Validated
public class LoveMemberGroupController {

    @Resource
    private LoveMemberGroupService loveMemberGroupService;

    @GetMapping("/page")
    @Operation(summary = "获取会员分组分页")
    @PreAuthorize("@ss.hasPermission('love:member-group:query')")
    public CommonResult<PageResult<LoveMemberGroupRespVO>> getMemberGroupPage(@Validated LoveMemberGroupPageReqVO reqVO) {
        PageResult<LoveMemberGroupDO> pageResult = loveMemberGroupService.getMemberGroupPage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), LoveMemberGroupRespVO.class), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获取会员分组详情")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-group:query')")
    public CommonResult<LoveMemberGroupRespVO> getMemberGroup(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMemberGroupService.getMemberGroup(id), LoveMemberGroupRespVO.class));
    }

    @GetMapping("/list-all")
    @Operation(summary = "获取启用会员分组列表")
    @PreAuthorize("@ss.hasPermission('love:member-group:query')")
    public CommonResult<java.util.List<LoveMemberGroupRespVO>> getEnabledMemberGroups() {
        return success(BeanUtils.toBean(loveMemberGroupService.getEnabledMemberGroups(), LoveMemberGroupRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建会员分组")
    @PreAuthorize("@ss.hasPermission('love:member-group:create')")
    public CommonResult<Long> createMemberGroup(@Valid @RequestBody LoveMemberGroupCreateReqVO reqVO) {
        return success(loveMemberGroupService.createMemberGroup(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员分组")
    @PreAuthorize("@ss.hasPermission('love:member-group:update')")
    public CommonResult<Boolean> updateMemberGroup(@Valid @RequestBody LoveMemberGroupUpdateReqVO reqVO) {
        loveMemberGroupService.updateMemberGroup(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员分组")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-group:delete')")
    public CommonResult<Boolean> deleteMemberGroup(@RequestParam("id") Long id) {
        loveMemberGroupService.deleteMemberGroup(id);
        return success(true);
    }
}
