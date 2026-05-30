package cn.iocoder.yudao.server.service.image;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.service.image.provider.BailianImageTranslateProvider;
import cn.iocoder.yudao.server.service.image.provider.ImageTranslationProvider;
import cn.iocoder.yudao.server.service.image.provider.QwenMtImageProvider;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class ProviderFallbackPolicy {

    @Resource
    private BailianImageTranslateProvider bailianProvider;
    @Resource
    private QwenMtImageProvider qwenMtImageProvider;

    public ImageTranslationProvider primary(String preferProvider) {
        String prefer = StrUtil.blankToDefault(preferProvider, "auto");
        if (QwenMtImageProvider.NAME.equals(prefer)) {
            return qwenMtImageProvider;
        }
        if (BailianImageTranslateProvider.NAME.equals(prefer)) {
            return bailianProvider;
        }
        return qwenMtImageProvider;
    }

    public ImageTranslationProvider fallbackAfter(String providerName) {
        if (QwenMtImageProvider.NAME.equals(providerName)) {
            return bailianProvider.enabled() ? bailianProvider : null;
        }
        if (BailianImageTranslateProvider.NAME.equals(providerName)) {
            return qwenMtImageProvider.enabled() ? qwenMtImageProvider : null;
        }
        return null;
    }

    public boolean bailianEnabled() {
        return bailianProvider.enabled() || qwenMtImageProvider.enabled();
    }
}
