package com.learn.service.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yujintao
 * @date 2025/5/9
 */
@Slf4j
public class CommonStrategy extends AbstractLearningTaskStrategy {

    @Override
    protected UserLearningTask doProcessLearningTask(Long userId, RecordLearningProgressRequest request) {
        log.info("处理学习任务: userId={}, contentType={}, contentId={}, parentId={}",
                userId, request.getParentType(), request.getContentId(), request.getParentId());
        // 查询或创建培训学习任务
        UserLearningTask trainTask = getLearningTask(userId, request.getContentType(), request.getContentId());
        if (Objects.nonNull(trainTask)) {
            return trainTask;
        }

        trainTask = new UserLearningTask();
        trainTask.setUserId(userId);
        trainTask.setBizType(request.getContentType());
        trainTask.setBizId(request.getContentId());

        // 设置指派信息
        setAssignInfo(userId, request.getParentType(), request.getContentId(), trainTask);

        trainTask.setStatus(LearningStatus.LEARNING); // 学习中
        trainTask.setStudyDuration(0);
        trainTask.setProgress(0);
        trainTask.setEarnedCredit(0);
        trainTask.setCertificateIssued(0);
        Date now = new Date();
        trainTask.setGmtCreate(now);
        trainTask.setGmtModified(now);
        trainTask.setStartTime(now); // 设置开始学习时间
        trainTask.setLastStudyTime(now); // 设置最后学习时间
        trainTask.setIsDel(0);

        // 设置培训类型的扩展属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("completedCourseCount", 0); // 已完成课程数
        attributes.put("completedRequiredCourseCount", 0); // 已完成必修课程数
        attributes.put("completedOptionalCourseCount", 0); // 已完成选修课程数
        trainTask.setAttributes(Json.toJson(attributes));
        trainTask.setSearchKey(trainTask.buildUserSearchKey());
        userLearningTaskMapper.insert(trainTask);
        log.info("创建培训学习任务成功: userId={}, trainId={}", userId, request.getContentId());
        return trainTask;
    }


    @Override
    protected void handleCourseCompletion(UserLearningTask learningTask) {

    }


    private UserLearningTask getLearningTask(Long userId, String bizType, Long bizId) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, bizType)
                .eq(UserLearningTask::getBizId, bizId)
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectOne(queryWrapper);

    }

}
