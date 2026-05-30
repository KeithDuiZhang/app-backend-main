package cn.iocoder.yudao.server.controller.app.model;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.app.AppAuthService;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.DownloadUrlReqVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.DownloadUrlRespVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.ModelCatalogRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Offline Models")
@RestController
@RequestMapping("/offline-models")
@Validated
public class AppOfflineModelController {

    @Resource
    private AppOfflineModelService offlineModelService;
    @Resource
    private AppAuthService appAuthService;

    @GetMapping("/catalog")
    @PermitAll
    @Operation(summary = "Get offline model catalog")
    public CommonResult<ModelCatalogRespVO> getCatalog() {
        return success(offlineModelService.getCatalog());
    }

    @PostMapping("/download-urls")
    @PermitAll
    @Operation(summary = "Create COS signed download URLs")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<DownloadUrlRespVO> createDownloadUrls(@Valid @RequestBody DownloadUrlReqVO reqVO,
                                                              HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(offlineModelService.createDownloadUrls(userId, reqVO));
    }
}
