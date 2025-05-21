package com.learn.service.map.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.map.LearningMapStatisticsDTO;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.map.LearnMapStatisticsService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习地图统计服务实现类
 */
@Service
@Slf4j
public class LearnMapStatisticsServiceImpl implements LearnMapStatisticsService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Override
    public void setLearningMapStatistics(LearningMapStatisticsDTO statisticsDTO) {
        if (null == statisticsDTO || null == statisticsDTO.getId()) {
            log.error("学习地图ID不能为空");
            return;
        }

        // 查询所有学习任务记录
        List<UserLearningTask> taskRecordList = getTaskRecordList(statisticsDTO.getId());

        // 查询所有学习阶段
        List<LearningMapStage> stageList = getStageList(statisticsDTO.getId());

        // 构建统计数据
        buildStatistics(statisticsDTO, taskRecordList, stageList);
    }


    /**
     * 获取学习地图的任务记录列表
     *
     * @param mapId 学习地图ID
     * @return 任务记录列表
     */
    private List<UserLearningTask> getTaskRecordList(Long mapId) {
        // 查询所有阶段
        List<LearningMapStage> stages = getStageList(mapId);
        if (stages.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有阶段ID
        List<Long> stageIds = stages.stream()
                .map(LearningMapStage::getId)
                .collect(Collectors.toList());

        // 查询所有阶段的学习任务
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE)
                .in(UserLearningTask::getBizId, stageIds)
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectList(queryWrapper);
    }

    /**
     * 获取学习地图的阶段列表
     *
     * @param mapId 学习地图ID
     * @return 阶段列表
     */
    private List<LearningMapStage> getStageList(Long mapId) {
        LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMapStage::getMapId, mapId)
                .eq(LearningMapStage::getIsDel, 0)
                .orderByAsc(LearningMapStage::getStageOrder);
        return learningMapStageMapper.selectList(queryWrapper);
    }

    /**
     * 构建统计数据
     *
     * @param taskRecordList 任务记录列表
     * @param stageList 阶段列表
     * @return 统计数据
     */
    private void buildStatistics(
            LearningMapStatisticsDTO statisticsDTO,
            List<UserLearningTask> taskRecordList,
            List<LearningMapStage> stageList) {

        // 各阶段完成率
        calculateStageCompletionRates(statisticsDTO, taskRecordList, stageList);

    }



    /**
     * 计算各阶段完成率
     *
     * @param statisticsDTO 统计数据DTO
     * @param taskRecordList 任务记录列表
     * @param stageList 阶段列表
     */
    private void calculateStageCompletionRates(
            LearningMapStatisticsDTO statisticsDTO,
            List<UserLearningTask> taskRecordList,
            List<LearningMapStage> stageList) {
        
        List<LearningMapStatisticsDTO.StageCompletionRate> stageCompletionRates = new ArrayList<>();
        
        // 按阶段ID分组任务记录
        Map<Long, List<UserLearningTask>> stageTaskMap = taskRecordList.stream()
                .collect(Collectors.groupingBy(UserLearningTask::getBizId));
        
        // 计算每个阶段的完成率
        for (LearningMapStage stage : stageList) {
            LearningMapStatisticsDTO.StageCompletionRate stageRate = new LearningMapStatisticsDTO.StageCompletionRate();
            stageRate.setName(stage.getName());
            
            List<UserLearningTask> stageTasks = stageTaskMap.getOrDefault(stage.getId(), Collections.emptyList());
            int totalTasks = stageTasks.size();
            int completedTasks = (int) stageTasks.stream()
                    .filter(task -> task.getStatus() != null && LearningStatus.COMPLETED.equals(task.getStatus()))
                    .count();
            float rate = totalTasks > 0 ? (float) completedTasks / totalTasks : 0f;
            stageRate.setRate(rate);
            stageCompletionRates.add(stageRate);
        }
        
        statisticsDTO.setStageCompletionRates(stageCompletionRates);
    }


    /**
     * 计算时间分布数据
     *
     * @param statisticsDTO 统计数据DTO
     * @param userProgressList 用户进度列表
     */
    private void calculateTimeDistribution(
            LearningMapStatisticsDTO statisticsDTO,
            List<UserLearningTask> userProgressList) {
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        // 按开始学习日期分组
        Map<String, List<UserLearningTask>> dateMap = new HashMap<>();
        for (UserLearningTask progress : userProgressList) {
            if (progress.getStartTime() != null) {
                String dateStr = dateFormat.format(progress.getStartTime());
                dateMap.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(progress);
            }
        }
        
        List<LearningMapStatisticsDTO.TimeDistribution> timeDistribution = new ArrayList<>();
        
        // 计算每天的学习人数
        for (Map.Entry<String, List<UserLearningTask>> entry : dateMap.entrySet()) {
            LearningMapStatisticsDTO.TimeDistribution distribution = new LearningMapStatisticsDTO.TimeDistribution();
            distribution.setDate(entry.getKey());
            distribution.setCount(entry.getValue().size());
            timeDistribution.add(distribution);
        }
        
        // 按日期升序排序
        timeDistribution.sort(Comparator.comparing(LearningMapStatisticsDTO.TimeDistribution::getDate));
        
        statisticsDTO.setTimeDistribution(timeDistribution);
    }
    
    /**
     * 解析attributes字符串为Map
     *
     * @param attributes 属性字符串
     * @return 属性Map
     */
    private Map<String, Object> parseAttributes(String attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return Json.fromJson(attributes, Map.class);
        } catch (Exception e) {
            log.warn("解析attributes失败: {}", attributes, e);
            return new HashMap<>();
        }
    }
}
