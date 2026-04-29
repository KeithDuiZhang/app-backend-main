package cn.iocoder.yudao.module.love.controller.app.profile.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "用户 App - 婚恋用户基础资料更新 Request VO")
@Data
public class AppLoveUserProfileUpdateBasicReqVO {

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "性别不能为空")
    private Integer gender;

    @Schema(description = "生日", requiredMode = Schema.RequiredMode.REQUIRED, example = "1995-05-01")
    @NotNull(message = "生日不能为空")
    private LocalDate birthday;

    @Schema(description = "城市编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "430626")
    @NotBlank(message = "城市编码不能为空")
    private String cityCode;

    @Schema(description = "城市名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "平江县")
    @NotBlank(message = "城市名称不能为空")
    private String cityName;

    @Schema(description = "婚姻状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "婚姻状态不能为空")
    private Integer maritalStatus;
}
