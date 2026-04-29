package cn.iocoder.yudao.module.love.controller.app.recommend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 推荐用户 Response VO")
@Data
public class AppLoveRecommendUserRespVO {

    private Long userId;

    private String nickname;

    private String avatar;

    private Integer gender;

    private Integer age;

    private String cityName;

    private Integer maritalStatus;

    private String bio;

    private String tags;

    private Integer authStatus;

    private Integer memberLevel;
}
