package cn.iocoder.yudao.module.love.controller.admin.user;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.love.controller.admin.user.vo.LoveUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.user.vo.LoveUserRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.service.user.LoveUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 婚恋用户")
@RestController
@RequestMapping("/love/user")
@Validated
public class LoveUserController {

    @Resource
    private LoveUserService loveUserService;

    @GetMapping("/page")
    @Operation(summary = "获取婚恋用户分页")
    @PreAuthorize("@ss.hasPermission('love:user:query')")
    public CommonResult<PageResult<LoveUserRespVO>> getUserPage(@Validated LoveUserPageReqVO reqVO) {
        PageResult<LoveUserDO> pageResult = loveUserService.getUserPage(reqVO);
        return success(BeanUtils.toBean(pageResult, LoveUserRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取婚恋用户详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('love:user:query')")
    public CommonResult<LoveUserRespVO> getUser(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(loveUserService.getUser(id), LoveUserRespVO.class));
    }
}
