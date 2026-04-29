package cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员套餐 Response VO")
@Data
public class LoveMemberPackageRespVO {

    private Long id;

    private String name;

    private Integer level;

    private Integer priceFen;

    private Integer durationMonths;

    private String description;

    private String featuresJson;

    private String theme;

    private Boolean popular;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;
}
