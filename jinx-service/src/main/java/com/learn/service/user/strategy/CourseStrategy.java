package com.learn.service.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * 课程学习任务策略实现
 * 处理普通课程和系列课程
 *
 * @author yujintao
 * @date 2023/5/10
 */
@Slf4j
@Component
public class CourseStrategy extends AbstractLearningTaskStrategy {

    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private CoursesMapper coursesMapper;

    @Override
    protected UserLearningTask doProcessLearningTask(Long userId, RecordLearningProgressRequest request) {
        log.info("处理课程学习任务: userId={}, contentType={}, contentId={}", 
                userId, request.getParentType(), request.getContentId());
        
        // 获取当前内容ID和类型
        Long contentId = request.getContentId().longValue();
        String contentType = request.getParentType();
        Date now = new Date();
        
        // 查询或创建课程学习任务
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, contentType)
                .eq(UserLearningTask::getBizId, contentId)
                .eq(UserLearningTask::getIsDel, 0);
        
        UserLearningTask courseTask = userLearningTaskMapper.selectOne(queryWrapper);
        
        if (courseTask == null) {
            // 创建课程学习任务
            courseTask = new UserLearningTask();
            courseTask.setUserId(userId);
            courseTask.setBizType(contentType);
            courseTask.setBizId(contentId);
            courseTask.setSource("SELF"); // 默认为自学
            courseTask.setStatus(LearningStatus.LEARNING); // 学习中
            courseTask.setStudyDuration(0);
            courseTask.setProgress(0);
            courseTask.setEarnedCredit(0);
            courseTask.setCertificateIssued(0);
            courseTask.setGmtCreate(now);
            courseTask.setGmtModified(now);
            courseTask.setStartTime(now); // 设置开始学习时间
            courseTask.setLastStudyTime(now); // 设置最后学习时间
            courseTask.setIsDel(0);

            userLearningTaskMapper.insert(courseTask);
            log.info("创建课程学习任务成功: userId={}, contentType={}, contentId={}", userId, contentType, contentId);
        } else if (courseTask.getStatus() == LearningStatus.NOT_STARTED) {
            // 如果状态为未开始，更新为学习中
            courseTask.setStatus(LearningStatus.LEARNING); // 学习中
            courseTask.setStartTime(now); // 设置开始学习时间P
            courseTask.setLastStudyTime(now); // 设置最后学习时间
            courseTask.setGmtModified(now);
            
            userLearningTaskMapper.updateById(courseTask);
            log.info("更新课程学习任务状态为学习中: userId={}, contentType={}, contentId={}", userId, contentType, contentId);
        }

        // 如果是系列课，需要处理系列课关联的课程
        Courses courses = coursesMapper.selectById(contentId);
        if ("series".equals(courses.getType())) {
            processSeriesCourseRelations(userId, contentId, courseTask);
        }
        
        return courseTask;
    }
    
    /**
     * 更新学习进度
     * 
     * @param learningTask 学习任务
     * @param request 学习进度请求
     */
    @Override
    @Transactional
    public void updateLearningProgress(UserLearningTask learningTask, RecordLearningProgressRequest request) {
        if (learningTask == null) {
            log.error("更新学习进度失败，学习任务为空");
            throw new IllegalArgumentException("学习任务不能为空");
        }
        
        if (request == null) {
            log.error("更新学习进度失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        // 根据内容类型处理进度
        setStudyProgress(learningTask, request);
        
        // 更新学习任务
        userLearningTaskMapper.updateById(learningTask);
        log.info("更新课程学习任务进度成功: userId={}, bizType={}, bizId={}, progress={}", 
                learningTask.getUserId(), learningTask.getBizType(), learningTask.getBizId(), learningTask.getProgress());
        
        // 如果任务完成，更新父节点进度
        if (learningTask.isCompleted() && learningTask.getParentId() != null) {
            updateParentProgress(learningTask.getUserId(), learningTask.getParentType(), learningTask.getParentId());
        }
    }
    
    /**
     * 更新视频学习进度
     *
     * @param learningTask 学习任务
     * @param request 学习进度请求
     */
    private void updateVideoProgress(UserLearningTask learningTask, RecordLearningProgressRequest request) {
        // 查询课程信息，获取视频总时长
        if (BizType.COURSE.equals(learningTask.getBizType())) {
            Courses course = coursesMapper.selectById(learningTask.getBizId());
            if (course != null && course.getAttribute(AttributeKey.DURATION, Integer.class) != null) {
                // 计算进度
                int totalDuration = course.getAttribute(AttributeKey.DURATION, Integer.class);

                // 计算进度百分比，最大为100%
                int progress = (int) ((double) request.getDuration() / totalDuration * 100);
                progress = Math.min(progress, 100);

                // 更新进度
                learningTask.setStudyDuration(request.getDuration());
                learningTask.setProgress(progress);
            } else {
                // 如果没有视频时长信息，使用请求中的进度
                if (request.getProgress() != null && request.getProgress() > learningTask.getProgress()) {
                    learningTask.setProgress(request.getProgress());
                }
            }
        } else {
            // 如果不是课程类型，使用请求中的进度
            if (request.getProgress() != null && request.getProgress() > learningTask.getProgress()) {
                learningTask.setProgress(request.getProgress());
            }
        }
    }
    
    /**
     * 处理课程完成时的逻辑
     * 课程和系列课没有证书，不需要处理
     *
     * @param learningTask 学习任务
     */
    @Override
    protected void handleCourseCompletion(UserLearningTask learningTask) {
        // 课程没有证书，不需要处理
        log.info("课程没有证书，不需要处理: userId={}, bizType={}, bizId={}", 
                learningTask.getUserId(), learningTask.getBizType(), learningTask.getBizId());
    }
    
    /**
     * 处理系列课关联的课程
     *
     * @param userId 用户ID
     * @param seriesCourseId 系列课ID
     * @param parentTask 父任务
     */
    private void processSeriesCourseRelations(Long userId, Long seriesCourseId, UserLearningTask parentTask) {
        // 查询系列课关联的所有课程
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ContentRelation::getBizType, BizType.COURSE)
                .eq(ContentRelation::getBizId, seriesCourseId)
                .eq(ContentRelation::getIsDel, 0);
        
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(relationQueryWrapper);
        if (CollectionUtils.isEmpty(contentRelations)) {
            log.info("系列课没有关联课程: seriesCourseId={}", seriesCourseId);
            return;
        }
        
        Date now = new Date();
        
        // 为每个关联的课程创建学习记录
        for (ContentRelation relation : contentRelations) {
            // 检查是否已存在学习记录
            LambdaQueryWrapper<UserLearningTask> subTaskQueryWrapper = new LambdaQueryWrapper<>();
            subTaskQueryWrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, relation.getContentType())
                    .eq(UserLearningTask::getBizId, relation.getContentId())
                    .eq(UserLearningTask::getParentId, parentTask.getBizId())
                    .eq(UserLearningTask::getIsDel, 0);
            
            UserLearningTask existingSubTask = userLearningTaskMapper.selectOne(subTaskQueryWrapper);
            
            if (existingSubTask == null) {
                // 创建子任务学习记录
                UserLearningTask subTask = new UserLearningTask();
                subTask.setUserId(userId);
                subTask.setBizType(relation.getContentType());
                subTask.setBizId(relation.getContentId());
                subTask.setParentId(parentTask.getBizId());
                subTask.setParentType(BizType.COURSE);
                subTask.setSource(parentTask.getSource());
                subTask.setStatus(LearningStatus.NOT_STARTED); // 未开始
                subTask.setStudyDuration(0);
                subTask.setProgress(0);
                subTask.setEarnedCredit(0);
                subTask.setCertificateIssued(0);
                subTask.setGmtCreate(now);
                subTask.setGmtModified(now);
                subTask.setIsDel(0);
                
                userLearningTaskMapper.insert(subTask);
                log.info("创建系列课子任务学习记录成功: userId={}, contentType={}, contentId={}", 
                        userId, relation.getContentType(), relation.getContentId());
            }
        }
    }
}
