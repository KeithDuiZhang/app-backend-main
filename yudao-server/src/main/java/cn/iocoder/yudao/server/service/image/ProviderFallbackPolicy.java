package cn.iocoder.yudao.server.service.image;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.service.image.provider.AliyunOcrTextImageFallbackProvider;
import cn.iocoder.yudao.server.service.image.provider.AliyunTranslateImageProvider;
import cn.iocoder.yudao.server.service.image.provider.BailianImageTranslateProvider;
import cn.iocoder.yudao.server.service.image.provider.ImageTranslationProvider;
import cn.iocoder.yudao.server.service.image.provider.QwenMtImageProvider;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProviderFallbackPolicy {

    @Resource
    private BailianImageTranslateProvider bailianProvider;
    @Resource
    private QwenMtImageProvider qwenMtImageProvider;
    @Resource
    private AliyunOcrTextImageFallbackProvider aliyunOcrTextProvider;
    @Resource
    private AliyunTranslateImageProvider aliyunTranslateImageProvider;

    public ImageTranslationProvider primary(String preferProvider) {
        String prefer = StrUtil.blankToDefault(preferProvider, "auto");
        if (QwenMtImageProvider.NAME.equals(prefer)) {
            return qwenMtImageProvider;
        }
        if (BailianImageTranslateProvider.NAME.equals(prefer)) {
            return bailianProvider;
        }
        if (AliyunTranslateImageProvider.NAME.equals(prefer)) {
            return aliyunTranslateImageProvider;
        }
        if (AliyunOcrTextImageFallbackProvider.NAME.equals(prefer)) {
            return aliyunOcrTextProvider;
        }
        return qwenMtImageProvider;
    }

    public ImageTranslationProvider fallbackAfter(String providerName) {
        List<ImageTranslationProvider> providers = fallbacksAfter(providerName);
        return providers.isEmpty() ? null : providers.get(0);
    }

    public List<ImageTranslationProvider> fallbacksAfter(String providerName) {
        ArrayList<ImageTranslationProvider> providers = new ArrayList<>();
        if (QwenMtImageProvider.NAME.equals(providerName)) {
            if (aliyunOcrTextProvider.enabled()) {
                if (aliyunTranslateImageProvider.enabled()) {
                    providers.add(aliyunTranslateImageProvider);
                }
                providers.add(aliyunOcrTextProvider);
                return providers;
            }
            if (aliyunTranslateImageProvider.enabled()) {
                providers.add(aliyunTranslateImageProvider);
            }
            if (bailianProvider.enabled()) {
                providers.add(bailianProvider);
            }
            return providers;
        }
        if (BailianImageTranslateProvider.NAME.equals(providerName)) {
            if (qwenMtImageProvider.enabled()) {
                providers.add(qwenMtImageProvider);
            }
            return providers;
        }
        if (AliyunOcrTextImageFallbackProvider.NAME.equals(providerName)) {
            if (bailianProvider.enabled()) {
                providers.add(bailianProvider);
            }
            return providers;
        }
        if (AliyunTranslateImageProvider.NAME.equals(providerName)) {
            if (aliyunOcrTextProvider.enabled()) {
                providers.add(aliyunOcrTextProvider);
            }
            if (bailianProvider.enabled()) {
                providers.add(bailianProvider);
            }
            return providers;
        }
        return providers;
    }

    public boolean bailianEnabled() {
        return bailianProvider.enabled()
                || qwenMtImageProvider.enabled()
                || aliyunTranslateImageProvider.enabled()
                || aliyunOcrTextProvider.enabled();
    }
}
