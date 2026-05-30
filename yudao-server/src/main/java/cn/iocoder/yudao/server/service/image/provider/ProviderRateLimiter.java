package cn.iocoder.yudao.server.service.image.provider;

import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderRateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class ProviderRateLimiter {

    private static final long MAX_SLEEP_MS = 1000L;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void acquire(String provider, ProviderRateLimit limit) {
        long intervalMillis = limit != null && limit.getIntervalMillis() > 0 ? limit.getIntervalMillis() : 1000L;
        if (redisTemplate == null) {
            sleep(intervalMillis);
            return;
        }
        String key = "image_translation:rate_limit:" + provider;
        while (!Thread.currentThread().isInterrupted()) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofMillis(intervalMillis));
            if (Boolean.TRUE.equals(acquired)) {
                return;
            }
            sleep(Math.min(intervalMillis, MAX_SLEEP_MS));
        }
        Thread.currentThread().interrupt();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
