package com.learn.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.train.StatisticsDTO;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.train.TrainStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @Description 培训统计服务实现类
 * @date 2025/4/21
 */
@Service
public class TrainStatisticsServiceImpl implements TrainStatisticsService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public StatisticsDTO getTrainStatistics(Long trainId) {
        // 检查培训是否存在
        Train train = trainMapper.selectById(trainId);
        if (train == null || train.getIsDel() == 1) {
            throw new IllegalArgumentException("培训不存在");
        }

        // 查询培训学习记录
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getBizId, trainId)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(taskWrapper);

        // 查询培训内容学习记录（子任务）
        LambdaQueryWrapper<UserLearningTask> contentTaskWrapper = new LambdaQueryWrapper<>();
        contentTaskWrapper.eq(UserLearningTask::getParentType, BizType.TRAIN)
                .eq(UserLearningTask::getParentId, trainId)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> contentTasks = userLearningTaskMapper.selectList(contentTaskWrapper);

        // 构建统计数据
        StatisticsDTO statisticsDTO = new StatisticsDTO();

        // 1. 学习人数、完成人数、完成率
        int learnerCount = learningTasks.size();
        int completionCount = (int) learningTasks.stream()
                .filter(t -> Objects.equals(t.getStatus(), LearningStatus.COMPLETED)) // 状态为2表示已完成
                .count();
        float completionRate = learnerCount > 0 ? (float) completionCount / learnerCount : 0f;

        statisticsDTO.setLearnerCount(learnerCount);
        statisticsDTO.setCompletionCount(completionCount);
        statisticsDTO.setCompletionRate(completionRate);

        // 2. 平均学习时长、总学习时长
        int totalDuration = learningTasks.stream()
                .mapToInt(t -> t.getStudyDuration() != null ? t.getStudyDuration() : 0)
                .sum();
        int avgDuration = learnerCount > 0 ? totalDuration / learnerCount : 0;

        statisticsDTO.setTotalDuration(totalDuration);
        statisticsDTO.setAvgDuration(avgDuration);

        // 3. 课程完成率、考试完成率、作业完成率、调研完成率
        if (!CollectionUtils.isEmpty(contentTasks)) {
            // 按内容类型分组
            Map<String, List<UserLearningTask>> tasksByType = contentTasks.stream()
                    .collect(Collectors.groupingBy(UserLearningTask::getBizType));

            // 课程完成率
            List<UserLearningTask> courseRecords = tasksByType.getOrDefault(BizType.COURSE, Collections.emptyList());
            int courseTotal = courseRecords.size();
            int courseFinished = (int) courseRecords.stream()
                    .filter(r -> Objects.equals(r.getStatus(), LearningStatus.COMPLETED)) // 状态为2表示已完成
                    .count();
            float courseCompletionRate = courseTotal > 0 ? (float) courseFinished / courseTotal : 0f;
            statisticsDTO.setCourseCompletionRate(courseCompletionRate);

            // 考试完成率
            List<UserLearningTask> examRecords = tasksByType.getOrDefault("EXAM", Collections.emptyList());
            int examTotal = examRecords.size();
            int examFinished = (int) examRecords.stream()
                    .filter(r -> Objects.equals(r.getStatus(), LearningStatus.COMPLETED)) // 状态为2表示已完成
                    .count();
            float examCompletionRate = examTotal > 0 ? (float) examFinished / examTotal : 0f;
            statisticsDTO.setExamCompletionRate(examCompletionRate);

            // 作业完成率
            List<UserLearningTask> assignmentRecords = tasksByType.getOrDefault("ASSIGNMENT", Collections.emptyList());
            int assignmentTotal = assignmentRecords.size();
            int assignmentFinished = (int) assignmentRecords.stream()
                    .filter(r -> Objects.equals(r.getStatus(), LearningStatus.COMPLETED)) // 状态为2表示已完成
                    .count();
            float assignmentCompletionRate = assignmentTotal > 0 ? (float) assignmentFinished / assignmentTotal : 0f;
            statisticsDTO.setAssignmentCompletionRate(assignmentCompletionRate);

            // 调研完成率
            List<UserLearningTask> surveyRecords = tasksByType.getOrDefault("SURVEY", Collections.emptyList());
            int surveyTotal = surveyRecords.size();
            int surveyFinished = (int) surveyRecords.stream()
                    .filter(r -> Objects.equals(r.getStatus(), LearningStatus.COMPLETED)) // 状态为2表示已完成
                    .count();
            float surveyCompletionRate = surveyTotal > 0 ? (float) surveyFinished / surveyTotal : 0f;
            statisticsDTO.setSurveyCompletionRate(surveyCompletionRate);
        } else {
            statisticsDTO.setCourseCompletionRate(0f);
            statisticsDTO.setExamCompletionRate(0f);
            statisticsDTO.setAssignmentCompletionRate(0f);
            statisticsDTO.setSurveyCompletionRate(0f);
        }

        // 4. 部门统计
        // 获取用户ID列表
//        List<Long> userIds = learningTasks.stream()
//                .map(UserLearningTask::getUserId)
//                .collect(Collectors.toList());
        
        // 查询用户信息，获取部门信息
//        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
//        userWrapper.in(User::getId, userIds)
//                .eq(User::getIsDel, 0);
//        List<User> users = userMapper.selectList(userWrapper);

        // 构建用户ID到部门名称的映射
//        Map<Long, String> userDepartmentMap = users.stream()
//                .collect(Collectors.toMap(User::getId, User::getDepartment, (v1, v2) -> v1));
//
//        // 按部门分组
//        Map<String, List<UserLearningTask>> tasksByDepartment = learningTasks.stream()
//                .collect(Collectors.groupingBy(task ->
//                    userDepartmentMap.get(task.getUserId())));
//
//        List<TrainStatisticsDTO.DepartmentStat> departmentStats = new ArrayList<>();
//        for (Map.Entry<String, List<UserLearningTask>> entry : tasksByDepartment.entrySet()) {
//            String departmentName = entry.getKey();
//            List<UserLearningTask> departmentTasks = entry.getValue();
//
//            TrainStatisticsDTO.DepartmentStat departmentStat = new TrainStatisticsDTO.DepartmentStat();
//            departmentStat.setName(departmentName);
//            departmentStat.setCount(departmentTasks.size());
//            departmentStat.setCompletionCount((int) departmentTasks.stream()
//                    .filter(t -> t.getStatus() == 2) // 状态为2表示已完成
//                    .count());
//
//            departmentStats.add(departmentStat);
//        }
//        statisticsDTO.setDepartmentStats(departmentStats);

        // 5. 时间分布
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> timeDistributionMap = new HashMap<>();

        for (UserLearningTask task : learningTasks) {
            Date lastStudyDate = null;
            
            // 使用最后学习时间或完成时间
            if (task.getCompletionTime() != null) {
                lastStudyDate = task.getCompletionTime();
            } else if (task.getLastStudyTime() != null) {
                lastStudyDate = task.getLastStudyTime();
            }
            
            if (lastStudyDate != null) {
                String dateStr = dateFormat.format(lastStudyDate);
                timeDistributionMap.put(dateStr, timeDistributionMap.getOrDefault(dateStr, 0) + 1);
            }
        }

        List<StatisticsDTO.TimeDistribution> timeDistributions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : timeDistributionMap.entrySet()) {
            StatisticsDTO.TimeDistribution timeDistribution = new StatisticsDTO.TimeDistribution();
            timeDistribution.setDate(entry.getKey());
            timeDistribution.setCount(entry.getValue());
            timeDistributions.add(timeDistribution);
        }
        
        // 按日期排序
        timeDistributions.sort(Comparator.comparing(StatisticsDTO.TimeDistribution::getDate));
        statisticsDTO.setTimeDistribution(timeDistributions);

        return statisticsDTO;
    }
}
