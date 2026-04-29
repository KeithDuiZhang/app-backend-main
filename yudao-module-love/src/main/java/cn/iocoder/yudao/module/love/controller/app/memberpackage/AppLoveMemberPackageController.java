package cn.iocoder.yudao.module.love.controller.app.memberpackage;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.memberpackage.vo.AppLoveMemberPackageRespVO;
import cn.iocoder.yudao.module.love.service.memberpackage.LoveMemberPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 会员套餐")
@RestController
@RequestMapping("/love/member-package")
@Validated
public class AppLoveMemberPackageController {

    @Resource
    private LoveMemberPackageService loveMemberPackageService;

    @GetMapping("/list")
    @PermitAll
    @Operation(summary = "获取会员套餐列表")
    public CommonResult<List<AppLoveMemberPackageRespVO>> getMemberPackageList() {
        return success(loveMemberPackageService.getEnableMemberPackageList());
    }
}
