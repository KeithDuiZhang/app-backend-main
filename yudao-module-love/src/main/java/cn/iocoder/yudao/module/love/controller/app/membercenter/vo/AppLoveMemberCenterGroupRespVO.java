package cn.iocoder.yudao.module.love.controller.app.membercenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "用户 App - 会员中心会员分组 Response VO")
@Data
public class AppLoveMemberCenterGroupRespVO {

    private Long groupId;

    private String code;

    private String name;

    private Integer level;

    private String theme;

    private List<String> benefits;

    private List<AppLoveMemberCenterSkuRespVO> skus;
}
