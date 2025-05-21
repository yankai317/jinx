package com.learn.service.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.user.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 学习任务策略抽象基类
 * 提供通用方法实现
 *
 * @author yujintao
 * @date 2023/5/10
 */
@Slf4j
public abstract class AbstractLearningTaskStrategy implements LearningTaskStrategy {

    @Autowired
    protected UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    protected AssignmentDetailMapper assignmentDetailMapper;

    @Autowired
    protected ContentRelationMapper contentRelationMapper;
    @Autowired
    protected CoursesMapper coursesMapper;

    @Autowired
    protected CertificateMapper certificateMapper;

    @Autowired
    protected LearningMapMapper learningMapMapper;

    @Autowired
    protected LearningMapStageMapper learningMapStageMapper;

    @Autowired
    protected TrainMapper trainMapper;
    @Autowired
    protected UserCertificateMapper userCertificateMapper;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 处理学习任务记录
     *
     * @param userId  用户ID
     * @param request 学习进度请求
     * @return 用户学习任务记录
     */
    @Override
    public UserLearningTask processLearningTask(Long userId, RecordLearningProgressRequest request) {
        validateRequest(userId, request);
        return doProcessLearningTask(userId, request);
    }

    /**
     * 更新学习进度
     *
     * @param learningTask 学习任务
     * @param request      学习进度请求
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

        // 更新当前任务进度
        Date now = new Date();
        // 更新学习时长
        if (request.getDuration() != null && request.getDuration() > 0) {
            int currentDuration = learningTask.getStudyDuration() != null ? learningTask.getStudyDuration() : 0;
            learningTask.setStudyDuration(currentDuration + request.getDuration());
        }

        // 更新学习进度
        if (request.getProgress() != null) {
            // 如果新进度大于当前进度，则更新
            if (request.getProgress() > learningTask.getProgress()) {
                learningTask.setProgress(request.getProgress());
            }
        }

        // 更新最后学习时间
        learningTask.setLastStudyTime(now);
        learningTask.setGmtModified(now);

        // 检查是否完成学习
        boolean isCompleted = false;
        if (learningTask.getProgress() >= 100) {
            // 如果进度达到100%，标记为已完成
            if (LearningStatus.COMPLETED.equals(learningTask.getStatus())) {
                learningTask.setStatus(LearningStatus.COMPLETED); // 已完成
                learningTask.setCompletionTime(now);
                isCompleted = true;
            }
        } else if (LearningStatus.NOT_STARTED.equals(learningTask.getStatus())) {
            // 如果状态为未开始，更新为学习中
            learningTask.setStatus(LearningStatus.LEARNING); // 学习中
            learningTask.setStartTime(now);
        }

        // 更新学习任务
        userLearningTaskMapper.updateById(learningTask);
        log.info("更新学习任务进度成功: userId={}, bizType={}, bizId={}, progress={}",
                learningTask.getUserId(), learningTask.getBizType(), learningTask.getBizId(), learningTask.getProgress());

        // 如果任务完成，处理课程完成逻辑
        if (isCompleted) {
            // 处理课程完成时的证书和学分
            handleCourseCompletion(learningTask);

            // 更新父节点进度
            if (learningTask.getParentId() != null) {
                updateParentProgress(learningTask.getUserId(), learningTask.getParentType(), learningTask.getParentId());
            }
        }
    }

    /**
     * 处理课程完成时的逻辑
     * 如果进度100%，根据类型和id查询对应的课程/地图/地图阶段/培训
     * 如果关联了证书，将证书id记录到学习记录中
     * 如果有学分，也要将学分记录到学习记录中
     *
     * @param learningTask 学习任务
     */
    protected abstract void handleCourseCompletion(UserLearningTask learningTask);

    /**
     * 更新父节点进度
     *
     * @param userId     用户ID
     * @param parentType 父节点类型
     * @param parentId   父节点ID
     */
    protected void updateParentProgress(Long userId, String parentType, Long parentId) {
        // 查询父节点任务
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, parentType)
                .eq(UserLearningTask::getBizId, parentId)
                .eq(UserLearningTask::getIsDel, 0);

        UserLearningTask parentTask = userLearningTaskMapper.selectOne(queryWrapper);

        if (parentTask == null) {
            log.warn("更新父节点进度失败，父节点任务不存在: userId={}, parentType={}, parentId={}",
                    userId, parentType, parentId);
            return;
        }

        // 查询父节点关联的所有子任务
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ContentRelation::getBizType, parentType)
                .eq(ContentRelation::getBizId, parentId)
                .eq(ContentRelation::getIsDel, 0);

        List<ContentRelation> contentRelations = contentRelationMapper.selectList(relationQueryWrapper);

        if (contentRelations == null || contentRelations.isEmpty()) {
            log.info("父节点没有关联内容，无需更新进度: userId={}, parentType={}, parentId={}",
                    userId, parentType, parentId);
            return;
        }

        // 查询所有子任务的完成情况
        int totalTasks = contentRelations.size();
        int completedTasks = 0;
        int totalRequiredTasks = 0;
        int completedRequiredTasks = 0;

        for (ContentRelation relation : contentRelations) {
            // 查询子任务
            LambdaQueryWrapper<UserLearningTask> subTaskQueryWrapper = new LambdaQueryWrapper<>();
            subTaskQueryWrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, relation.getContentType())
                    .eq(UserLearningTask::getBizId, relation.getContentId())
                    .eq(UserLearningTask::getParentId, parentId)
                    .eq(UserLearningTask::getIsDel, 0);

            UserLearningTask subTask = userLearningTaskMapper.selectOne(subTaskQueryWrapper);

            // 如果是必修任务
            if (relation.getIsRequired() != null && relation.getIsRequired() == 1) {
                totalRequiredTasks++;
                if (subTask != null &&  LearningStatus.COMPLETED.equals(subTask.getStatus())) {
                    completedRequiredTasks++;
                }
            }

            // 统计已完成的任务
            if (subTask != null && LearningStatus.COMPLETED.equals(subTask.getStatus())) {
                completedTasks++;
            }
        }

        // 计算进度
        int progress = 0;
        if (totalTasks > 0) {
            progress = (int) ((double) completedTasks / totalTasks * 100);
        }

        // 更新父节点进度
        Date now = new Date();
        parentTask.setProgress(progress);
        parentTask.setGmtModified(now);

        // 如果所有必修任务都已完成，且进度达到100%，则标记为已完成
        boolean isCompleted = false;
        if (progress >= 100 && (totalRequiredTasks == 0 || completedRequiredTasks == totalRequiredTasks)) {
            if (parentTask.getStatus() != LearningStatus.COMPLETED) {
                parentTask.setStatus(LearningStatus.COMPLETED); // 已完成
                parentTask.setCompletionTime(now);
                isCompleted = true;
            }
        }

        // 更新父节点任务
        userLearningTaskMapper.updateById(parentTask);
        log.info("更新父节点进度成功: userId={}, parentType={}, parentId={}, progress={}",
                userId, parentType, parentId, progress);

        // 如果父节点任务完成，且有上级父节点，则继续更新上级父节点进度
        if (isCompleted && parentTask.getParentId() != null) {
            updateParentProgress(userId, parentTask.getParentType(), parentTask.getParentId());
        }
    }

    /**
     * 验证请求参数
     *
     * @param userId  用户ID
     * @param request 学习进度请求
     */
    protected void validateRequest(Long userId, RecordLearningProgressRequest request) {
        if (userId == null) {
            log.error("获取或创建学习任务记录失败，用户ID为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (request == null) {
            log.error("获取或创建学习任务记录失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.getContentId() == null) {
            log.error("获取或创建学习任务记录失败，内容ID为空");
            throw new IllegalArgumentException("内容ID不能为空");
        }
    }

    /**
     * 实际处理学习任务记录的方法，由子类实现
     *
     * @param userId  用户ID
     * @param request 学习进度请求
     * @return 用户学习任务记录
     */
    protected abstract UserLearningTask doProcessLearningTask(Long userId, RecordLearningProgressRequest request);

    /**
     * 判断学习进度
     *
     * @return 计算后的进度
     */
    protected void setStudyProgress(UserLearningTask learningTask, RecordLearningProgressRequest request) {
        String bizType = learningTask.getBizType();
        Long bizId = learningTask.getBizId();
        Long parentId = learningTask.getParentId();
        String parentType = learningTask.getParentType();
        Integer duration = learningTask.getStudyDuration();

        // 附件类型学习进度
        String courseType;
        int totalDuration;
        if (BizType.APPENDIX_FILE.equals(bizType)) {
            LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ContentRelation::getContentId, bizId)
                    .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                    .eq(ContentRelation::getBizId, parentId)
                    .eq(ContentRelation::getBizType, parentType)
                    .eq(ContentRelation::getIsDel, 0);
            ContentRelation relation = contentRelationMapper.selectOne(queryWrapper);
            if (Objects.isNull(relation)) {
                return;
            }
            courseType = relation.getAttribute(AttributeKey.TYPE, String.class);
            totalDuration = relation.getAttribute(AttributeKey.DURATION, Integer.class);
        } else if (BizType.COURSE.equals(bizType)) {
            Courses course = coursesMapper.selectById(bizId);
            courseType = Objects.isNull(course) ? "" : course.getType();
            totalDuration = course.getAttribute(AttributeKey.DURATION, Integer.class);
        } else {
            return;
        }


        if ("video".equals(courseType)) {
            int progress = (int) ((double) duration / totalDuration);
            progress = Math.min(progress, 100);
            learningTask.setProgress(progress);
            learningTask.setStudyDuration(request.getDuration());
        }

        if ("document".equals(courseType) || "article".equals(courseType)) {
            learningTask.setProgress(request.getProgress());
            learningTask.setStudyDuration(learningTask.getStudyDuration() + request.getDuration());
        }

        // 设置学习时间
        Date now = new Date();
        // 更新最后学习时间
        learningTask.setLastStudyTime(now);
        learningTask.setGmtModified(now);
        // 检查是否完成学习
        if (learningTask.getProgress() >= 100) {
            // 如果进度达到100%，标记为已完成
            learningTask.setStatus(LearningStatus.COMPLETED); // 已完成
            learningTask.setCompletionTime(now);
        } else if (learningTask.getStatus() == LearningStatus.NOT_STARTED) {
            // 如果状态为未开始，更新为学习中
            learningTask.setStatus(LearningStatus.LEARNING); // 学习中
            learningTask.setStartTime(now);
        }
    }

    /**
     * 检查是否为指派任务
     *
     * @param userId  用户ID
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 来源类型，"ASSIGN"表示指派，"SELF"表示自学
     */
    protected void setAssignInfo(Long userId, String bizType, Long bizId, UserLearningTask trainTask) {
        LambdaQueryWrapper<AssignmentDetail> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.eq(AssignmentDetail::getType, bizType)
                .eq(AssignmentDetail::getTypeId, bizId)
                .eq(AssignmentDetail::getStatus, 1)
                .eq(AssignmentDetail::getUserid, userId)
                .eq(AssignmentDetail::getIsDel, 0);

        AssignmentDetail assignmentDetail = assignmentDetailMapper.selectOne(assignWrapper);
        if (assignmentDetail != null) {
            trainTask.setAssignTime(assignmentDetail.getGmtCreate());
            trainTask.setSource("ASSIGN");
        } else {
            trainTask.setSource("SELF");
        }
    }
}
