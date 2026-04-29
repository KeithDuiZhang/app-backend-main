package cn.iocoder.yudao.module.love.service.auth.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LoveAuthLoginRespDTO {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String openid;

    private Boolean profileCompleted;

    private Integer authStatus;

}
