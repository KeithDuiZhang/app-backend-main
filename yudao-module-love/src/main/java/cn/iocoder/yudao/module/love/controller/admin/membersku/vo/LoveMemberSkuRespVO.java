package cn.iocoder.yudao.module.love.controller.admin.membersku.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员 SKU Response VO")
@Data
public class LoveMemberSkuRespVO {

    private Long id;
    private Long groupId;
    private Integer durationType;
    private Integer durationDays;
    private Integer salePriceFen;
    private Integer originalPriceFen;
    private String tagText;
    private Boolean recommend;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
