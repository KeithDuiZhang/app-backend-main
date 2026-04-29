package cn.iocoder.yudao.module.love.controller.app.memberpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "用户 App - 会员套餐 Response VO")
@Data
public class AppLoveMemberPackageRespVO {

    private Long id;

    private String name;

    private Integer level;

    private Integer priceFen;

    private Integer durationMonths;

    private String durationText;

    private String description;

    private List<String> features;

    private String theme;

    private Boolean popular;

    private Integer sort;
}
