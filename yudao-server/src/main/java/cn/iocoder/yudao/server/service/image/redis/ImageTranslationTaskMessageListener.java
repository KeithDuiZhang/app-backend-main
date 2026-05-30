package cn.iocoder.yudao.server.service.image.redis;

import cn.iocoder.yudao.framework.mq.redis.core.stream.AbstractRedisStreamMessageListener;
import cn.iocoder.yudao.server.service.image.ImageTranslationTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "image-translation", name = "queue-type", havingValue = "redis", matchIfMissing = true)
public class ImageTranslationTaskMessageListener extends AbstractRedisStreamMessageListener<ImageTranslationTaskMessage> {

    @Resource
    private ImageTranslationTaskService taskService;

    @Override
    public void onMessage(ImageTranslationTaskMessage message) {
        if (message == null || message.getTaskId() == null) {
            return;
        }
        taskService.processQueuedTask(message.getTaskId());
    }
}
