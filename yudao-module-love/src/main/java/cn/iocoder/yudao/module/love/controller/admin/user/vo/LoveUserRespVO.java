package cn.iocoder.yudao.module.love.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 婚恋用户 Response VO")
@Data
public class LoveUserRespVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String mobile;
    private Integer status;
    private Integer authStatus;
    private LocalDateTime certifiedAt;
    private Integer memberLevel;
    private Integer freeMatchQuota;
    private LocalDateTime lastQuotaResetAt;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
