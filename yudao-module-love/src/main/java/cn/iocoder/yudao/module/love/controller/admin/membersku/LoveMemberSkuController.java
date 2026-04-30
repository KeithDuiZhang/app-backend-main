package cn.iocoder.yudao.module.love.controller.admin.membersku;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuRespVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;
import cn.iocoder.yudao.module.love.service.membersku.LoveMemberSkuService;
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

@Tag(name = "管理后台 - 婚恋会员 SKU")
@RestController
@RequestMapping("/love/member-sku")
@Validated
public class LoveMemberSkuController {

    @Resource
    private LoveMemberSkuService loveMemberSkuService;

    @GetMapping("/page")
    @Operation(summary = "获取会员 SKU 分页")
    @PreAuthorize("@ss.hasPermission('love:member-sku:query')")
    public CommonResult<PageResult<LoveMemberSkuRespVO>> getMemberSkuPage(@Validated LoveMemberSkuPageReqVO reqVO) {
        PageResult<LoveMemberSkuDO> pageResult = loveMemberSkuService.getMemberSkuPage(reqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), LoveMemberSkuRespVO.class), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获取会员 SKU 详情")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-sku:query')")
    public CommonResult<LoveMemberSkuRespVO> getMemberSku(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveMemberSkuService.getMemberSku(id), LoveMemberSkuRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建会员 SKU")
    @PreAuthorize("@ss.hasPermission('love:member-sku:create')")
    public CommonResult<Long> createMemberSku(@Valid @RequestBody LoveMemberSkuCreateReqVO reqVO) {
        return success(loveMemberSkuService.createMemberSku(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员 SKU")
    @PreAuthorize("@ss.hasPermission('love:member-sku:update')")
    public CommonResult<Boolean> updateMemberSku(@Valid @RequestBody LoveMemberSkuUpdateReqVO reqVO) {
        loveMemberSkuService.updateMemberSku(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员 SKU")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('love:member-sku:delete')")
    public CommonResult<Boolean> deleteMemberSku(@RequestParam("id") Long id) {
        loveMemberSkuService.deleteMemberSku(id);
        return success(true);
    }
}
