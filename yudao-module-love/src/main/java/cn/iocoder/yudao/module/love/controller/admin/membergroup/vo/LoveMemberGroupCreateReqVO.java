package cn.iocoder.yudao.module.love.controller.admin.membergroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员分组创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberGroupCreateReqVO extends LoveMemberGroupBaseReqVO {
}
