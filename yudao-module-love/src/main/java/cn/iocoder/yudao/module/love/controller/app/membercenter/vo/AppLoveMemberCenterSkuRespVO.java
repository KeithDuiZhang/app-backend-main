package cn.iocoder.yudao.module.love.controller.app.membercenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 会员中心 SKU Response VO")
@Data
public class AppLoveMemberCenterSkuRespVO {

    private Long skuId;

    private Integer durationType;

    private String durationCode;

    private Integer durationDays;

    private Integer originalPriceFen;

    private Integer salePriceFen;

    private String tagText;

    private Boolean recommend;

    private Integer sort;
}
