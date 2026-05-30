package cn.iocoder.yudao.server.controller.app.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.app.AppAuthService;
import cn.iocoder.yudao.server.service.app.AppPaymentService;
import cn.iocoder.yudao.server.service.app.AppPaymentService.FeedbackSubmitReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Feedback")
@RestController
@RequestMapping("/feedback")
@Validated
public class AppFeedbackController {

    @Resource
    private AppAuthService appAuthService;
    @Resource
    private AppPaymentService appPaymentService;

    @PostMapping("/submit")
    @PermitAll
    @Operation(summary = "Submit app feedback")
    public CommonResult<Boolean> submitFeedback(@Valid @RequestBody FeedbackSubmitReqVO reqVO,
                                                HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(appPaymentService.submitFeedback(userId, reqVO));
    }
}
