package cn.iocoder.yudao.server.controller.admin.integration;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.UploadPublishReqVO;
import cn.iocoder.yudao.server.service.app.AppOfflineModelService.UploadPublishRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - Offline Model Publish")
@RestController
@RequestMapping("/integration/offline-models")
@Validated
public class AppOfflineModelAdminController {

    @Resource
    private AppOfflineModelService offlineModelService;

    @PostMapping("/publish-local")
    @Operation(summary = "Upload local published offline model repository to Tencent COS")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PreAuthorize("@ss.hasPermission('app:integration-config:update')")
    public CommonResult<UploadPublishRespVO> publishLocal(@Valid @RequestBody(required = false) UploadPublishReqVO reqVO)
            throws IOException {
        return success(offlineModelService.uploadPublishedRepository(reqVO));
    }
}
