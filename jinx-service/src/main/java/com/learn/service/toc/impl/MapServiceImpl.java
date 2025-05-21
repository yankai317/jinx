package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.learn.common.dto.ContentBO;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.constants.StageStatus;
import com.learn.dto.toc.map.MapDetailResponse;
import com.learn.dto.toc.map.MapStageDetailResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.impl.CommonRangeInterfaceImpl;
import com.learn.service.toc.ContentService;
import com.learn.service.toc.MapService;
import com.learn.service.user.UserStudyService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * C端学习地图服务实现类
 */
@Service
@Slf4j
public class MapServiceImpl implements MapService {

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private CommonRangeInterfaceImpl commonRangeInterface;

    @Autowired
    private ContentService contentService;


    /**
     * 获取学习地图详情
     *
     * @param id     学习地图ID
     * @param userId 当前用户ID
     * @return 学习地图详情响应
     */
    @Override
    public MapDetailResponse getMapDetail(Long id, Long userId) {
        log.info("获取C端学习地图详情，地图ID：{}，用户ID：{}", id, userId);

        MapDetailResponse response = new MapDetailResponse();

        // 参数校验
        if (id == null || id <= 0) {
            response.setCode(400);
            response.setMessage("参数错误：学习地图ID无效");
            return response;
        }

        if (userId == null) {
            response.setCode(401);
            response.setMessage("未登录或登录已过期");
            return response;
        }
        checkUserHasPermission(id, userId);

        // 查询学习地图基本信息
        LearningMap learningMap = learningMapMapper.selectById(id);
        if (learningMap == null || learningMap.getIsDel() == 1) {
            response.setCode(404);
            response.setMessage("学习地图不存在或已删除");
            return response;
        }

        // 构建响应数据
        MapDetailResponse.MapDetailData data = buildMapDetailData(learningMap, userId);

        response.setCode(200);
        response.setMessage("获取成功");
        response.setData(data);

        return response;
    }

    /**
     * 获取学习地图阶段详情
     *
     * @param mapId   学习地图ID
     * @param stageId 阶段ID
     * @param userId  当前用户ID
     * @return 学习地图阶段详情响应
     */
    @Override
    public MapStageDetailResponse getMapStageDetail(Long mapId, Long stageId, Long userId) {
        log.info("获取C端学习地图阶段详情，地图ID：{}，阶段ID：{}，用户ID：{}", mapId, stageId, userId);

        MapStageDetailResponse response = new MapStageDetailResponse();

        try {
            // 参数校验
            if (mapId == null || mapId <= 0) {
                response.setCode(400);
                response.setMessage("参数错误：学习地图ID无效");
                return response;
            }

            if (stageId == null || stageId <= 0) {
                response.setCode(400);
                response.setMessage("参数错误：阶段ID无效");
                return response;
            }

            if (userId == null) {
                response.setCode(401);
                response.setMessage("未登录或登录已过期");
                return response;
            }
            checkUserHasPermission(mapId, userId);

            // 查询学习地图阶段信息
            LearningMapStage stage = learningMapStageMapper.selectById(stageId);
            if (stage == null || stage.getIsDel() == 1) {
                response.setCode(404);
                response.setMessage("学习地图阶段不存在或已删除");
                return response;
            }

            // 验证阶段是否属于指定的学习地图
            if (!stage.getMapId().equals(mapId)) {
                response.setCode(400);
                response.setMessage("参数错误：阶段不属于指定的学习地图");
                return response;
            }

            // 获取用户学习进度
            UserLearningTask userProgress = getUserProgress(mapId, userId);

            // 构建响应数据
            MapStageDetailResponse.StageDetailData data = buildStageDetailData(stage, userProgress, userId);

            response.setCode(200);
            response.setMessage("获取成功");
            response.setData(data);

        } catch (Exception e) {
            log.error("获取学习地图阶段详情异常", e);
            response.setCode(500);
            response.setMessage("服务器内部错误");
        }

        return response;
    }

    /**
     * 构建学习地图详情数据
     *
     * @param learningMap 学习地图实体
     * @param userId      用户ID
     * @return 学习地图详情数据
     */
    private MapDetailResponse.MapDetailData buildMapDetailData(LearningMap learningMap, Long userId) {
        MapDetailResponse.MapDetailData data = new MapDetailResponse.MapDetailData();

        // 设置基本信息
        data.setId(learningMap.getId());
        data.setName(learningMap.getName());
        data.setCover(learningMap.getCover());
        data.setIntroduction(learningMap.getIntroduction());
        data.setCreditRule(learningMap.getCreditRule());
        data.setRequiredCredit(learningMap.getRequiredCredit());
        data.setElectiveCredit(learningMap.getElectiveCredit());
        data.setCertificateRule(learningMap.getCertificateRule());
        data.setUnlockMode(learningMap.getUnlockMode());
        data.setTheme(learningMap.getTheme());

        // 设置证书信息
        if (learningMap.getCertificateId() != null && learningMap.getCertificateId() > 0) {
            Certificate certificate = certificateMapper.selectById(learningMap.getCertificateId());
            if (certificate != null && certificate.getIsDel() == 0) {
                MapDetailResponse.CertificateInfo certificateInfo = new MapDetailResponse.CertificateInfo();
                certificateInfo.setId(certificate.getId());
                certificateInfo.setName(certificate.getName());
                data.setCertificate(certificateInfo);
            }
        }

        // 获取用户学习进度
        UserLearningTask userProgress = getUserProgress(learningMap.getId(), userId);
        data.setUserProgress(buildUserProgressData(userProgress, learningMap.getId()));

        // 获取阶段列表
        List<LearningMapStage> stages = getStages(learningMap.getId());
        data.setStages(buildStagesData(stages, userId, data.getUserProgress()));

        // 获取学习人员信息
        data.setLearners(buildLearnersData(learningMap.getId()));

        return data;
    }

    /**
     * 构建阶段详情数据
     *
     * @param stage        阶段实体
     * @param userProgress 用户学习进度
     * @return 阶段详情数据
     */
    private MapStageDetailResponse.StageDetailData buildStageDetailData(LearningMapStage stage, UserLearningTask userProgress, Long userId) {
        MapStageDetailResponse.StageDetailData data = new MapStageDetailResponse.StageDetailData();

        // 设置基本信息
        data.setId(stage.getId());
        data.setMapId(stage.getMapId());
        data.setName(stage.getName());
        data.setStageOrder(stage.getStageOrder());
        data.setCredit(stage.getCredit());
        data.setOpenType(stage.getOpenType());

        // 获取阶段任务列表
        List<ContentRelation> taskRelations = getStageTaskRelations(stage.getId());

        // 获取用户任务学习记录
        List<String> searchKeys = taskRelations.stream()
                .map(v -> v.getContentType() + "_" + v.getContentId() + "_" + userId.toString())
                .collect(Collectors.toList());
        List<UserLearningTask> taskRecords = getStageTaskRecords(searchKeys);

        // 构建任务列表
        List<MapStageDetailResponse.TaskInfo> tasks = buildTaskInfoList(taskRelations, taskRecords);
        data.setTasks(tasks);

        // 构建用户学习进度
        MapStageDetailResponse.UserProgress progress = buildUserStageProgress(taskRecords, taskRelations.size());
        data.setUserProgress(progress);

        return data;
    }

    /**
     * 构建用户学习进度数据
     *
     * @param userProgress 用户学习进度实体
     * @return 用户学习进度数据
     */
    private MapDetailResponse.UserProgress buildUserProgressData(UserLearningTask userProgress, Long mapId) {
        MapDetailResponse.UserProgress progressData = new MapDetailResponse.UserProgress();

        if (userProgress == null) {
            // 用户未开始学习
            progressData.setStatus("learning");
            List<LearningMapStage> stages = getStages(mapId);
            progressData.setCurrentStageId(stages.getFirst().getId());
            progressData.setProgress(0);
            progressData.setStudyDuration(0);
            progressData.setCompletedStageCount(0);
            progressData.setEarnedCredit(BigDecimal.ZERO);
            return progressData;
        }

        // 获取所有阶段
        List<LearningMapStage> stages = getStages(mapId);
        if (stages.isEmpty()) {
            // 没有阶段，直接返回基本进度
            progressData.setStatus(userProgress.getStatus());
            progressData.setProgress(userProgress.getProgress() != null ? userProgress.getProgress() : 0);
            progressData.setStudyDuration(userProgress.getStudyDuration() != null ? userProgress.getStudyDuration() : 0);
            progressData.setCompletedStageCount(0);
            progressData.setEarnedCredit(userProgress.getEarnedCredit() != null ?
                    BigDecimal.valueOf(userProgress.getEarnedCredit()) : BigDecimal.ZERO);
            return progressData;
        }

        // 获取所有阶段ID
        List<String> stageSearchKeys = stages.stream()
                .map(v -> BizType.MAP_STAGE + "_" + v.getId() + "_" + userProgress.getUserId())
                .collect(Collectors.toList());

        LambdaQueryWrapper<UserLearningTask> stageQueryCondition = new LambdaQueryWrapper<>();
        stageQueryCondition.in(UserLearningTask::getSearchKey, stageSearchKeys);
        stageQueryCondition.eq(UserLearningTask::getIsDel, 0);
        // 查询所有内容的学习记录
        List<UserLearningTask> stageTasks = userLearningTaskMapper.selectList(stageQueryCondition);

        // 计算进度
        int totalTasks = stages.size();
        int totalStudyDuration = 0;
        int completedStageCount = 0;
        Long currentStageId = null;
        for (UserLearningTask stageTask : stageTasks) {
            if (stageTask.isCompleted()) {
                completedStageCount++;
            } else {
                currentStageId = currentStageId == null ? stageTask.getId() : currentStageId;
            }
            totalStudyDuration = totalStudyDuration + (null == stageTask.getStudyDuration() ? 0 : stageTask.getStudyDuration());
        }


        // 计算进度百分比 - 已完成的任务 / 总任务
        totalTasks = Math.max(totalTasks, 1);
        int progress = (int) (((double) completedStageCount / totalTasks) * 100);

        // 设置用户进度信息
        progressData.setStatus(userProgress.convertStatus(progress));
        progressData.setProgress(progress);
        progressData.setStudyDuration(totalStudyDuration);
        progressData.setCompletedStageCount(completedStageCount);
        progressData.setCurrentStageId(currentStageId);
        progressData.setEarnedCredit(userProgress.getEarnedCredit() != null ?
                BigDecimal.valueOf(userProgress.getEarnedCredit()) : BigDecimal.ZERO);

        return progressData;
    }

    /**
     * 构建阶段列表数据
     *
     * @param stages       阶段实体列表
     * @param userId       用户ID
     * @param userProgress 用户学习进度
     * @return 阶段列表数据
     */
    private List<MapDetailResponse.StageInfo> buildStagesData(List<LearningMapStage> stages, Long userId, MapDetailResponse.UserProgress userProgress) {
        if (stages == null || stages.isEmpty()) {
            return new ArrayList<>();
        }

        List<MapDetailResponse.StageInfo> stageInfoList = new ArrayList<>();

        // 学习进度
        List<Long> stageIds = stages.stream().map(LearningMapStage::getId).collect(Collectors.toList());
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE);
        queryWrapper.in(UserLearningTask::getBizId, stageIds);
        queryWrapper.eq(UserLearningTask::getUserId, userId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);

        List<UserLearningTask> stageTasks = userLearningTaskMapper.selectList(queryWrapper);
        Map<Long, UserLearningTask> stageTaskMap = null == stageTasks ? new HashMap<>() : stageTasks.stream()
                .collect(Collectors.toMap(UserLearningTask::getBizId, v -> v));

        // 获取学习地图信息
        LearningMap learningMap = learningMapMapper.selectById(stages.get(0).getMapId());
        Integer unlockMode = learningMap != null ? learningMap.getUnlockMode() : 0;

        // 构建阶段数据
        for (int i = 0; i < stages.size(); i++) {
            LearningMapStage stage = stages.get(i);
            MapDetailResponse.StageInfo stageInfo = new MapDetailResponse.StageInfo();
            stageInfo.setId(stage.getId());
            stageInfo.setName(stage.getName());
            stageInfo.setStageOrder(stage.getStageOrder());
            stageInfo.setCredit(stage.getCredit());

            // 设置阶段状态
            UserLearningTask stageLearningTask = stageTaskMap.get(stage.getId());
            String stageStatus;

            if (stageLearningTask != null) {
                // 如果阶段有学习记录，使用学习记录中的状态
                stageStatus = stageLearningTask.getStatus();
            } else {
                // 根据解锁模式设置阶段状态
                if (unlockMode == 2) {
                    // 自由模式：全部解锁
                    stageStatus = "learning";
                } else {
                    // 按阶段解锁或按阶段和任务解锁
                    if (i == 0) {
                        // 第一个阶段默认解锁
                        stageStatus = "learning";
                    } else {
                        // 检查前一个阶段是否完成
                        UserLearningTask prevStageTask = stageTaskMap.get(stages.get(i - 1).getId());
                        if (prevStageTask != null && "completed".equals(prevStageTask.getStatus())) {
                            stageStatus = "learning";
                        } else {
                            stageStatus = "locked";
                        }
                    }
                }
            }
            stageInfo.setStatus(stageStatus);

            // 设置任务列表
            List<MapDetailResponse.TaskInfo> taskInfoList = new ArrayList<>();
            List<ContentRelation> tasks = getTasksByStageId(stage.getId());

            if (tasks != null && !tasks.isEmpty()) {
                for (ContentRelation task : tasks) {
                    MapDetailResponse.TaskInfo taskInfo = new MapDetailResponse.TaskInfo();
                    taskInfo.setId(task.getContentId());
                    taskInfo.setType(task.getContentType());
                    taskInfo.setTitle(getTaskTitle(task));
                    taskInfo.setIsRequired(task.getIsRequired() == 1);

                    // 设置任务状态
                    String taskStatus;
                    if (unlockMode == 2) {
                        // 自由模式：全部解锁
                        taskStatus = "learning";
                    } else if (unlockMode == 1) {
                        // 按阶段解锁：阶段解锁后任务全部解锁
                        taskStatus = "learning".equals(stageStatus) ? "learning" : "locked";
                    } else {
                        // 按阶段和任务解锁：任务按顺序解锁
                        if (taskInfoList.isEmpty()) {
                            // 第一个任务默认解锁
                            taskStatus = "learning".equals(stageStatus) ? "learning" : "locked";
                        } else {
                            // 检查前一个任务是否完成
                            MapDetailResponse.TaskInfo prevTask = taskInfoList.get(taskInfoList.size() - 1);
                            if ("completed".equals(prevTask.getStatus())) {
                                taskStatus = "learning".equals(stageStatus) ? "learning" : "locked";
                            } else {
                                taskStatus = "locked";
                            }
                        }
                    }
                    taskInfo.setStatus(taskStatus);

                    taskInfoList.add(taskInfo);
                }
            }

            stageInfo.setTasks(taskInfoList);
            stageInfoList.add(stageInfo);
        }

        return stageInfoList;
    }


    /**
     * 构建学习人员信息数据
     *
     * @param mapId 学习地图ID
     * @return 学习人员信息数据
     */
    private MapDetailResponse.LearnersInfo buildLearnersData(Long mapId) {
        MapDetailResponse.LearnersInfo learnersInfo = new MapDetailResponse.LearnersInfo();

        // 查询学习人员
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP);
        queryWrapper.eq(UserLearningTask::getBizId, mapId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> progressList = userLearningTaskMapper.selectList(queryWrapper);

        if (progressList == null || progressList.isEmpty()) {
            // 没有学习人员
            learnersInfo.setTotal(0);
            learnersInfo.setCompleted(0);
            learnersInfo.setLearning(0);
            learnersInfo.setNotStart(0);
            learnersInfo.setCompletedList(Collections.emptyList());
            learnersInfo.setLearningList(Collections.emptyList());
            learnersInfo.setNotStartList(Collections.emptyList());
            learnersInfo.setList(Collections.emptyList());
            return learnersInfo;
        }

        // 设置总学习人数
        learnersInfo.setTotal(progressList.size());

        // 按状态分组用户
        List<UserLearningTask> completedUsers = progressList.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equals(LearningStatus.COMPLETED))
                .collect(Collectors.toList());

        List<UserLearningTask> learningUsers = progressList.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().equals(LearningStatus.LEARNING))
                .collect(Collectors.toList());

        List<UserLearningTask> notStartUsers = progressList.stream()
                .filter(t -> t.getStatus() == null || t.getStatus().equals(LearningStatus.NOT_STARTED))
                .collect(Collectors.toList());

        // 设置各状态人数
        learnersInfo.setCompleted(completedUsers.size());
        learnersInfo.setLearning(learningUsers.size());
        learnersInfo.setNotStart(notStartUsers.size());

        // 获取用户信息
        List<Long> userIds = progressList.stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getUserId, userIds);
            userQueryWrapper.eq(User::getIsDel, 0);
            List<User> users = userMapper.selectList(userQueryWrapper);

            userMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, user -> user, (v1, v2) -> v1));
        }

        // 构建已完成学习人员列表（最多显示20个）
        List<MapDetailResponse.LearnerInfo> completedItems = buildLearnerItems(completedUsers, userMap, 20, "completed");
        learnersInfo.setCompletedList(completedItems);

        // 构建学习中人员列表（最多显示20个）
        List<MapDetailResponse.LearnerInfo> learningItems = buildLearnerItems(learningUsers, userMap, 20, "learning");
        learnersInfo.setLearningList(learningItems);

        // 构建未开始人员列表（最多显示20个）
        List<MapDetailResponse.LearnerInfo> notStartItems = buildLearnerItems(notStartUsers, userMap, 20, "not_started");
        learnersInfo.setNotStartList(notStartItems);

        // 兼容旧版本，设置list字段
        List<MapDetailResponse.LearnerInfo> allItems = new ArrayList<>();
        allItems.addAll(completedItems);
        allItems.addAll(learningItems);
        allItems.addAll(notStartItems);

        // 限制返回的学习人员数量，最多返回10个
        if (allItems.size() > 10) {
            allItems = allItems.subList(0, 10);
        }

        learnersInfo.setList(allItems);

        return learnersInfo;
    }

    /**
     * 构建学习人员列表项
     *
     * @param tasks    学习任务记录列表
     * @param userMap  用户信息映射
     * @param maxCount 最大显示数量
     * @param status   学习状态
     * @return 学习人员列表
     */
    private List<MapDetailResponse.LearnerInfo> buildLearnerItems(
            List<UserLearningTask> tasks,
            Map<Long, User> userMap,
            int maxCount,
            String status) {

        List<MapDetailResponse.LearnerInfo> items = new ArrayList<>();
        int count = 0;

        for (UserLearningTask task : tasks) {
            if (count >= maxCount) {
                break;
            }

            MapDetailResponse.LearnerInfo item = new MapDetailResponse.LearnerInfo();
            item.setUserId(task.getUserId());
            item.setStatus(status);

            // 设置用户信息
            User user = userMap.get(task.getUserId());
            if (user != null) {
                item.setNickname(user.getNickname());
                item.setAvatar(user.getAvatar());
            } else {
                item.setNickname(task.getCreatorName());
                item.setAvatar("");
            }

            items.add(item);
            count++;
        }

        return items;
    }

    /**
     * 构建任务信息列表
     *
     * @param taskRelations 任务关联列表
     * @param taskRecords   任务记录列表
     * @return 任务信息列表
     */
    private List<MapStageDetailResponse.TaskInfo> buildTaskInfoList(List<ContentRelation> taskRelations,
                                                                    List<UserLearningTask> taskRecords) {
        List<MapStageDetailResponse.TaskInfo> taskInfoList = new ArrayList<>();

        // 构建任务记录映射
        Map<String, UserLearningTask> taskRecordMap = new HashMap<>();
        if (taskRecords != null && !taskRecords.isEmpty()) {
            for (UserLearningTask record : taskRecords) {
                // 使用内容ID作为键
                taskRecordMap.put(record.buildContentSearchKey(), record);
            }
        }

        // 获取任务内容信息
        List<Pair<String, Long>> contentSearchKeys = taskRelations.stream()
                .map(v -> Pair.of(v.getContentType(), v.getContentId()))
                .collect(Collectors.toList());
        Map<String, ContentBO> contentMap = contentService.getContentMap(contentSearchKeys);

        // 构建任务信息
        Long currentTaskId = null;
        for (ContentRelation relation : taskRelations) {
            MapStageDetailResponse.TaskInfo taskInfo = new MapStageDetailResponse.TaskInfo();
            taskInfo.setId(relation.getContentId());

            // 设置任务类型
            String type = relation.getContentType();
            taskInfo.setType(type);

            // 设置是否必修
            taskInfo.setIsRequired(relation.getIsRequired());

            // 设置内容信息
            ContentBO contentBO = contentMap.get(relation.getContentSearchKey());
            if (contentBO != null) {
                taskInfo.setTitle(contentBO.getTitle());
                taskInfo.setContentType(contentBO.getContentType());
                taskInfo.setCoverImage(contentBO.getCoverImg());
                taskInfo.setType(contentBO.getType());
                taskInfo.setId(contentBO.getId());
            }

            // 设置任务状态和进度
            UserLearningTask record = taskRecordMap.get(relation.getContentSearchKey());
            String status = record == null ? LearningStatus.NOT_STARTED : record.getStatus();
            Integer progress = record == null ? 0 : record.getProgress();
            taskInfo.setStatusAndProgress(status, progress);

            // 全部完成标记
            if (!StageStatus.COMPLETED.equals(taskInfo.getStatus()) && Objects.isNull(currentTaskId)) {
                currentTaskId = taskInfo.getId();
                taskInfo.setStatus(StageStatus.LEARNING);
            }

            taskInfoList.add(taskInfo);
        }

        return taskInfoList;
    }

    /**
     * 构建用户阶段学习进度
     *
     * @param taskRecords    任务记录列表
     * @param totalTaskCount 总任务数
     * @return 用户阶段学习进度
     */
    private MapStageDetailResponse.UserProgress buildUserStageProgress(List<UserLearningTask> taskRecords, int totalTaskCount) {
        MapStageDetailResponse.UserProgress progress = new MapStageDetailResponse.UserProgress();

        // 设置总任务数
        progress.setTotalTaskCount(totalTaskCount);

        // 计算已完成任务数
        int completedTaskCount = 0;
        if (taskRecords != null && !taskRecords.isEmpty()) {
            for (UserLearningTask record : taskRecords) {
                if (record.getStatus() != null && record.getStatus().equals(LearningStatus.COMPLETED)) {
                    completedTaskCount++;
                }
            }
        }
        progress.setCompletedTaskCount(completedTaskCount);

        // 计算进度百分比
        int progressPercent = 0;
        if (totalTaskCount > 0) {
            progressPercent = (int) Math.round((double) completedTaskCount / totalTaskCount * 100);
        }
        progress.setProgress(progressPercent);

        return progress;
    }

    /**
     * 获取用户学习进度
     *
     * @param mapId  学习地图ID
     * @param userId 用户ID
     * @return 用户学习进度实体
     */
    private UserLearningTask getUserProgress(Long mapId, Long userId) {
        if (userId == null) {
            return null;
        }

        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP);
        queryWrapper.eq(UserLearningTask::getBizId, mapId);
        queryWrapper.eq(UserLearningTask::getUserId, userId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);

        return userLearningTaskMapper.selectOne(queryWrapper);
    }

    /**
     * 获取阶段对应的学习任务
     *
     * @param stageId 阶段ID
     * @param userId  用户ID
     * @return 阶段学习任务
     */
    private UserLearningTask getStageTask(Long stageId, Long userId) {
        if (stageId == null || userId == null) {
            return null;
        }

        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE);
        queryWrapper.eq(UserLearningTask::getBizId, stageId);
        queryWrapper.eq(UserLearningTask::getUserId, userId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);

        return userLearningTaskMapper.selectOne(queryWrapper);
    }

    /**
     * 获取学习地图的阶段列表
     *
     * @param mapId 学习地图ID
     * @return 阶段实体列表
     */
    private List<LearningMapStage> getStages(Long mapId) {
        LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMapStage::getMapId, mapId);
        queryWrapper.eq(LearningMapStage::getIsDel, 0);
        queryWrapper.orderByAsc(LearningMapStage::getStageOrder);

        return learningMapStageMapper.selectList(queryWrapper);
    }

    /**
     * 获取阶段的任务记录
     *
     * @param stageIds 阶段ID列表
     * @param userId   用户ID
     * @return 阶段ID到任务记录列表的映射
     */
    private Map<Long, List<UserLearningTask>> getStageTaskRecords(List<Long> stageIds, Long userId) {
        if (stageIds == null || stageIds.isEmpty() || userId == null) {
            return new HashMap<>();
        }

        // 获取所有阶段关联的内容
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.in(ContentRelation::getBizId, stageIds);
        relationQueryWrapper.eq(ContentRelation::getBizType, "MAP_STAGE");
        relationQueryWrapper.eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> relations = contentRelationMapper.selectList(relationQueryWrapper);

        // 按阶段ID分组
        Map<Long, List<ContentRelation>> stageRelationsMap = new HashMap<>();
        for (ContentRelation relation : relations) {
            Long stageId = relation.getBizId();
            if (!stageRelationsMap.containsKey(stageId)) {
                stageRelationsMap.put(stageId, new ArrayList<>());
            }
            stageRelationsMap.get(stageId).add(relation);
        }

        // 获取所有内容的学习记录
        List<Long> contentIds = relations.stream()
                .map(ContentRelation::getContentId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (contentIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<UserLearningTask> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.in(UserLearningTask::getBizId, contentIds);
        taskQueryWrapper.eq(UserLearningTask::getUserId, userId);
        taskQueryWrapper.eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> tasks = userLearningTaskMapper.selectList(taskQueryWrapper);

        // 按阶段ID分组
        Map<Long, List<UserLearningTask>> stageTasksMap = new HashMap<>();
        for (Map.Entry<Long, List<ContentRelation>> entry : stageRelationsMap.entrySet()) {
            Long stageId = entry.getKey();
            List<ContentRelation> stageRelations = entry.getValue();

            // 获取该阶段关联的内容ID
            List<Long> stageContentIds = stageRelations.stream()
                    .map(ContentRelation::getContentId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            // 过滤出该阶段内容的学习记录
            List<UserLearningTask> stageTasks = tasks.stream()
                    .filter(task -> stageContentIds.contains(task.getBizId()))
                    .collect(Collectors.toList());

            stageTasksMap.put(stageId, stageTasks);
        }

        return stageTasksMap;
    }

    /**
     * 获取单个阶段的任务记录
     *
     * @param searchKeys
     * @return 任务记录列表
     */
    private List<UserLearningTask> getStageTaskRecords(List<String> searchKeys) {
        if (CollectionUtils.isEmpty(searchKeys)) {
            return new ArrayList<>();
        }
        // 查询学习记录
        LambdaQueryWrapper<UserLearningTask> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.in(UserLearningTask::getSearchKey, searchKeys);
        taskQueryWrapper.eq(UserLearningTask::getIsDel, 0);

        return userLearningTaskMapper.selectList(taskQueryWrapper);
    }

    /**
     * 获取阶段的任务关联
     *
     * @param stageIds 阶段ID列表
     * @return 阶段ID到任务关联列表的映射
     */
    private Map<Long, List<ContentRelation>> getStageTaskRelations(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ContentRelation::getBizId, stageIds);
        queryWrapper.eq(ContentRelation::getBizType, "MAP_STAGE");
        queryWrapper.eq(ContentRelation::getIsDel, 0);
        queryWrapper.orderByAsc(ContentRelation::getSortOrder);

        List<ContentRelation> relations = contentRelationMapper.selectList(queryWrapper);

        // 按阶段ID分组
        Map<Long, List<ContentRelation>> stageRelationsMap = new HashMap<>();
        for (ContentRelation relation : relations) {
            Long stageId = relation.getBizId();
            if (!stageRelationsMap.containsKey(stageId)) {
                stageRelationsMap.put(stageId, new ArrayList<>());
            }
            stageRelationsMap.get(stageId).add(relation);
        }

        return stageRelationsMap;
    }

    /**
     * 获取单个阶段的任务关联
     *
     * @param stageId 阶段ID
     * @return 任务关联列表
     */
    private List<ContentRelation> getStageTaskRelations(Long stageId) {
        if (stageId == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, stageId);
        queryWrapper.eq(ContentRelation::getBizType, BizType.MAP_STAGE);
        queryWrapper.notIn(ContentRelation::getContentType, Lists.newArrayList(BizType.CERTIFICATE));
        queryWrapper.eq(ContentRelation::getIsDel, 0);
        queryWrapper.orderByAsc(ContentRelation::getSortOrder);

        return contentRelationMapper.selectList(queryWrapper);
    }

    /**
     * 获取内容标题
     *
     * @param contentIds 内容ID列表
     * @return 内容ID到标题的映射
     */
    private Map<Long, String> getContentTitles(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, String> titleMap = new HashMap<>();

        // 查询课程标题
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Courses::getId, contentIds);
        queryWrapper.eq(Courses::getIsDel, 0);

        List<Courses> courses = coursesMapper.selectList(queryWrapper);
        for (Courses course : courses) {
            titleMap.put(course.getId(), course.getTitle());
        }

        // 这里可以添加其他类型内容的标题查询，如考试、作业等

        return titleMap;
    }


    /**
     * 检查用户是否有权限访问学习地图
     *
     * @param mapId  学习地图ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    private void checkUserHasPermission(Long mapId, Long userId) {
        // 构建范围查询请求
        CommonRangeQueryRequest rangeRequest = new CommonRangeQueryRequest();
        rangeRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode()); // 可见范围
        rangeRequest.setType(BizType.LEARNING_MAP); // 学习地图
        rangeRequest.setUserId(userId.toString());
        rangeRequest.setTypeId(mapId);
        commonRangeInterface.checkUserHasRightsIfNotThrowException(rangeRequest);
    }

    /**
     * 获取阶段下的所有任务
     *
     * @param stageId 阶段ID
     * @return 任务列表
     */
    private List<ContentRelation> getTasksByStageId(Long stageId) {
        if (stageId == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, stageId);
        queryWrapper.eq(ContentRelation::getBizType, BizType.MAP_STAGE);
        queryWrapper.eq(ContentRelation::getIsDel, 0);
        queryWrapper.orderByAsc(ContentRelation::getSortOrder);

        return contentRelationMapper.selectList(queryWrapper);
    }

    /**
     * 获取任务标题
     *
     * @param task 任务关联关系
     * @return 任务标题
     */
    private String getTaskTitle(ContentRelation task) {
        if (task == null) {
            return "";
        }

        // 根据任务类型获取标题
        String type = task.getContentType();
        Long contentId = task.getContentId();

        if (contentId == null) {
            return "";
        }

        // TODO: 根据不同的任务类型获取对应的标题
        // 这里需要根据实际业务逻辑实现
        return "任务" + contentId;
    }
}
