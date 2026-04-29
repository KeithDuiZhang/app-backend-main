package cn.iocoder.yudao.module.love.service.auth;

import cn.iocoder.yudao.module.love.service.auth.dto.LoveAuthLoginRespDTO;

public interface LoveAuthService {

    LoveAuthLoginRespDTO loginByWechatMiniCode(String code);

}
