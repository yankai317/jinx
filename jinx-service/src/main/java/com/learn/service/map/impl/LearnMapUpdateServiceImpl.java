package com.learn.service.map.impl;

import com.google.common.collect.Lists;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.map.LearningMapUpdateRequest;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.dingtalk.DingTalkDepartmentSyncService;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.dto.CommonRangeUpdateRequest;
import com.learn.service.dto.CommonRangeUpdateResponse;
import com.learn.service.map.LearnMapUpdateService;
import com.learn.service.operation.OperationLogService;
import com.learn.common.dto.UserTokenInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习地图更新服务实现类
 */
@Service
@Slf4j
public class LearnMapUpdateServiceImpl implements LearnMapUpdateService {

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired(required = false)
    private DingTalkDepartmentSyncService dingTalkSyncService;
    
    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private RangeSetService rangeSetService;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BIZ_TYPE_MAP_STAGE = "MAP_STAGE";

    /**
     * 更新学习地图
     *
     * @param request 更新学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateLearningMap(LearningMapUpdateRequest request, Long userId, String userName) {
        // 参数校验
        validateUpdateRequest(request);

        // 查询学习地图
        LearningMap learningMap = learningMapMapper.selectById(request.getId());
        if (learningMap == null || learningMap.getIsDel() == 1) {
            throw new CommonException("学习地图不存在");
        }

        // 更新学习地图基本信息
        updateLearningMapBasic(request, learningMap, userId, userName);

        // 更新学习地图阶段和任务
        updateLearningMapStagesAndTasks(request, learningMap, userId, userName);
        // 更新证书关联
        updateCertificateRelation(request, learningMap.getId(), userId, userName);

        // 更新学习地图可见范围和协同管理
        updateLearningMapVisibilityAndCollaborators(request, learningMap.getId(), userId, userName);

        // 如果需要创建钉钉群
        if (request.getDingtalkGroup() != null && request.getDingtalkGroup() == 1 
                && StringUtils.isBlank(learningMap.getDingtalkGroupId())) {
            createDingtalkGroup(learningMap);
        }
        
        // 记录操作日志
        try {
            UserTokenInfo userInfo = new UserTokenInfo();
            userInfo.setUserId(userId);
            
            // 构建操作详情JSON
            Map<String, Object> operationDetail = new HashMap<>();
            operationDetail.put("id", request.getId());
            operationDetail.put("title", learningMap.getName());
            operationDetail.put("cover", learningMap.getCover());
            operationDetail.put("introduction", learningMap.getIntroduction());
            operationDetail.put("creditRule", learningMap.getCreditRule());
            operationDetail.put("requiredCredit", learningMap.getRequiredCredit());
            operationDetail.put("electiveCredit", learningMap.getElectiveCredit());
            operationDetail.put("categoryIds", learningMap.getCategoryIds());
            operationDetail.put("certificateRule", learningMap.getCertificateRule());
            operationDetail.put("certificateId", learningMap.getCertificateId());
            operationDetail.put("theme", learningMap.getTheme());
            
            String operationDetailJson = objectMapper.writeValueAsString(operationDetail);
            operationLogService.recordUpdateOperation(learningMap.getId(), BizType.LEARNING_MAP, operationDetailJson, userInfo);
        } catch (Exception e) {
            log.error("记录更新学习地图操作日志失败", e);
            // 不影响主流程，继续执行
        }

        return true;
    }

    /**
     * 校验更新学习地图请求
     *
     * @param request 更新学习地图请求
     */
    private void validateUpdateRequest(LearningMapUpdateRequest request) {
        // 校验必填字段
        if (request.getId() == null || request.getId() <= 0) {
            throw new CommonException("地图ID不能为空");
        }

        // 校验证书规则
        if (request.getCertificateRule() != null && request.getCertificateRule() == 1) {
            if (request.getCertificateId() == null) {
                throw new CommonException("整体发放证书时，证书ID不能为空");
            }
        }

        // 校验阶段
        if (request.getStages() != null && !request.getStages().isEmpty()) {
            for (LearningMapUpdateRequest.StageRequest stage : request.getStages()) {
                if (StringUtils.isBlank(stage.getName())) {
                    throw new CommonException("阶段名称不能为空");
                }

                if (stage.getStageOrder() == null) {
                    throw new CommonException("阶段顺序不能为空");
                }

                // 校验开放类型
                if (stage.getOpenType() != null) {
                    if (stage.getOpenType() == 1) {
                        // 固定时间
                        if (StringUtils.isBlank(stage.getStartTime()) || StringUtils.isBlank(stage.getEndTime())) {
                            throw new CommonException("固定开放时间模式下，开始时间和结束时间不能为空");
                        }
                    } else if (stage.getOpenType() == 2) {
                        // 学习期限
                        if (stage.getDurationDays() == null) {
                            throw new CommonException("学习期限模式下，学习期限天数不能为空");
                        }
                    }
                }

                // 校验学分规则
                if (request.getCreditRule() != null && request.getCreditRule() == 1) {
                    if (stage.getCredit() == null) {
                        throw new CommonException("按阶段发放学分时，阶段学分不能为空");
                    }
                }

                // 校验证书规则
                if (request.getCertificateRule() != null && request.getCertificateRule() == 2) {
                    if (stage.getCertificateId() == null) {
                        throw new CommonException("按阶段发放证书时，阶段证书ID不能为空");
                    }
                }

                // 校验任务
                if (stage.getTasks() != null && !stage.getTasks().isEmpty()) {
                    for (LearningMapUpdateRequest.TaskRequest task : stage.getTasks()) {
                        if (StringUtils.isBlank(task.getType())) {
                            throw new CommonException("任务类型不能为空");
                        }

                        if (task.getContentId() == null) {
                            throw new CommonException("任务内容ID不能为空");
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新学习地图基本信息
     *
     * @param request 更新学习地图请求
     * @param learningMap 学习地图实体
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void updateLearningMapBasic(LearningMapUpdateRequest request, LearningMap learningMap, Long userId, String userName) {
        boolean isModified = false;
        
        // 更新名称
        if (StringUtils.isNotBlank(request.getName()) && !request.getName().equals(learningMap.getName())) {
            learningMap.setName(request.getName());
            isModified = true;
        }
        
        // 更新封面
        if (StringUtils.isNotBlank(request.getCover()) && !request.getCover().equals(learningMap.getCover())) {
            learningMap.setCover(request.getCover());
            isModified = true;
        }
        
        // 更新简介
        if (StringUtils.isNotBlank(request.getIntroduction()) && !request.getIntroduction().equals(learningMap.getIntroduction())) {
            learningMap.setIntroduction(request.getIntroduction());
            isModified = true;
        }
        
        // 更新学分规则
        if (request.getCreditRule() != null && !request.getCreditRule().equals(learningMap.getCreditRule())) {
            learningMap.setCreditRule(request.getCreditRule());
            isModified = true;
        }
        
        // 更新必修学分
        if (request.getRequiredCredit() != null && !request.getRequiredCredit().equals(learningMap.getRequiredCredit())) {
            learningMap.setRequiredCredit(request.getRequiredCredit());
            isModified = true;
        }
        
        // 更新选修学分
        if (request.getElectiveCredit() != null && !request.getElectiveCredit().equals(learningMap.getElectiveCredit())) {
            learningMap.setElectiveCredit(request.getElectiveCredit());
            isModified = true;
        }
        
        // 更新分类IDs
        if (request.getCategoryIds() != null) {
            learningMap.setCategoryIds(request.getCategoryIds());
            isModified = true;
        }
        
        // 更新证书规则
        if (request.getCertificateRule() != null && !request.getCertificateRule().equals(learningMap.getCertificateRule())) {
            learningMap.setCertificateRule(request.getCertificateRule());
            isModified = true;
        }
        
        // 更新证书ID
        if (request.getCertificateId() != null && !request.getCertificateId().equals(learningMap.getCertificateId())) {
            learningMap.setCertificateId(request.getCertificateId());
            isModified = true;
        }
        
        // 更新钉钉群
        if (request.getDingtalkGroup() != null && !request.getDingtalkGroup().equals(learningMap.getDingtalkGroup())) {
            learningMap.setDingtalkGroup(request.getDingtalkGroup());
            isModified = true;
        }
        
        // 更新开放开始时间
        if (StringUtils.isNotBlank(request.getStartTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startTime = sdf.parse(request.getStartTime());
                if (learningMap.getStartTime() == null || !startTime.equals(learningMap.getStartTime())) {
                    learningMap.setStartTime(startTime);
                    isModified = true;
                }
            } catch (ParseException e) {
                log.error("解析开始时间失败", e);
                throw new CommonException("开始时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
            }
        }
        
        // 更新开放结束时间
        if (StringUtils.isNotBlank(request.getEndTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date endTime = sdf.parse(request.getEndTime());
                if (learningMap.getEndTime() == null || !endTime.equals(learningMap.getEndTime())) {
                    learningMap.setEndTime(endTime);
                    isModified = true;
                }
            } catch (ParseException e) {
                log.error("解析结束时间失败", e);
                throw new CommonException("结束时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
            }
        }
        
        // 更新解锁方式
        if (request.getUnlockMode() != null && !request.getUnlockMode().equals(learningMap.getUnlockMode())) {
            learningMap.setUnlockMode(request.getUnlockMode());
            isModified = true;
        }
        
        // 更新主题
        if (StringUtils.isNotBlank(request.getTheme()) && !request.getTheme().equals(learningMap.getTheme())) {
            learningMap.setTheme(request.getTheme());
            isModified = true;
        }
        
        // 如果有修改，更新更新人信息和更新时间
        if (isModified) {
            learningMap.setUpdaterId(userId);
            learningMap.setUpdaterName(userName);
            learningMap.setGmtModified(new Date());
            
            // 更新数据库
            learningMapMapper.updateById(learningMap);
        }
    }

    /**
     * 更新学习地图阶段和任务
     *
     * @param request 更新学习地图请求
     * @param learningMap 学习地图实体
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void updateLearningMapStagesAndTasks(LearningMapUpdateRequest request, LearningMap learningMap, Long userId, String userName) {
        // 删除阶段
        if (request.getDeleteStageIds() != null && !request.getDeleteStageIds().isEmpty()) {
            for (Long stageId : request.getDeleteStageIds()) {
                // 逻辑删除阶段
                LearningMapStage stage = learningMapStageMapper.selectById(stageId);
                if (stage != null && stage.getMapId().equals(learningMap.getId()) && stage.getIsDel() == 0) {
                    stage.setIsDel(1);
                    stage.setUpdaterId(userId);
                    stage.setUpdaterName(userName);
                    stage.setGmtModified(new Date());
                    learningMapStageMapper.updateById(stage);
                    
                    // 逻辑删除阶段下的所有任务
                    deleteTasksByStageId(stageId, userId, userName);
                }
            }
        }
        
        // 删除任务
        if (request.getDeleteTaskIds() != null && !request.getDeleteTaskIds().isEmpty()) {
            for (Long taskId : request.getDeleteTaskIds()) {
                ContentRelation task = contentRelationMapper.selectById(taskId);
                if (task != null && BIZ_TYPE_MAP_STAGE.equals(task.getBizType()) && task.getIsDel() == 0) {
                    // 确认任务所属的阶段是否属于当前地图
                    LearningMapStage stage = learningMapStageMapper.selectById(task.getBizId());
                    if (stage != null && stage.getMapId().equals(learningMap.getId()) && stage.getIsDel() == 0) {
                        task.setIsDel(1);
                        task.setUpdaterId(userId);
                        task.setUpdaterName(userName);
                        task.setGmtModified(new Date());
                        contentRelationMapper.updateById(task);
                    }
                }
            }
        }
        
        // 更新或新增阶段和任务
        if (request.getStages() != null && !request.getStages().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            
            for (LearningMapUpdateRequest.StageRequest stageRequest : request.getStages()) {
                if (stageRequest.getId() != null) {
                    // 更新阶段
                    LearningMapStage stage = learningMapStageMapper.selectById(stageRequest.getId());
                    if (stage != null && stage.getMapId().equals(learningMap.getId()) && stage.getIsDel() == 0) {
                        boolean isModified = false;
                        
                        // 更新阶段名称
                        if (StringUtils.isNotBlank(stageRequest.getName()) && !stageRequest.getName().equals(stage.getName())) {
                            stage.setName(stageRequest.getName());
                            isModified = true;
                        }
                        
                        // 更新阶段顺序
                        if (stageRequest.getStageOrder() != null && !stageRequest.getStageOrder().equals(stage.getStageOrder())) {
                            stage.setStageOrder(stageRequest.getStageOrder());
                            isModified = true;
                        }
                        
                        // 更新开放类型
                        if (stageRequest.getOpenType() != null && !stageRequest.getOpenType().equals(stage.getOpenType())) {
                            stage.setOpenType(stageRequest.getOpenType());
                            isModified = true;
                        }
                        
                        // 更新开放时间
                        if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 1) {
                            try {
                                if (StringUtils.isNotBlank(stageRequest.getStartTime())) {
                                    Date startTime = sdf.parse(stageRequest.getStartTime());
                                    if (stage.getStartTime() == null || !startTime.equals(stage.getStartTime())) {
                                        stage.setStartTime(startTime);
                                        isModified = true;
                                    }
                                }
                                
                                if (StringUtils.isNotBlank(stageRequest.getEndTime())) {
                                    Date endTime = sdf.parse(stageRequest.getEndTime());
                                    if (stage.getEndTime() == null || !endTime.equals(stage.getEndTime())) {
                                        stage.setEndTime(endTime);
                                        isModified = true;
                                    }
                                }
                            } catch (ParseException e) {
                                log.error("解析阶段开放时间失败", e);
                                throw new CommonException("阶段开放时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
                            }
                        }
                        
                        // 更新学习期限
                        if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 2) {
                            if (stageRequest.getDurationDays() != null && !stageRequest.getDurationDays().equals(stage.getDurationDays())) {
                                stage.setDurationDays(stageRequest.getDurationDays());
                                isModified = true;
                            }
                        }
                        
                        // 更新阶段学分
                        if (learningMap.getCreditRule() == 1) {
                            if (stageRequest.getCredit() != null && !stageRequest.getCredit().equals(stage.getCredit())) {
                                stage.setCredit(stageRequest.getCredit());
                                isModified = true;
                            }
                        }
                        
                        // 更新阶段证书
                        if (learningMap.getCertificateRule() == 2) {
                            if (stageRequest.getCertificateId() != null && !stageRequest.getCertificateId().equals(stage.getCertificateId())) {
                                stage.setCertificateId(stageRequest.getCertificateId());
                                isModified = true;
                            }
                        }
                        
                        // 如果有修改，更新更新人信息和更新时间
                        if (isModified) {
                            stage.setUpdaterId(userId);
                            stage.setUpdaterName(userName);
                            stage.setGmtModified(now);
                            
                            // 更新数据库
                            learningMapStageMapper.updateById(stage);
                        }
                        
                        // 更新任务
                        updateLearningMapTasks(stageRequest.getTasks(), stage.getId(), userId, userName);
                    }
                } else {
                    // 创建新阶段
                    LearningMapStage stage = new LearningMapStage();
                    stage.setMapId(learningMap.getId());
                    stage.setName(stageRequest.getName());
                    stage.setStageOrder(stageRequest.getStageOrder());
                    stage.setOpenType(stageRequest.getOpenType() != null ? stageRequest.getOpenType() : 0);
                    
                    // 设置开放时间
                    if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 1) {
                        try {
                            if (StringUtils.isNotBlank(stageRequest.getStartTime())) {
                                stage.setStartTime(sdf.parse(stageRequest.getStartTime()));
                            }
                            if (StringUtils.isNotBlank(stageRequest.getEndTime())) {
                                stage.setEndTime(sdf.parse(stageRequest.getEndTime()));
                            }
                        } catch (ParseException e) {
                            log.error("解析阶段开放时间失败", e);
                            throw new CommonException("阶段开放时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
                        }
                    }
                    
                    // 设置学习期限
                    if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 2) {
                        stage.setDurationDays(stageRequest.getDurationDays());
                    }
                    
                    // 设置阶段学分
                    if (learningMap.getCreditRule() == 1) {
                        stage.setCredit(stageRequest.getCredit());
                    }
                    
                    // 设置阶段证书
                    if (learningMap.getCertificateRule() == 2) {
                        stage.setCertificateId(stageRequest.getCertificateId());
                    }
                    
                    // 设置创建人信息
                    stage.setCreatorId(userId);
                    stage.setCreatorName(userName);
                    stage.setUpdaterId(userId);
                    stage.setUpdaterName(userName);
                    
                    // 设置时间信息
                    stage.setGmtCreate(now);
                    stage.setGmtModified(now);
                    stage.setIsDel(0);
                    
                    // 插入数据库
                    learningMapStageMapper.insert(stage);
                    
                    // 创建任务
                    createLearningMapTasks(stageRequest.getTasks(), stage.getId(), userId, userName);
                }
            }
        }
    }

    /**
     * 删除阶段下的所有任务
     *
     * @param stageId 阶段ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void deleteTasksByStageId(Long stageId, Long userId, String userName) {
        // 构建查询条件
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("biz_id", stageId);
        queryMap.put("biz_type", BIZ_TYPE_MAP_STAGE);
        queryMap.put("is_del", 0);
        
        // 查询阶段下的所有任务
        List<ContentRelation> tasks = contentRelationMapper.selectByMap(queryMap);
        if (!CollectionUtils.isEmpty(tasks)) {
            Date now = new Date();
            for (ContentRelation task : tasks) {
                task.setIsDel(1);
                task.setUpdaterId(userId);
                task.setUpdaterName(userName);
                task.setGmtModified(now);
                contentRelationMapper.updateById(task);
            }
        }
    }

    /**
     * 更新学习地图任务
     *
     * @param tasks 任务请求列表
     * @param stageId 阶段ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void updateLearningMapTasks(List<LearningMapUpdateRequest.TaskRequest> tasks, Long stageId, Long userId, String userName) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        Date now = new Date();
        
        for (LearningMapUpdateRequest.TaskRequest taskRequest : tasks) {
            if (taskRequest.getId() != null) {
                // 更新任务
                ContentRelation task = contentRelationMapper.selectById(taskRequest.getId());
                if (task != null && task.getBizId().equals(stageId) && BIZ_TYPE_MAP_STAGE.equals(task.getBizType()) && task.getIsDel() == 0) {
                    boolean isModified = false;
                    
                    // 更新任务类型
                    if (StringUtils.isNotBlank(taskRequest.getType()) && !taskRequest.getType().equals(task.getContentType())) {
                        task.setContentType(taskRequest.getType());
                        isModified = true;
                    }
                    
                    // 更新内容ID
                    if (taskRequest.getContentId() != null && !taskRequest.getContentId().equals(task.getContentId())) {
                        task.setContentId(taskRequest.getContentId());
                        isModified = true;
                    }
                    
                    // 更新是否必修
                    if (taskRequest.getIsRequired() != null) {
                        int isRequired = taskRequest.getIsRequired() ? 1 : 0;
                        if (isRequired != task.getIsRequired()) {
                            task.setIsRequired(isRequired);
                            isModified = true;
                        }
                    }
                    
                    // 更新排序序号
                    if (taskRequest.getSortOrder() != null && !taskRequest.getSortOrder().equals(task.getSortOrder())) {
                        task.setSortOrder(taskRequest.getSortOrder());
                        isModified = true;
                    }
                    
                    // 如果有修改，更新更新人信息和更新时间
                    if (isModified) {
                        task.setUpdaterId(userId);
                        task.setUpdaterName(userName);
                        task.setGmtModified(now);
                        
                        // 更新数据库
                        contentRelationMapper.updateById(task);
                    }
                }
            } else {
                // 创建新任务
                ContentRelation task = new ContentRelation();
                task.setBizId(stageId);
                task.setBizType(BIZ_TYPE_MAP_STAGE);
                task.setContentType(taskRequest.getType());
                task.setContentId(taskRequest.getContentId());
                task.setSortOrder(taskRequest.getSortOrder() != null ? taskRequest.getSortOrder() : 0);
                task.setIsRequired(taskRequest.getIsRequired() != null && taskRequest.getIsRequired() ? 1 : 0);
                
                // 设置创建人信息
                task.setCreatorId(userId);
                task.setCreatorName(userName);
                task.setUpdaterId(userId);
                task.setUpdaterName(userName);
                
                // 设置时间信息
                task.setGmtCreate(now);
                task.setGmtModified(now);
                task.setIsDel(0);
                
                // 插入数据库
                contentRelationMapper.insert(task);
            }
        }
    }

    /**
     * 创建学习地图任务
     *
     * @param tasks 任务请求列表
     * @param stageId 阶段ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void createLearningMapTasks(List<LearningMapUpdateRequest.TaskRequest> tasks, Long stageId, Long userId, String userName) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        Date now = new Date();
        
        for (LearningMapUpdateRequest.TaskRequest taskRequest : tasks) {
            ContentRelation task = new ContentRelation();
            task.setBizId(stageId);
            task.setBizType(BIZ_TYPE_MAP_STAGE);
            task.setContentType(taskRequest.getType());
            task.setContentId(taskRequest.getContentId());
            task.setSortOrder(taskRequest.getSortOrder() != null ? taskRequest.getSortOrder() : 0);
            task.setIsRequired(taskRequest.getIsRequired() != null && taskRequest.getIsRequired() ? 1 : 0);
            
            // 设置创建人信息
            task.setCreatorId(userId);
            task.setCreatorName(userName);
            task.setUpdaterId(userId);
            task.setUpdaterName(userName);
            
            // 设置时间信息
            task.setGmtCreate(now);
            task.setGmtModified(now);
            task.setIsDel(0);
            
            // 插入数据库
            contentRelationMapper.insert(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCreator(List<Long> courseIds, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName) {
        log.info("批量更新课程创建人，课程ID列表：{}，新创建人ID：{}，操作人：{}", courseIds, newCreatorId, operatorName);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(courseIds)) {
            return false;
        }

        // 执行批量更新
        learningMapMapper.batchUpdateCreator(courseIds, newCreatorId, newCreatorName, operatorId, operatorName);

        log.info("批量更新课程创建人完成");
        return true;
    }

    /**
     * 更新学习地图可见范围和协同管理
     *
     * @param request 更新学习地图请求
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void updateLearningMapVisibilityAndCollaborators(LearningMapUpdateRequest request, Long mapId, Long userId, String userName) {
        request.setOperatorId(userId);
        request.setOperatorName(userName);
        // 更新可见范围
        rangeSetService.updateVisibilityRange(BizType.LEARNING_MAP, mapId, request);
        
        // 更新协同管理
        rangeSetService.updateCollaborators(BizType.LEARNING_MAP, mapId, request);
    }



    /**
     * 创建钉钉群
     * 注意：由于DingTalkSyncService接口中没有createGroup方法，这里只记录日志，不实际创建钉钉群
     *
     * @param learningMap 学习地图实体
     */
    private void createDingtalkGroup(LearningMap learningMap) {
        try {
            log.info("尝试创建钉钉群: {}", learningMap.getName());
            // 由于DingTalkSyncService接口中没有createGroup方法，这里只记录日志，不实际创建钉钉群
            // 如果后续DingTalkSyncService接口添加了createGroup方法，可以取消注释下面的代码
            // String groupId = dingTalkSyncService.createGroup(learningMap.getName(), "学习地图：" + learningMap.getName());
            // if (StringUtils.isNotBlank(groupId)) {
            //     learningMap.setDingtalkGroupId(groupId);
            //     learningMapMapper.updateById(learningMap);
            // }
        } catch (Exception e) {
            log.error("创建钉钉群失败", e);
            // 不抛出异常，继续执行后续逻辑
        }
    }


    /**
     * 更新证书关联
     * 
     * 1. 如果地图阶段或者地图已存在证书id的content_relation，则更新relation
     * 2. 如果地图阶段或者地图已存在证书id，本次更新不存在证书id，则删除relation
     * 3. 如果地图阶段或者地图不存在证书id，本次更新存在证书id，则创建relation
     * 4. 不需要先批量删除，再批量新增
     *
     * @param request 更新学习地图请求
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void updateCertificateRelation(LearningMapUpdateRequest request, Long mapId, Long userId, String userName) {
        Date now = new Date();
        
        // 处理地图整体证书关联
        updateMapCertificateRelation(request, mapId, userId, userName, now);
        
        // 处理阶段证书关联
        updateStageCertificateRelation(request, userId, userName, now);
    }
    
    /**
     * 更新地图整体证书关联
     *
     * @param request 更新学习地图请求
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param now 当前时间
     */
    private void updateMapCertificateRelation(LearningMapUpdateRequest request, Long mapId, Long userId, String userName, Date now) {
        // 查询地图现有的证书关联
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("biz_id", mapId);
        queryMap.put("biz_type", BizType.LEARNING_MAP);
        queryMap.put("content_type", BizType.CERTIFICATE);
        queryMap.put("is_del", 0);
        
        List<ContentRelation> existingRelations = contentRelationMapper.selectByMap(queryMap);
        
        // 判断是否需要更新地图证书关联
        boolean hasExistingCertificate = !existingRelations.isEmpty();
        boolean needCertificate = request.getCertificateId() != null && request.getCertificateRule() == 1;
        
        if (hasExistingCertificate && needCertificate) {
            // 情况1: 已存在证书关联且需要更新
            ContentRelation existingRelation = existingRelations.get(0);
            Long existingCertificateId = existingRelation.getContentId();
            
            // 如果证书ID不同，则更新
            if (!existingCertificateId.equals(request.getCertificateId())) {
                existingRelation.setContentId(request.getCertificateId());
                existingRelation.setUpdaterId(userId);
                existingRelation.setUpdaterName(userName);
                existingRelation.setGmtModified(now);
                contentRelationMapper.updateById(existingRelation);
            }
        } else if (hasExistingCertificate && !needCertificate) {
            // 情况2: 已存在证书关联但不再需要
            ContentRelation existingRelation = existingRelations.get(0);
            existingRelation.setIsDel(1);
            existingRelation.setUpdaterId(userId);
            existingRelation.setUpdaterName(userName);
            existingRelation.setGmtModified(now);
            contentRelationMapper.updateById(existingRelation);
        } else if (!hasExistingCertificate && needCertificate) {
            // 情况3: 不存在证书关联但需要创建
            ContentRelation certificateRelation = new ContentRelation();
            certificateRelation.setBizId(mapId);
            certificateRelation.setBizType(BizType.LEARNING_MAP);
            certificateRelation.setContentType(BizType.CERTIFICATE);
            certificateRelation.setContentId(request.getCertificateId());
            certificateRelation.setSortOrder(0);
            certificateRelation.setIsRequired(1);
            certificateRelation.setCreatorId(userId);
            certificateRelation.setCreatorName(userName);
            certificateRelation.setUpdaterId(userId);
            certificateRelation.setUpdaterName(userName);
            certificateRelation.setGmtCreate(now);
            certificateRelation.setGmtModified(now);
            certificateRelation.setIsDel(0);
            
            contentRelationMapper.insert(certificateRelation);
        }
    }
    
    /**
     * 更新阶段证书关联
     *
     * @param request 更新学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @param now 当前时间
     */
    private void updateStageCertificateRelation(LearningMapUpdateRequest request, Long userId, String userName, Date now) {
        // 只有当证书规则为按阶段发放(rule=2)且有阶段数据时才处理
        if (request.getCertificateRule() != null && request.getCertificateRule() == 2 && request.getStages() != null) {
            // 收集所有阶段ID和证书ID的映射关系
            Map<Long, Long> stageCertificateMap = new HashMap<>();
            for (LearningMapUpdateRequest.StageRequest stage : request.getStages()) {
                if (stage.getId() != null) {
                    stageCertificateMap.put(stage.getId(), stage.getCertificateId());
                }
            }
            
            // 获取所有阶段ID
            List<Long> stageIds = new ArrayList<>(stageCertificateMap.keySet());
            if (stageIds.isEmpty()) {
                return;
            }
            
            // 查询这些阶段现有的证书关联
            Map<String, Object> stageQueryMap = new HashMap<>();
            stageQueryMap.put("biz_type", BIZ_TYPE_MAP_STAGE);
            stageQueryMap.put("biz_id", stageIds);
            stageQueryMap.put("content_type", BizType.CERTIFICATE);
            stageQueryMap.put("is_del", 0);
            
            List<ContentRelation> existingStageCertificates = contentRelationMapper.selectByMap(stageQueryMap);
            
            // 过滤出当前地图阶段的证书关联，并按阶段ID分组
            Map<Long, ContentRelation> existingRelationMap = existingStageCertificates.stream()
                    .filter(relation -> stageIds.contains(relation.getBizId()))
                    .collect(Collectors.toMap(ContentRelation::getBizId, relation -> relation, (r1, r2) -> r1));
            
            // 处理每个阶段的证书关联
            for (Long stageId : stageIds) {
                ContentRelation existingRelation = existingRelationMap.get(stageId);
                Long newCertificateId = stageCertificateMap.get(stageId);
                
                if (existingRelation != null && newCertificateId != null) {
                    // 情况1: 已存在证书关联且需要更新
                    if (!existingRelation.getContentId().equals(newCertificateId)) {
                        existingRelation.setContentId(newCertificateId);
                        existingRelation.setUpdaterId(userId);
                        existingRelation.setUpdaterName(userName);
                        existingRelation.setGmtModified(now);
                        contentRelationMapper.updateById(existingRelation);
                    }
                } else if (existingRelation != null && newCertificateId == null) {
                    // 情况2: 已存在证书关联但不再需要
                    existingRelation.setIsDel(1);
                    existingRelation.setUpdaterId(userId);
                    existingRelation.setUpdaterName(userName);
                    existingRelation.setGmtModified(now);
                    contentRelationMapper.updateById(existingRelation);
                } else if (existingRelation == null && newCertificateId != null) {
                    // 情况3: 不存在证书关联但需要创建
                    // 获取阶段信息以获取排序顺序
                    LearningMapStage stage = learningMapStageMapper.selectById(stageId);
                    Integer sortOrder = stage != null ? stage.getStageOrder() : 0;
                    
                    ContentRelation stageCertificateRelation = new ContentRelation();
                    stageCertificateRelation.setBizId(stageId);
                    stageCertificateRelation.setBizType(BIZ_TYPE_MAP_STAGE);
                    stageCertificateRelation.setContentType(BizType.CERTIFICATE);
                    stageCertificateRelation.setContentId(newCertificateId);
                    stageCertificateRelation.setSortOrder(sortOrder);
                    stageCertificateRelation.setIsRequired(1);
                    stageCertificateRelation.setCreatorId(userId);
                    stageCertificateRelation.setCreatorName(userName);
                    stageCertificateRelation.setUpdaterId(userId);
                    stageCertificateRelation.setUpdaterName(userName);
                    stageCertificateRelation.setGmtCreate(now);
                    stageCertificateRelation.setGmtModified(now);
                    stageCertificateRelation.setIsDel(0);
                    contentRelationMapper.insert(stageCertificateRelation);
                }
                // 情况4: 不存在证书关联且不需要创建，无需操作
            }
        }
    }
}
