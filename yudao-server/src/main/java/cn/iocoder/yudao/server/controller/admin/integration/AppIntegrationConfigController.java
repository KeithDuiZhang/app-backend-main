package cn.iocoder.yudao.server.controller.admin.integration;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService.IntegrationConfigRespVO;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService.IntegrationConfigSaveReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - Integration Config")
@RestController
@RequestMapping("/integration/config")
@Validated
public class AppIntegrationConfigController {

    @Resource
    private AppIntegrationConfigService integrationConfigService;

    @GetMapping("/get")
    @Operation(summary = "Get backend integration config")
    @ApiAccessLog(responseEnable = false)
    @PreAuthorize("@ss.hasPermission('app:integration-config:query')")
    public CommonResult<IntegrationConfigRespVO> getConfig() {
        return success(integrationConfigService.getEditableConfig());
    }

    @PutMapping("/save")
    @Operation(summary = "Save backend integration config")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    @PreAuthorize("@ss.hasPermission('app:integration-config:update')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody IntegrationConfigSaveReqVO reqVO) {
        integrationConfigService.saveConfig(reqVO);
        return success(true);
    }
}
