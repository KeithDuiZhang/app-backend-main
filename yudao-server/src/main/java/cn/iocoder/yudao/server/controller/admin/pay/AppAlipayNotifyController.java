package cn.iocoder.yudao.server.controller.admin.pay;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.server.service.app.AppPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - App Alipay Notify")
@RestController
@RequestMapping("/app-pay/alipay")
@Validated
public class AppAlipayNotifyController {

    @Resource
    private AppPaymentService appPaymentService;

    @PostMapping("/notify")
    @PermitAll
    @Operation(summary = "Alipay async notify")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public String notify(HttpServletRequest request) {
        return appPaymentService.handleAlipayNotify(ServletUtils.getParamMap(request)) ? "success" : "failure";
    }
}
