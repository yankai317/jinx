package com.learn.event.listener;

import com.alibaba.mos.boot.data.MosBootSingleResult;
import com.alibaba.mos.boot.event.annotation.MosBootEventListener;
import com.alibaba.mos.boot.event.annotation.MosBootEventSubscribe;
import com.alibaba.mos.boot.event.common.MosBootEventContext;
import com.alibaba.mos.boot.event.model.MosBootEventType;
import com.alibaba.mos.boot.kv.ability.MosBootLockAbility;
import com.alibaba.mos.boot.util.MosBootJsonUtils;
import com.alibaba.mos.boot.util.MosBootSpiLoader;
import com.google.common.collect.Lists;
import com.learn.event.CourseCompletedEvent;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY;
import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY_EXPIRE_TIME;

/**
 * @author yujintao
 * @date 2025/5/17
 */
@Slf4j
@Component
@MosBootEventListener(type = MosBootEventType.REMOTE)
public class CourseCompletedEventListener {
    @Autowired
    private UserStudyService userStudyService;


    @MosBootEventSubscribe(retryTimes = 3)
    public boolean create(CourseCompletedEvent event, MosBootEventContext context) {
        log.info("消息接收成功: {}", MosBootJsonUtils.json(event));
        if (null == event.getUserId()) {
            log.error("用户id为空");
            return true;
        }
        if (null == event.getContentId() || StringUtils.isBlank(event.getContentType())) {
            log.error("内容id或类型为空");
            return true;
        }

        userStudyService.processCompletionTasks(event.getUserId(), event.getContentType(), event.getContentId());
        return true;
    }

}
