package cn.iocoder.yudao.server.controller.app.client;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.app.AppClientCommandService;
import cn.iocoder.yudao.server.service.app.AppClientCommandService.CommandAckReqVO;
import cn.iocoder.yudao.server.service.app.AppClientCommandService.HeartbeatReqVO;
import cn.iocoder.yudao.server.service.app.AppClientCommandService.HeartbeatRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Client Maintenance")
@RestController
@RequestMapping("/client")
@Validated
public class AppClientMaintenanceController {

    @Resource
    private AppClientCommandService clientCommandService;

    @PostMapping("/heartbeat")
    @PermitAll
    @Operation(summary = "Pull client maintenance commands")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HeartbeatRespVO> heartbeat(@Valid @RequestBody HeartbeatReqVO reqVO) {
        return success(clientCommandService.heartbeat(reqVO));
    }

    @PostMapping("/commands/{id}/ack")
    @PermitAll
    @Operation(summary = "Ack client maintenance command")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Boolean> ack(@PathVariable("id") Long id,
                                     @Valid @RequestBody CommandAckReqVO reqVO) {
        return success(clientCommandService.ack(id, reqVO));
    }
}
