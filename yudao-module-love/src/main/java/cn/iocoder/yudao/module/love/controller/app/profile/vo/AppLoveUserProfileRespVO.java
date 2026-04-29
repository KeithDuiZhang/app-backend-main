package cn.iocoder.yudao.module.love.controller.app.profile.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "用户 App - 婚恋用户资料 Response VO")
@Data
public class AppLoveUserProfileRespVO {

    private Long userId;

    private String realName;

    private Integer gender;

    private LocalDate birthday;

    private String cityCode;

    private String cityName;

    private Integer maritalStatus;

    private Integer heightCm;

    private Integer weightKg;

    private String profession;

    private String education;

    private String incomeDesc;

    private String photos;

    private String bio;

    private String tags;

    private String partnerPreference;

    private Boolean profilePublic;

    private Integer completionRate;
}
