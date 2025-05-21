package com.learn.service.common.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.dto.query.StatisticsQueryBO;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.dto.common.LearnerDTO;
import com.learn.dto.train.StatisticLearnersResponse;
import com.learn.dto.train.StatisticsDTO;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.common.StatisticService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @date 2025/5/13
 */
@Service
@Slf4j
public class StatisticServiceImpl implements StatisticService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public <T extends StatisticsDTO> T getStatistics(StatisticsQueryBO queryBO, Class<T> type) {
        // 参数说明：bizType 和 bizId 必填。 bizType 只支持TRAIN 和 LEARNING_MAP.bizId是对应的培训id和课程id。其他查询条件不需要
        if (queryBO == null || !StringUtils.hasText(queryBO.getBizType()) || Objects.isNull(queryBO.getBizId())) {
            throw new IllegalArgumentException("业务类型和业务ID不能为空");
        }

        String bizType = queryBO.getBizType();
        Long bizId = queryBO.getBizId();

        // 验证业务类型
        if (!BizType.TRAIN.equals(bizType) && !BizType.LEARNING_MAP.equals(bizType)) {
            throw new IllegalArgumentException("不支持的业务类型：" + bizType);
        }

        // 1. 使用count SQL统计学习人数和完成人数
        Map<String, Object> learnerStats = userLearningTaskMapper.countLearnerAndCompletion(bizType, bizId);
        int learnerCount = Objects.isNull(learnerStats.get("learnerCount")) ? 0 : ((Number) learnerStats.get("learnerCount")).intValue();
        int completionCount = Objects.isNull(learnerStats.get("completionCount")) ? 0 : ((Number) learnerStats.get("completionCount")).intValue();
        float completionRate = learnerCount > 0 ? (float) completionCount / learnerCount : 0f;

        // 2. 使用count SQL统计学习时长
        Map<String, Object> durationStats = userLearningTaskMapper.countDuration(bizType, bizId);
        int totalDuration = Objects.isNull(durationStats.get("totalDuration")) ? 0 : ((Number) durationStats.get("totalDuration")).intValue();
        int avgDuration = Objects.isNull(durationStats.get("avgDuration")) ? 0 : ((Number) durationStats.get("avgDuration")).intValue();

        // 3. 使用count SQL按内容类型统计完成率
        List<Map<String, Object>> contentTypeStats = userLearningTaskMapper.countCompletionByContentType(bizType, bizId);


        // 构建统计数据
        try {
            T statisticsDTO = type.getDeclaredConstructor().newInstance();
            statisticsDTO.setId(bizId);
            statisticsDTO.setLearnerCount(learnerCount);
            statisticsDTO.setCompletionCount(completionCount);
            statisticsDTO.setCompletionRate(completionRate);


            statisticsDTO.setTotalDuration(totalDuration);
            statisticsDTO.setAvgDuration(avgDuration);
            // 初始化默认值
            statisticsDTO.setCourseCompletionRate(0f);
            statisticsDTO.setExamCompletionRate(0f);
            statisticsDTO.setAssignmentCompletionRate(0f);
            statisticsDTO.setSurveyCompletionRate(0f);

            if (!CollectionUtils.isEmpty(contentTypeStats)) {
                for (Map<String, Object> stat : contentTypeStats) {
                    String contentType = (String) stat.get("contentType");
                    int totalCount = ((Number) stat.get("totalCount")).intValue();
                    int completedCount = ((Number) stat.get("completedCount")).intValue();
                    float rate = totalCount > 0 ? (float) completedCount / totalCount : 0f;

                    // 根据内容类型设置对应的完成率
                    if (BizType.COURSE.equals(contentType)) {
                        statisticsDTO.setCourseCompletionRate(rate);
                    } else if ("EXAM".equals(contentType)) {
                        statisticsDTO.setExamCompletionRate(rate);
                    } else if ("ASSIGNMENT".equals(contentType)) {
                        statisticsDTO.setAssignmentCompletionRate(rate);
                    } else if ("SURVEY".equals(contentType)) {
                        statisticsDTO.setSurveyCompletionRate(rate);
                    }
                }
            }
            return statisticsDTO;
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            throw new RuntimeException("获取统计数据失败");
        }


    }

    @Override
    public StatisticLearnersResponse getLearners(StatisticsQueryBO queryBO) {
        // 参数说明：bizType 和 bizId 必填。 bizType 只支持TRAIN 和 LEARNING_MAP.bizId是对应的培训id和课程id。
        if (queryBO == null || !StringUtils.hasText(queryBO.getBizType()) || Objects.isNull(queryBO.getBizId())) {
            throw new IllegalArgumentException("业务类型和业务ID不能为空");
        }

        String bizType = queryBO.getBizType();
        Long bizId = queryBO.getBizId();

        // 验证业务类型
        if (!BizType.TRAIN.equals(bizType) && !BizType.LEARNING_MAP.equals(bizType)) {
            throw new IllegalArgumentException("不支持的业务类型：" + bizType);
        }

        // 设置默认分页参数
        Integer pageNum = queryBO.getPageNum();
        Integer pageSize = queryBO.getPageSize();
        
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 查询学习完成情况
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, bizType)
                .eq(UserLearningTask::getBizId, bizId)
                .eq(UserLearningTask::getIsDel, 0);
        
        // 按用户ID筛选
        if (Objects.nonNull(queryBO.getUserId())) {
            taskWrapper.eq(UserLearningTask::getUserId, queryBO.getUserId());
        }
        
        // 按完成状态筛选
        if (org.apache.commons.lang3.StringUtils.isNotBlank(queryBO.getStatus())) {
            taskWrapper.eq(UserLearningTask::getStatus, queryBO.getStatus());
        }
        
        // 分页查询
        Page<UserLearningTask> page = new Page<>(pageNum, pageSize);
        Page<UserLearningTask> taskPage = userLearningTaskMapper.selectPage(page, taskWrapper);
        
        // 构建响应对象
        StatisticLearnersResponse response = new StatisticLearnersResponse();
        response.setTotal(Math.toIntExact(taskPage.getTotal()));
        if (taskPage.getRecords() == null || taskPage.getRecords().isEmpty()) {
            response.setList(new ArrayList<>());
            return response;
        }
        
        // 获取所有用户ID
        List<Long> taskUserIds = taskPage.getRecords().stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());
                
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getUserId, taskUserIds)
                .eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = new HashMap<>();
        if (users != null) {
            userMap = users.stream().collect(Collectors.toMap(User::getUserId, user -> user));
        }

        // 查询阶段信息
        Map<Long, String> stageNameMap = new HashMap<>();
        if (BizType.LEARNING_MAP.equals(bizType)) {
            // 查询学习地图的阶段列表
            LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LearningMapStage::getMapId, queryBO.getBizId())
                    .eq(LearningMapStage::getIsDel, 0)
                    .orderByAsc(LearningMapStage::getStageOrder);
            List<LearningMapStage> stages = learningMapStageMapper.selectList(queryWrapper);
            stageNameMap = stages.stream().collect(Collectors.toMap(LearningMapStage::getId, LearningMapStage::getName));
        }

        
        // 构建学习人员列表
        List<LearnerDTO> learnerList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (UserLearningTask task : taskPage.getRecords()) {
            LearnerDTO learner = new LearnerDTO();
            learner.setUserId(task.getUserId());
            
            // 设置用户信息
            User user = userMap.get(task.getUserId());
            if (user != null) {
                learner.setUserName(user.getNickname());
                learner.setEmployeeNo(user.getEmployeeNo());
            }
            
            learner.setSource(task.getSource());
            learner.setStatus(task.getStatus());
            learner.setStudyDuration(task.getStudyDuration());
            learner.setProgress(task.getProgress());
            String currentTaskId = task.getAttribute(AttributeKey.currentTaskId);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(currentTaskId)) {
                Long id = Long.valueOf(currentTaskId);
                learner.setCurrentStageName(stageNameMap.get(id));
            }
            learner.setEarnedCredit(task.getEarnedCredit());
            learner.setCompletedRequiredTaskCount(task.getAttribute(AttributeKey.completedRequiredCount, Integer.class));
            learner.setCompletedElectiveTaskCount(task.getAttribute(AttributeKey.completedElectiveCount, Integer.class));
            learner.setCompletedStageCount(learner.getCompletedRequiredTaskCount() + learner.getCompletedElectiveTaskCount());
            learner.setCertificateIssued(task.getCertificateIssued() != null && task.getCertificateIssued() == 1);

            // 设置时间字段
            if (task.getAssignTime() != null) {
                learner.setAssignTime(dateFormat.format(task.getAssignTime()));
            }
            
            if (task.getDeadline() != null) {
                learner.setDeadline(dateFormat.format(task.getDeadline()));
            }
            
            if (task.getCompletionTime() != null) {
                learner.setCompletionTime(dateFormat.format(task.getCompletionTime()));
            }
            
            learnerList.add(learner);
        }
        
        response.setList(learnerList);
        return response;
    }

    /**
     * 解析扩展属性
     *
     * @param attributesJson 扩展属性JSON字符串
     * @return 扩展属性Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAttributes(String attributesJson) {
        if (!StringUtils.hasText(attributesJson)) {
            return new HashMap<>();
        }
        
        try {
            // 使用Jackson或Gson等JSON库解析
            // 这里简化处理，实际项目中应该使用JSON库
            return new HashMap<>();
        } catch (Exception e) {
            log.error("解析扩展属性失败", e);
            return new HashMap<>();
        }
    }


}
