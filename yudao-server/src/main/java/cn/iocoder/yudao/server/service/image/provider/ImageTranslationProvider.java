package cn.iocoder.yudao.server.service.image.provider;

import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRateLimit;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRequest;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;

public interface ImageTranslationProvider {

    String providerName();

    boolean enabled();

    ProviderRateLimit rateLimit();

    ProviderResult translate(ProviderRequest request);
}
