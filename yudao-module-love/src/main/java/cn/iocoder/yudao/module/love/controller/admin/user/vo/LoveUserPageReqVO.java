package cn.iocoder.yudao.module.love.controller.admin.user.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 婚恋用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveUserPageReqVO extends PageParam {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "认证状态")
    private Integer authStatus;
}
