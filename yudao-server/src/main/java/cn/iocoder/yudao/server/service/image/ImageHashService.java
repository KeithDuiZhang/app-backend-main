package cn.iocoder.yudao.server.service.image;

import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class ImageHashService {

    @Resource
    private ImageTranslationProperties properties;

    public String rawSha256(byte[] bytes) {
        return sha256Hex(bytes == null ? new byte[0] : bytes);
    }

    public String cacheKey(String rawSha256, String sourceLang, String targetLang, String provider) {
        String value = rawSha256 + ":"
                + normalize(sourceLang) + ":"
                + normalize(targetLang) + ":"
                + normalize(provider) + ":"
                + properties.getPipelineVersion() + ":"
                + properties.getPreprocessVersion() + ":"
                + properties.getRenderVersion();
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    public String digest(String value) {
        return sha256Hex((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(result.length * 2);
            for (byte value : result) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
