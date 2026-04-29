package cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 红娘 Response VO")
@Data
public class LoveMatchmakerRespVO {

    private Long id;
    private String name;
    private String avatar;
    private String introduction;
    private String mobile;
    private String wechatNo;
    private Integer sort;
    private Boolean isDefault;
    private Integer status;
    private LocalDateTime createTime;
}
