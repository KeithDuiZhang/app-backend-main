package cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 红娘新增/修改 Request VO")
@Data
public class LoveMatchmakerSaveReqVO {

    private Long id;

    @NotBlank(message = "红娘名称不能为空")
    private String name;

    private String avatar;

    private String introduction;

    private String mobile;

    private String wechatNo;

    private Integer sort;

    private Boolean isDefault;

    private Integer status;
}
