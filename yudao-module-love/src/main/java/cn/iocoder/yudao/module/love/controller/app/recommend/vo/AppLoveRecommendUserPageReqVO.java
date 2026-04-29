package cn.iocoder.yudao.module.love.controller.app.recommend.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户 App - 推荐用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppLoveRecommendUserPageReqVO extends PageParam {

    @Schema(description = "性别", example = "2")
    private Integer gender;

    @Schema(description = "婚姻状态", example = "1")
    private Integer maritalStatus;

    @Schema(description = "基准城市名称", example = "平江县")
    private String cityName;

    @Schema(description = "是否同城筛选", example = "true")
    private Boolean sameCity;

    @Schema(description = "最小年龄", example = "25")
    private Integer minAge;

    @Schema(description = "最大年龄", example = "32")
    private Integer maxAge;
}
