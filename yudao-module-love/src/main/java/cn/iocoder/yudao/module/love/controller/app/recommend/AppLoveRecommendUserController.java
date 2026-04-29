package cn.iocoder.yudao.module.love.controller.app.recommend;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserRespVO;
import cn.iocoder.yudao.module.love.service.recommend.LoveRecommendUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 推荐用户")
@RestController
@RequestMapping("/love/recommend-user")
@Validated
public class AppLoveRecommendUserController {

    @Resource
    private LoveRecommendUserService loveRecommendUserService;

    @GetMapping("/page")
    @Operation(summary = "获取推荐用户分页")
    @PermitAll
    public CommonResult<PageResult<AppLoveRecommendUserRespVO>> getRecommendPage(AppLoveRecommendUserPageReqVO reqVO) {
        return success(loveRecommendUserService.getRecommendPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取推荐用户详情")
    @Parameter(name = "id", required = true, example = "1024")
    @PermitAll
    public CommonResult<AppLoveRecommendUserRespVO> getRecommendUser(@RequestParam("id") Long id) {
        return success(loveRecommendUserService.getRecommendUser(id));
    }
}
