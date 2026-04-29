package cn.iocoder.yudao.module.love.controller.app.profile;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileRespVO;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileUpdateBasicReqVO;
import cn.iocoder.yudao.module.love.service.profile.LoveUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 婚恋用户资料")
@RestController
@RequestMapping("/love/user-profile")
@Validated
public class AppLoveUserProfileController {

    @Resource
    private LoveUserProfileService loveUserProfileService;

    @GetMapping("/get")
    @Operation(summary = "获取我的资料")
    public CommonResult<AppLoveUserProfileRespVO> getMyProfile() {
        return success(loveUserProfileService.getProfile(getLoginUserId()));
    }

    @PutMapping("/update-basic")
    @Operation(summary = "更新基础 Tab1 资料")
    public CommonResult<Boolean> updateBasic(@RequestBody @Valid AppLoveUserProfileUpdateBasicReqVO reqVO) {
        loveUserProfileService.updateBasicProfile(getLoginUserId(), reqVO);
        return success(true);
    }
}
