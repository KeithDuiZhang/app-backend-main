package cn.iocoder.yudao.server.controller.app.image;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.app.AppAuthService;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CreateTaskFromCosReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CreateTaskRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CosUploadTicketReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.CosUploadTicketRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.RetryReqVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TaskStatusRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - Image Translation")
@RestController
@RequestMapping("/image-translation")
@Validated
public class AppImageTranslationController {

    @Resource
    private AppAuthService appAuthService;
    @Resource
    private ImageTranslationTaskService imageTranslationTaskService;

    @PostMapping("/upload-tickets")
    @PermitAll
    @Operation(summary = "Create COS direct upload ticket")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<CosUploadTicketRespVO> createUploadTicket(@RequestBody CosUploadTicketReqVO reqVO,
                                                                  HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(imageTranslationTaskService.createUploadTicket(userId, reqVO));
    }

    @PostMapping(value = "/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PermitAll
    @Operation(summary = "Create image translation task")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<CreateTaskRespVO> createTask(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("sourceLang") String sourceLang,
                                                     @RequestParam("targetLang") String targetLang,
                                                     @RequestParam(value = "mode", required = false, defaultValue = "AUTO") String mode,
                                                     @RequestParam(value = "preferProvider", required = false, defaultValue = "auto") String preferProvider,
                                                     HttpServletRequest request) throws IOException {
        Long userId = appAuthService.requireUserId(request);
        return success(imageTranslationTaskService.createTask(userId, file, sourceLang, targetLang, mode, preferProvider));
    }

    @PostMapping("/tasks/from-cos")
    @PermitAll
    @Operation(summary = "Create image translation task from uploaded COS object")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<CreateTaskRespVO> createTaskFromCos(@RequestBody CreateTaskFromCosReqVO reqVO,
                                                            HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(imageTranslationTaskService.createTaskFromCos(userId, reqVO));
    }

    @GetMapping("/tasks/{taskId}")
    @PermitAll
    @Operation(summary = "Get image translation task status")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<TaskStatusRespVO> getTask(@PathVariable("taskId") String taskId,
                                                  HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(imageTranslationTaskService.getTaskStatus(userId, taskId));
    }

    @PostMapping("/tasks/{taskId}/retry")
    @PermitAll
    @Operation(summary = "Retry image translation task")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<CreateTaskRespVO> retryTask(@PathVariable("taskId") String taskId,
                                                    @RequestBody(required = false) RetryReqVO reqVO,
                                                    HttpServletRequest request) {
        Long userId = appAuthService.requireUserId(request);
        return success(imageTranslationTaskService.retryTask(userId, taskId, reqVO));
    }
}
