package cn.iocoder.yudao.module.love.controller.admin.membergroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员分组 Response VO")
@Data
public class LoveMemberGroupRespVO {

    private Long id;
    private String code;
    private String name;
    private Integer level;
    private String theme;
    private String benefitsJson;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
