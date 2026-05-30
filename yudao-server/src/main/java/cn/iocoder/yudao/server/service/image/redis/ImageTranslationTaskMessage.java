package cn.iocoder.yudao.server.service.image.redis;

import cn.iocoder.yudao.framework.mq.redis.core.stream.AbstractRedisStreamMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImageTranslationTaskMessage extends AbstractRedisStreamMessage {

    private Long taskId;
    private String taskNo;

    @Override
    public String getStreamKey() {
        return "image_translation_task";
    }
}
