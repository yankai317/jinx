package com.learn.service.map.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.map.LearningMapLearnerDetailDTO;
import com.learn.dto.map.LearningMapLearnersRequest;
import com.learn.dto.map.LearningMapLearnersResponse;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.map.LearnMapUserService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习地图用户服务实现类
 */
@Service
@Slf4j
public class LearnMapUserServiceImpl implements LearnMapUserService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;

    
    /**
     * 从attributes中获取整数属性值
     *
     * @param attributes 属性Map
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    private int getIntAttribute(Map<String, Object> attributes, String key, int defaultValue) {
        if (attributes == null || !attributes.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }return defaultValue;
    }
    
    /**
     * 解析attributes字符串为Map
     *
     * @param attributes 属性字符串
     * @return 属性Map
     */
    private Map<String, Object> parseAttributes(String attributes) {
        if (StringUtils.isBlank(attributes)) {
            return new HashMap<>();
        }
        
        try {
            return Json.fromJson(attributes, Map.class);
        } catch (Exception e) {
            log.warn("解析attributes失败: {}", attributes, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 获取用户部门名称
     *
     * @param user 用户信息
     * @return 部门名称
     */
    private String getDepartmentName(User user) {
        // 实际项目中可能需要查询用户部门信息
        // 这里简化处理，返回空字符串
        return "";
    }
    
    /**
     * 根据关键词查询用户ID列表
     *
     * @param keyword 关键词(用户名/工号)
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByKeyword(String keyword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIsDel, 0); // 未删除的用户
        queryWrapper.and(wrapper -> wrapper
                .like(User::getNickname, keyword)
                .or()
                .like(User::getEmployeeNo, keyword));
        
        List<User> users = userMapper.selectList(queryWrapper);
        return users.stream().map(User::getUserId).collect(Collectors.toList());
    }
    
    /**
     * 获取用户信息Map
     *
     * @param userIds 用户ID列表
     * @return 用户ID到用户信息的映射
     */
    private Map<Long, User> getUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getUserId, userIds);
        queryWrapper.eq(User::getIsDel, 0); // 未删除的用户
        
        List<User> users = userMapper.selectList(queryWrapper);
        return users.stream().collect(Collectors.toMap(User::getUserId, user -> user, (v1, v2) -> v1));
    }
    
    /**
     * 获取学习地图的所有阶段
     *
     * @param mapId 学习地图ID
     * @return 阶段列表
     */
    private List<LearningMapStage> getStagesByMapId(Long mapId) {
        LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMapStage::getMapId, mapId);
        queryWrapper.eq(LearningMapStage::getIsDel, 0); // 未删除的阶段
        queryWrapper.orderByAsc(LearningMapStage::getStageOrder); // 按阶段顺序排序
        
        return learningMapStageMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取学习地图学员学习详情
     *
     * @param mapId  学习地图ID
     * @param userId 用户ID
     * @return 学习详情
     */
    @Override
    public LearningMapLearnerDetailDTO getLearningMapLearnerDetail(Long mapId, Long userId) {
        log.info("获取学习地图学员学习详情, mapId: {}, userId: {}", mapId, userId);
        
        // 查询用户在学习地图中的学习记录
        LambdaQueryWrapper<UserLearningTask> taskQuery = new LambdaQueryWrapper<>();
        taskQuery.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP);
        taskQuery.eq(UserLearningTask::getBizId, mapId);
        taskQuery.eq(UserLearningTask::getUserId, userId);
        taskQuery.eq(UserLearningTask::getIsDel, 0); // 未删除的记录
        
        UserLearningTask mapTask = userLearningTaskMapper.selectOne(taskQuery);
        if (mapTask == null) {
            log.warn("未找到用户在学习地图中的学习记录, mapId: {}, userId: {}", mapId, userId);
            return null;
        }
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("未找到用户信息, userId: {}", userId);
            return null;
        }
        
        // 构建响应DTO
        LearningMapLearnerDetailDTO detailDTO = new LearningMapLearnerDetailDTO();
        detailDTO.setUserId(userId);
        detailDTO.setUserName(user.getNickname());
        detailDTO.setDepartment(getDepartmentName(user));
        detailDTO.setSource(mapTask.getSource());
        detailDTO.setStatus(mapTask.getStatus());
        detailDTO.setStudyDuration(mapTask.getStudyDuration());
        
        // 从attributes中获取扩展属性
        Map<String, Object> attributes = parseAttributes(mapTask.getAttributes());
        
        // 设置当前学习阶段ID
        Long currentStageId = mapTask.getParentId();
        if (currentStageId != null && BizType.MAP_STAGE.equals(mapTask.getParentType())) {
            detailDTO.setCurrentStageId(currentStageId);
        }
        
        // 设置已完成阶段数、必修任务数、选修任务数和学分
        detailDTO.setCompletedStageCount(getIntAttribute(attributes, "completedStageCount", 0));
        detailDTO.setCompletedRequiredTaskCount(getIntAttribute(attributes, "completedRequiredTaskCount", 0));
        detailDTO.setCompletedElectiveTaskCount(getIntAttribute(attributes, "completedOptionalTaskCount", 0));
        detailDTO.setEarnedCredit(mapTask.getEarnedCredit() != null ? mapTask.getEarnedCredit().intValue() : 0);
        detailDTO.setCertificateIssued(mapTask.getCertificateIssued() != null && mapTask.getCertificateIssued() == 1);
        
        // 格式化日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (mapTask.getAssignTime() != null) {
            detailDTO.setAssignTime(dateFormat.format(mapTask.getAssignTime()));
        }
        if (mapTask.getStartTime() != null) {
            detailDTO.setStartTime(dateFormat.format(mapTask.getStartTime()));
        }
        if (mapTask.getDeadline() != null) {
            detailDTO.setDeadline(dateFormat.format(mapTask.getDeadline()));
        }
        if (mapTask.getCompletionTime() != null) {
            detailDTO.setCompletionTime(dateFormat.format(mapTask.getCompletionTime()));
        }
        if (mapTask.getLastStudyTime() != null) {
            detailDTO.setLastStudyTime(dateFormat.format(mapTask.getLastStudyTime()));
        }
        
        // 查询学习地图的所有阶段
        List<LearningMapStage> stages = getStagesByMapId(mapId);
        
        // 如果有当前阶段ID，设置当前阶段名称
        if (currentStageId != null) {
            for (LearningMapStage stage : stages) {
                if (stage.getId().equals(currentStageId)) {
                    detailDTO.setCurrentStageName(stage.getName());
                    break;
                }
            }
        }
        
        // 查询用户在各阶段的学习记录
        List<LearningMapLearnerDetailDTO.StageRecord> stageRecords = new ArrayList<>();
        
        for (LearningMapStage stage : stages) {
            LearningMapLearnerDetailDTO.StageRecord stageRecord = new LearningMapLearnerDetailDTO.StageRecord();
            stageRecord.setStageId(stage.getId());
            stageRecord.setStageName(stage.getName());
            stageRecord.setStageOrder(stage.getStageOrder());
            stageRecord.setCredit(stage.getCredit());
            stageRecord.setCertificateId(stage.getCertificateId());
            
            // 查询该阶段的学习记录
            UserLearningTask stageTask = getStageTask(userId, stage.getId());
            
            // 设置阶段状态
            stageRecord.setStatus(stageTask != null ? stageTask.getStatus() : LearningStatus.NOT_STARTED);
            
            // 查询阶段下的任务内容关联信息
            List<ContentRelation> taskContents = getTaskContents(stage.getId());
            
            // 构建任务学习记录列表
            List<LearningMapLearnerDetailDTO.TaskRecord> taskRecordDTOs = new ArrayList<>();
            
            for (ContentRelation content : taskContents) {
                // 查询任务的学习记录
                UserLearningTask taskRecord = getTaskRecord(userId, content.getContentType(), content.getContentId());
                
                LearningMapLearnerDetailDTO.TaskRecord taskRecordDTO = new LearningMapLearnerDetailDTO.TaskRecord();
                taskRecordDTO.setTaskId(content.getId());
                taskRecordDTO.setType(content.getContentType());
                taskRecordDTO.setTitle(getTaskTitle(content));
                
                if (taskRecord != null) {
                    taskRecordDTO.setStatus(taskRecord.getStatus());
                    taskRecordDTO.setStudyDuration(taskRecord.getStudyDuration());
                    taskRecordDTO.setProgress(taskRecord.getProgress());
                    taskRecordDTO.setScore(taskRecord.getScore());
                    taskRecordDTO.setPassStatus(taskRecord.getPassStatus());
                    
                    // 格式化日期
                    if (taskRecord.getStartTime() != null) {
                        taskRecordDTO.setStartTime(dateFormat.format(taskRecord.getStartTime()));
                    }
                    if (taskRecord.getLastStudyTime() != null) {
                        taskRecordDTO.setLastStudyTime(dateFormat.format(taskRecord.getLastStudyTime()));
                    }
                    if (taskRecord.getCompletionTime() != null) {
                        taskRecordDTO.setCompletionTime(dateFormat.format(taskRecord.getCompletionTime()));
                    }
                } else {
                    // 如果没有学习记录，设置默认值
                    taskRecordDTO.setStatus(LearningStatus.NOT_STARTED); // 未开始
                    taskRecordDTO.setStudyDuration(0);
                    taskRecordDTO.setProgress(0);
                    taskRecordDTO.setScore(0);
                    taskRecordDTO.setPassStatus(0);
                }
                
                taskRecordDTOs.add(taskRecordDTO);
            }
            
            // 按任务ID排序
            taskRecordDTOs.sort(Comparator.comparing(LearningMapLearnerDetailDTO.TaskRecord::getTaskId));
            stageRecord.setTaskRecords(taskRecordDTOs);
            stageRecords.add(stageRecord);
        }
        
        // 按阶段顺序排序
        stageRecords.sort(Comparator.comparing(LearningMapLearnerDetailDTO.StageRecord::getStageOrder));
        detailDTO.setStageRecords(stageRecords);
        
        return detailDTO;
    }
    
    /**
     * 获取阶段的学习记录
     *
     * @param userId  用户ID
     * @param stageId 阶段ID
     * @return 阶段学习记录
     */
    private UserLearningTask getStageTask(Long userId, Long stageId) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId);
        queryWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE);
        queryWrapper.eq(UserLearningTask::getBizId, stageId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        
        return userLearningTaskMapper.selectOne(queryWrapper);
    }
    
    /**
     * 获取任务的学习记录
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 任务学习记录
     */
    private UserLearningTask getTaskRecord(Long userId, String contentType, Long contentId) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId);
        queryWrapper.eq(UserLearningTask::getBizType, contentType);
        queryWrapper.eq(UserLearningTask::getBizId, contentId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        
        return userLearningTaskMapper.selectOne(queryWrapper);
    }
    
    /**
     * 获取阶段下的任务内容关联信息
     *
     * @param stageId 阶段ID
     * @return 任务内容关联列表
     */
    private List<ContentRelation> getTaskContents(Long stageId) {
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, stageId);
        queryWrapper.eq(ContentRelation::getBizType, "MAP_STAGE"); // 学习地图阶段
        queryWrapper.eq(ContentRelation::getIsDel, 0); // 未删除的记录
        queryWrapper.orderByAsc(ContentRelation::getSortOrder); // 按排序顺序排序
        
        return contentRelationMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取任务标题
     *
     * @param contentRelation 内容关联信息
     * @return 任务标题
     */
    private String getTaskTitle(ContentRelation contentRelation) {
        // 从attribute字段中提取标题信息，或者使用其他方式获取任务标题
        // 这里简化处理，实际项目中可能需要更复杂的逻辑
        String attribute = contentRelation.getAttributes();
        if (StringUtils.isNotBlank(attribute) && attribute.contains("title")) {
            // 假设attribute是JSON格式，包含title字段
            try {
                Map<String, Object> attributes = parseAttributes(attribute);
                if (attributes.containsKey("title")) {
                    return attributes.get("title").toString();
                }
            } catch (Exception e) {
                log.warn("解析任务标题失败", e);
            }
        }
        
        // 如果无法从attribute中获取标题，则使用内容类型作为标题
        return contentRelation.getContentType() + "-" + contentRelation.getContentId();
    }
}
