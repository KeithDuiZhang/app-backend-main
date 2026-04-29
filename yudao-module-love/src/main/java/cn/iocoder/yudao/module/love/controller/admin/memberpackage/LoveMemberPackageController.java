package cn.iocoder.yudao.module.love.controller.admin.memberpackage;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackagePageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageRespVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberPackageDO;
import cn.iocoder.yudao.module.love.service.memberpackage.LoveMemberPackageService;
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

@Tag(name = "管理后台 - 婚恋会员套餐")
@RestController
@RequestMapping("/love/member-package")
@Validated
public class LoveMemberPackageController {

    @Resource
    private LoveMemberPackageService loveMemberPackageService;

    @GetMapping("/page")
    @Operation(summary = "获取会员套餐分页")
    @PreAuthorize("@ss.hasPermission('love:member-package:query')")
    public CommonResult<PageResult<LoveMemberPackageRespVO>> getMemberPackagePage(@Validated LoveMemberPackagePageReqVO reqVO) {
        PageResult<LoveMemberPackageDO> pageResult = loveMemberPackageService.getMemberPackagePage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), LoveMemberPackageRespVO.class), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获取会员套餐详情")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-package:query')")
    public CommonResult<LoveMemberPackageRespVO> getMemberPackage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMemberPackageService.getMemberPackage(id), LoveMemberPackageRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建会员套餐")
    @PreAuthorize("@ss.hasPermission('love:member-package:create')")
    public CommonResult<Long> createMemberPackage(@Valid @RequestBody LoveMemberPackageCreateReqVO reqVO) {
        return success(loveMemberPackageService.createMemberPackage(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员套餐")
    @PreAuthorize("@ss.hasPermission('love:member-package:update')")
    public CommonResult<Boolean> updateMemberPackage(@Valid @RequestBody LoveMemberPackageUpdateReqVO reqVO) {
        loveMemberPackageService.updateMemberPackage(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员套餐")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-package:delete')")
    public CommonResult<Boolean> deleteMemberPackage(@RequestParam("id") Long id) {
        loveMemberPackageService.deleteMemberPackage(id);
        return success(true);
    }
}
