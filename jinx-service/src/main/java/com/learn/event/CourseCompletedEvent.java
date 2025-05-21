package com.learn.event;

import com.alibaba.mos.boot.event.annotation.MosBootEvent;
import com.alibaba.mos.boot.event.model.MosBootEventType;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yujintao
 * @date 2025/5/17
 */
@Data
@Accessors(chain = true)
@MosBootEvent(type = MosBootEventType.REMOTE, topic = "USER_LEARN_EVENT", tags = "COURSE_COMPLETED")
public class CourseCompletedEvent {
    private Long userId;

    private Long contentId;

    private String contentType;

}
