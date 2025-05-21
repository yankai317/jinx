package com.learn.service.map.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.dto.user.UserStudyRecordCreateDTO;
import com.learn.infrastructure.repository.entity.AssignmentDetail;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.mapper.AssignmentDetailMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.map.LearnMapAssignService;
import com.learn.service.operation.OperationLogService;
import com.learn.common.dto.UserTokenInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.constants.BizType;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 学习地图指派服务实现类
 */
@Service
@Slf4j
public class LearnMapAssignServiceImpl implements LearnMapAssignService {

    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;
    
    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 指派学习地图给用户
     * 1. 创建学习地图指派记录
     * 2. 为每个用户创建学习记录
     * 3. 记录操作日志
     *
     * @param request 指派学习地图请求
     * @return 指派学习地图响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignResponse assignLearningMap(AssignRequest request) {
        log.info("开始指派学习地图，学习地图ID：{}，用户数量：{}", request.getBizId(), request.getUserIds().size());
        
        // 1. 校验学习地图是否存在
        LearningMap learningMap = learningMapMapper.selectById(request.getBizId());
        if (learningMap == null || learningMap.getIsDel() == 1) {
            throw new IllegalArgumentException("学习地图不存在或已删除");
        }
        
//        // 2. 校验操作权限
//        checkOperationPermission(request.getBizId(), request.getOperatorId());
        
        // 3. 创建范围记录
        CommonRangeCreateResponse rangeResponse = createAssignmentRange(request.getBizId(), request.getOperatorId(), request.getOperatorName(), request);
        if (rangeResponse == null || !rangeResponse.getSuccess() || CollectionUtils.isEmpty(rangeResponse.getRangeIds())) {
            throw new RuntimeException("创建指派范围记录失败");
        }
        
        // 4. 解析截止时间
        Date deadline = null;
        if (request.getDeadline() != null && !request.getDeadline().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                deadline = sdf.parse(request.getDeadline());
            } catch (ParseException e) {
                log.error("解析截止时间失败", e);
                throw new IllegalArgumentException("截止时间格式不正确，正确格式为：yyyy-MM-dd HH:mm:ss");
            }
        }
        
        // 5. 查询学习地图阶段和任务
        List<LearningMapStage> stages = getStagesByMapId(request.getBizId());
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("学习地图没有配置学习阶段");
        }
        
        // 7. 构建响应
        AssignResponse response = new AssignResponse();
        response.setRangeIds(rangeResponse.getRangeIds()); // 设置范围ID
        response.setAssignEndTime(learningMap.getEndTime());
        
        // 记录操作日志
        try {
            UserTokenInfo userInfo = new UserTokenInfo();
            userInfo.setUserId(request.getOperatorId());

            // 构建操作详情JSON
            Map<String, Object> operationDetail = new HashMap<>();
            operationDetail.put("bizId", request.getBizId());
            operationDetail.put("userIds", request.getUserIds());
            operationDetail.put("roleIds", request.getRoleIds());
            operationDetail.put("departmentIds", request.getDepartmentIds());
            operationDetail.put("deadline", request.getDeadline());
            String operationDetailJson = objectMapper.writeValueAsString(operationDetail);
            operationLogService.recordAssignOperation(request.getBizId(), BizType.LEARNING_MAP, operationDetailJson, userInfo);
        } catch (Exception e) {
            log.error("记录指派学习地图操作日志失败", e);
            // 不影响主流程，继续执行
        }

        return response;
    }

    /**
     * 创建指派范围记录
     *
     * @param mapId       学习地图ID
     * @param operatorId  操作人ID
     * @param operatorName 操作人名称
     * @param request     指派请求
     * @return 指派范围创建响应
     */
    private CommonRangeCreateResponse createAssignmentRange(Long mapId, Long operatorId, String operatorName, AssignRequest request) {
        CommonRangeCreateRequest rangeRequest = new CommonRangeCreateRequest();
        rangeRequest.setModelType("ASSIGN"); // 功能类型：指派
        rangeRequest.setType("MAP"); // 业务类型：学习地图
        rangeRequest.setTypeId(mapId);
        rangeRequest.setCreatorId(operatorId);
        rangeRequest.setCreatorName(operatorName);
        
        // 设置目标范围
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        // 添加用户ID
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            targetTypeAndIds.put("user", request.getUserIds());
        }
        
        // 添加角色ID
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            targetTypeAndIds.put("role", request.getRoleIds());
        }
        
        // 添加部门ID
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            targetTypeAndIds.put("department", request.getDepartmentIds());
        }
        rangeRequest.setTargetTypeAndIds(targetTypeAndIds);
        
        return commonRangeInterface.batchCreateRange(rangeRequest);
    }
    
    /**
     * 检查用户是否已被指派过该学习地图
     *
     * @param mapId  学习地图ID
     * @param userId 用户ID
     * @return 是否已被指派
     */
    private boolean isAlreadyAssigned(Long mapId, Long userId) {
        // 查询指派记录
        String bizUniqueId = BizType.LEARNING_MAP + "_" + mapId;
        LambdaQueryWrapper<AssignmentDetail> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.eq(AssignmentDetail::getBizId, bizUniqueId)
                .eq(AssignmentDetail::getUserid, userId)
                .eq(AssignmentDetail::getIsDel, 0);

        return assignmentDetailMapper.selectOne(assignWrapper) != null;
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
     * 获取阶段下的所有任务
     *
     * @param stageId 阶段ID
     * @return 任务列表
     */
    private List<ContentRelation> getTasksByStageId(Long stageId) {
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, stageId);
        queryWrapper.eq(ContentRelation::getBizType, "MAP_STAGE"); // 学习地图阶段
        queryWrapper.eq(ContentRelation::getIsDel, 0); // 未删除的记录
        queryWrapper.orderByAsc(ContentRelation::getSortOrder); // 按排序顺序排序
        
        return contentRelationMapper.selectList(queryWrapper);
    }
}
