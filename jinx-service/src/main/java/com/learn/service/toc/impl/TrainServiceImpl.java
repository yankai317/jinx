package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.train.TrainDetailResponse;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.toc.ContentService;
import com.learn.service.toc.TrainService;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * C端培训服务实现类
 */
@Service
@Slf4j
public class TrainServiceImpl implements TrainService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private UserStudyService userStudyService;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ContentService contentService;

    /**
     * 获取培训详情
     *
     * @param id     培训ID
     * @param userId 当前用户ID
     * @return 培训详情
     */
    @Override
    public TrainDetailResponse getTrainDetail(Long id, Long userId) {
        log.info("获取培训详情，培训ID：{}，用户ID：{}", id, userId);

        // 1. 获取培训基本信息
        Train train = trainMapper.selectById(id);
        if (train == null || train.getIsDel() == 1) {
            log.error("培训不存在或已删除，培训ID：{}", id);
            return null;
        }

        // 2. 构建培训详情响应
        TrainDetailResponse response = new TrainDetailResponse();
        response.setId(train.getId());
        response.setName(train.getName());
        response.setCover(train.getCover());
        response.setIntroduction(train.getIntroduction());
        response.setCredit(train.getCredit());

        // 3. 获取证书信息
        setCertificateInfo(response, train);

        // 4. 获取培训内容列表
        setContentInfo(response, train.getId());

        // 5. 获取用户学习进度
        setUserProgress(response, train.getId(), userId);

        // 6. 获取学习人员信息
        setLearnerInfo(response, train.getId());

        return response;
    }

    /**
     * 设置证书信息
     *
     * @param response 响应对象
     * @param train    培训实体
     */
    private void setCertificateInfo(TrainDetailResponse response, Train train) {
        long start = System.currentTimeMillis();
        if (train.getCertificateId() != null) {
            Certificate certificate = certificateMapper.selectById(train.getCertificateId());
            if (certificate != null && certificate.getIsDel() == 0) {
                TrainDetailResponse.CertificateInfo certificateInfo = new TrainDetailResponse.CertificateInfo();
                certificateInfo.setId(certificate.getId());
                certificateInfo.setName(certificate.getName());
                response.setCertificate(certificateInfo);
            }
        }
        System.out.println("setCertificateInfo:耗时:" + (System.currentTimeMillis() - start));
    }

    /**
     * 设置用户学习进度
     *
     * @param response 响应对象
     * @param trainId  培训ID
     * @param userId   用户ID
     */
    private void setUserProgress(TrainDetailResponse response, Long trainId, Long userId) {
        long start = System.currentTimeMillis();
        // 没有关联课程
        if (CollectionUtils.isEmpty(response.getContents())) {
            log.warn("培训下没有关联,培训ID：{}", trainId);
            return;
        }
        // 获取所有内容ID
        List<String> contentSearchKeys = response.getContents().stream()
                .map(v -> v.getContentType() + "_" + v.getContentId())
                .toList();
        // 查询培训的学习任务记录
        RecordLearningProgressRequest request = new RecordLearningProgressRequest();
        request.setContentType(BizType.TRAIN);
        request.setContentId(trainId);
        UserLearningTask trainLearningTask = userStudyService.getLearningTask(userId, request);
        trainLearningTask = Objects.isNull(trainLearningTask) ? new UserLearningTask() : trainLearningTask;


        // 查询培训关联课程的学习记录
        List<String> userSearchKeys = contentSearchKeys.stream()
                .map(v -> v + "_" + userId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<UserLearningTask> courseTaskWrapper = new LambdaQueryWrapper<>();
        courseTaskWrapper.in(UserLearningTask::getSearchKey, userSearchKeys)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> courseTasks = userLearningTaskMapper.selectList(courseTaskWrapper);

        // 设置课程进度
        Map<Long, UserLearningTask> courseTaskMap = courseTasks.stream()
                .collect(Collectors.toMap(UserLearningTask::getBizId, c -> c));
        for (TrainDetailResponse.ContentItem item : response.getContents()) {
            UserLearningTask courseTask = courseTaskMap.get(item.getContentId());
            if (courseTask != null) {
                item.setProgress(courseTask.getProgress());
                item.setStatus(courseTask.convertStatus(courseTask.getProgress()));
            } else {
                item.setProgress(0);
                item.setStatus(LearningStatus.NOT_STARTED);
            }
        }

        // 统计已完成的课程数和总学习时长
        int totalStudyDuration = courseTasks.stream()
                .filter(task -> task.getStudyDuration() != null)
                .mapToInt(UserLearningTask::getStudyDuration)
                .sum();
        // 计算必修课程总数和已完成数
        int requiredTaskTotal = (int) response.getContents().stream()
                .filter(relation -> relation.getIsRequired() == 1)
                .count();
        // 已完成的必修课程数
        int requiredTaskFinished = (int) courseTasks.stream()
                .filter(task -> LearningStatus.COMPLETED.equals(task.getStatus()))
                .count();
        
        // 设置用户进度信息
        TrainDetailResponse.UserProgress userProgress = new TrainDetailResponse.UserProgress();
        
        // 计算进度百分比 - 已完成的课程 / 总课程
        int progress = 0;
        if (requiredTaskTotal > 0) {
            progress = (int) (((double) requiredTaskFinished / requiredTaskTotal) * 100);
        }
        
        // 设置学习状态
        userProgress.setStatus(trainLearningTask.convertStatus(progress));
        userProgress.setProgress(progress);
        userProgress.setStudyDuration(totalStudyDuration);
        userProgress.setRequiredTaskFinished(requiredTaskFinished);
        userProgress.setRequiredTaskTotal(requiredTaskTotal);
        
        response.setUserProgress(userProgress);
        System.out.println("setUserProgress:耗时:" + (System.currentTimeMillis() - start));
    }

    /**
     * 设置培训内容列表
     *
     * @param response 响应对象
     * @param trainId  培训ID
     */
    private void setContentInfo(TrainDetailResponse response, Long trainId) {
        long start = System.currentTimeMillis();
        // 1. 查询培训内容关联
        LambdaQueryWrapper<ContentRelation> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.eq(ContentRelation::getBizId, trainId)
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getContentType, BizType.COURSE)
                .eq(ContentRelation::getIsDel, 0)
                .orderByAsc(ContentRelation::getSortOrder);
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(contentWrapper);
        if (CollectionUtils.isEmpty(contentRelations)) {
            response.setContents(Collections.emptyList());
            return;
        }

        // 2. 获取所有内容ID
        List<Long> courseIds = contentRelations.stream()
                .map(ContentRelation::getContentId)
                .collect(Collectors.toList());
        // 批量查询课程信息
        LambdaQueryWrapper<Courses> coursesWrapper = new LambdaQueryWrapper<>();
        coursesWrapper.in(Courses::getId, courseIds).eq(Courses::getIsDel, 0);
        List<Courses> coursesList = coursesMapper.selectList(coursesWrapper);
        Map<Long, Courses> coursesMap = coursesList.stream().collect(Collectors.toMap(Courses::getId, c -> c));

        // 转换为DTO
        List<TrainDetailResponse.ContentItem> contentItems = new ArrayList<>();
        for (ContentRelation relation : contentRelations) {
            Courses course = coursesMap.get(relation.getContentId());
            if (null == course) {
                continue;
            }
            TrainDetailResponse.ContentItem item = new TrainDetailResponse.ContentItem();
            item.setId(relation.getId());
            item.setContentType(relation.getContentType());
            item.setContentId(relation.getContentId());
            item.setIsRequired(relation.getIsRequired());
            item.setTitle(course.getTitle());
            item.setType(course.getType());
            item.setCoverImage(course.getCoverImage());
            contentItems.add(item);
        }

        response.setContents(contentItems);
        System.out.println("setContentInfo:耗时:" + (System.currentTimeMillis() - start));
    }

    /**
     * 设置学习人员信息
     *
     * @param response 响应对象
     * @param trainId  培训ID
     */
    private void setLearnerInfo(TrainDetailResponse response, Long trainId) {
        long start = System.currentTimeMillis();
        // 查询学习任务记录
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getBizId, trainId)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> tasks = userLearningTaskMapper.selectList(taskWrapper);

        TrainDetailResponse.LearnerInfo learnerInfo = new TrainDetailResponse.LearnerInfo();
        if (CollectionUtils.isEmpty(tasks)) {
            learnerInfo.setTotal(0);
            learnerInfo.setCompleted(0);
            learnerInfo.setLearning(0);
            learnerInfo.setNotStart(0);
            learnerInfo.setCompletedList(Collections.emptyList());
            learnerInfo.setLearningList(Collections.emptyList());
            learnerInfo.setNotStartList(Collections.emptyList());
            response.setLearners(learnerInfo);
            return;
        }

        // 统计总人数和各状态人数
        int total = tasks.size();
        
        // 按状态分组用户
        List<UserLearningTask> completedUsers = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equals(LearningStatus.COMPLETED))
                .collect(Collectors.toList());
        
        List<UserLearningTask> learningUsers = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equals(LearningStatus.LEARNING))
                .collect(Collectors.toList());
        
        List<UserLearningTask> notStartUsers = tasks.stream()
                .filter(t -> t.getStatus() == null || t.getStatus().equals(LearningStatus.NOT_STARTED))
                .collect(Collectors.toList());
        
        int completed = completedUsers.size();
        int learning = learningUsers.size();
        int notStart = notStartUsers.size();

        learnerInfo.setTotal(total);
        learnerInfo.setCompleted(completed);
        learnerInfo.setLearning(learning);
        learnerInfo.setNotStart(notStart);

        // 获取所有用户ID
        List<Long> userIds = tasks.stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userIds)) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(User::getUserId, userIds).eq(User::getIsDel, 0);
            List<User> users = userMapper.selectList(userWrapper);
            userMap = users.stream().collect(Collectors.toMap(User::getUserId, u -> u, (v1, v2) -> v1));
        }

        // 构建已完成学习人员列表（最多显示20个）
        List<TrainDetailResponse.LearnerItem> completedItems = buildLearnerItems(completedUsers, userMap, 20, "completed");
        learnerInfo.setCompletedList(completedItems);
        
        // 构建学习中人员列表（最多显示20个）
        List<TrainDetailResponse.LearnerItem> learningItems = buildLearnerItems(learningUsers, userMap, 20, "learning");
        learnerInfo.setLearningList(learningItems);
        
        // 构建未开始人员列表（最多显示20个）
        List<TrainDetailResponse.LearnerItem> notStartItems = buildLearnerItems(notStartUsers, userMap, 20, "not_started");
        learnerInfo.setNotStartList(notStartItems);

        response.setLearners(learnerInfo);


        System.out.println("setLearnerInfo:耗时:" + (System.currentTimeMillis() - start));
    }
    
    /**
     * 构建学习人员列表项
     *
     * @param tasks 学习任务记录列表
     * @param userMap 用户信息映射
     * @param maxCount 最大显示数量
     * @param status 学习状态
     * @return 学习人员列表
     */
    private List<TrainDetailResponse.LearnerItem> buildLearnerItems(
            List<UserLearningTask> tasks, 
            Map<Long, User> userMap, 
            int maxCount,
            String status) {
        
        List<TrainDetailResponse.LearnerItem> items = new ArrayList<>();
        int count = 0;
        
        for (UserLearningTask task : tasks) {
            if (count >= maxCount) {
                break;
            }

            TrainDetailResponse.LearnerItem item = new TrainDetailResponse.LearnerItem();
            item.setUserId(task.getUserId());
            item.setStatus(status);

            // 设置用户信息
            User user = userMap.get(task.getUserId());
            if (user != null) {
                item.setNickname(user.getNickname());
                item.setAvatar(user.getAvatar());
            } else {
                item.setNickname(task.getCreatorName());
            }

            items.add(item);
            count++;
        }
        
        return items;
    }
}
