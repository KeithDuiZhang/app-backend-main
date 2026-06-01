package cn.iocoder.yudao.server.controller.app.pay;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.server.service.app.AppAuthService;
import cn.iocoder.yudao.server.service.app.AppPaymentService;
import cn.iocoder.yudao.server.service.app.AppPaymentService.CreateWapPayReqVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.CreateWapPayRespVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.OfflineMembershipProductRespVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.OrderStatusRespVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.TokenProductRespVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UserEntitlementRespVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UsageConsumeReqVO;
import cn.iocoder.yudao.server.service.app.AppPaymentService.UsageRecordRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Payment")
@RestController
@RequestMapping("/pay")
@Validated
public class AppPaymentController {

    @Resource
    private AppAuthService appAuthService;
    @Resource
    private AppPaymentService appPaymentService;

    @GetMapping("/products/token")
    @PermitAll
    @Operation(summary = "List token packages")
    public CommonResult<List<TokenProductRespVO>> listTokenProducts() {
        return success(appPaymentService.listTokenProducts());
    }

    @GetMapping("/products/online-packages")
    @PermitAll
    @Operation(summary = "List online packages")
    public CommonResult<List<TokenProductRespVO>> listOnlinePackageProducts() {
        return success(appPaymentService.listTokenProducts());
    }

    @GetMapping("/products/offline-membership")
    @PermitAll
    @Operation(summary = "List offline membership packages")
    public CommonResult<List<OfflineMembershipProductRespVO>> listOfflineMembershipProducts() {
        return success(appPaymentService.listOfflineMembershipProducts());
    }

    @PostMapping("/alipay/wap/create")
    @PermitAll
    @Operation(summary = "Create Alipay WAP payment")
    public CommonResult<CreateWapPayRespVO> createAlipayWapPay(@Valid @RequestBody CreateWapPayReqVO reqVO,
                                                               HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.createAlipayWapOrder(userId, reqVO, ServletUtils.getClientIP(request)));
    }

    @GetMapping(value = "/alipay/return", produces = MediaType.TEXT_HTML_VALUE)
    @PermitAll
    @Operation(summary = "Alipay WAP return page")
    public String alipayReturnPage() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>支付结果处理中</title>
                    <style>
                        body{margin:0;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:#f7f8fb;color:#172033}
                        main{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;box-sizing:border-box}
                        section{max-width:420px;background:#fff;border-radius:18px;padding:28px 24px;box-shadow:0 12px 36px rgba(17,24,39,.08);text-align:center}
                        h1{font-size:22px;margin:0 0 12px}
                        p{font-size:15px;line-height:1.7;margin:0;color:#5d6678}
                    </style>
                </head>
                <body>
                    <main>
                        <section>
                            <h1>支付结果处理中</h1>
                            <p>请返回鲲穹翻译机 App 查看订单状态。支付是否成功以后端支付宝异步通知确认为准。</p>
                        </section>
                    </main>
                </body>
                </html>
                """;
    }

    @PostMapping("/alipay/notify")
    @PermitAll
    @Operation(summary = "Alipay async notify")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public String alipayNotify(HttpServletRequest request) {
        return appPaymentService.handleAlipayNotify(ServletUtils.getParamMap(request)) ? "success" : "failure";
    }

    @GetMapping("/orders/{orderNo}")
    @PermitAll
    @Operation(summary = "Get payment order status")
    public CommonResult<OrderStatusRespVO> getOrderStatus(@PathVariable("orderNo") String orderNo,
                                                          HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.getOrderStatus(userId, orderNo));
    }

    @GetMapping("/orders")
    @PermitAll
    @Operation(summary = "List current app user payment orders")
    public CommonResult<List<OrderStatusRespVO>> listOrders(@RequestParam(value = "range", required = false, defaultValue = "30d") String range,
                                                            @RequestParam(value = "status", required = false) String status,
                                                            @RequestParam(value = "productType", required = false) String productType,
                                                            HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.listUserOrders(userId, range, status, productType));
    }

    @PostMapping("/orders/query")
    @PermitAll
    @Operation(summary = "Query current app user payment order")
    public CommonResult<OrderStatusRespVO> queryOrder(@RequestBody Map<String, String> body,
                                                      HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.getOrderStatus(userId, body.get("orderNo")));
    }

    @GetMapping("/me/entitlement")
    @PermitAll
    @Operation(summary = "Get current app user entitlement")
    public CommonResult<UserEntitlementRespVO> getMyEntitlement(HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.getUserEntitlement(userId));
    }

    @GetMapping("/me/entitlements")
    @PermitAll
    @Operation(summary = "Get current app user entitlements")
    public CommonResult<UserEntitlementRespVO> getMyEntitlements(HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.getUserEntitlement(userId));
    }

    @GetMapping("/me/quota")
    @PermitAll
    @Operation(summary = "Get current app user quota")
    public CommonResult<UserEntitlementRespVO> getMyQuota(HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.getUserEntitlement(userId));
    }

    @PostMapping("/usage/consume")
    @PermitAll
    @Operation(summary = "Consume online package quota")
    public CommonResult<UserEntitlementRespVO> consumeOnlineUsage(@Valid @RequestBody UsageConsumeReqVO reqVO,
                                                                  HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.consumeOnlineUsage(userId, reqVO));
    }

    @GetMapping("/usage/records")
    @PermitAll
    @Operation(summary = "List real usage and paid package records")
    public CommonResult<List<UsageRecordRespVO>> listUsageRecords(HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.listUsageRecords(userId));
    }
}
