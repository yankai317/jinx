package com.learn.service.train.impl;

import com.learn.constants.BizType;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.TrainOperationLogMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.train.TrainAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训指派服务实现类
 */
@Service
@Slf4j
public class TrainAssignServiceImpl implements TrainAssignService {

    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private TrainOperationLogMapper trainOperationLogMapper;
    
    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 指派培训给用户
     * 1. 创建培训指派记录
     * 2. 为每个用户创建学习完成记录和学习记录
     * 3. 记录操作日志
     *
     * @param request 指派培训请求
     * @return 指派培训响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignResponse assignTrain(AssignRequest request) {
        log.info("开始指派培训，培训ID：{}，用户数量：{}", request.getBizId(), request.getUserIds().size());
        
        // 1. 校验培训是否存在
        Train train = trainMapper.selectById(request.getBizId());
        if (train == null || train.getIsDel() == 1) {
            throw new IllegalArgumentException("培训不存在或已删除");
        }
        
        // 2. 校验培训状态是否为已发布
        if (!"published".equals(train.getStatus())) {
            throw new IllegalArgumentException("只能指派已发布的培训");
        }
        
        // 3. 创建范围记录
        CommonRangeCreateResponse rangeResponse = createAssignmentRange(request.getBizId(), request.getOperatorId(), request.getOperatorName(), request);
        if (rangeResponse == null || !rangeResponse.getSuccess() || CollectionUtils.isEmpty(rangeResponse.getRangeIds())) {
            throw new RuntimeException("创建指派范围记录失败");
        }

        // 8. 记录操作日志
        createOperationLog(request.getBizId(), train.getName(), 0, request.getOperatorId(), request.getOperatorName(), new Date());
        
        // 9. 构建响应
        AssignResponse response = new AssignResponse();
        response.setRangeIds(rangeResponse.getRangeIds()); // 设置范围ID
        return response;
    }
    
    /**
     * 创建指派范围记录
     *
     * @param trainId     培训ID
     * @param operatorId  操作人ID
     * @param operatorName 操作人名称
     * @return 指派范围创建响应
     */
    private CommonRangeCreateResponse createAssignmentRange(Long trainId, Long operatorId, String operatorName, AssignRequest request) {
        CommonRangeCreateRequest rangeRequest = new CommonRangeCreateRequest();
        rangeRequest.setModelType("ASSIGN"); // 功能类型：指派
        rangeRequest.setType(BizType.TRAIN); // 业务类型：培训
        rangeRequest.setTypeId(trainId);
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
     * 创建操作日志
     *
     * @param trainId      培训ID
     * @param trainName    培训名称
     * @param successCount 成功指派数量
     * @param operatorId   操作人ID
     * @param operatorName 操作人名称
     * @param now          当前时间
     */
    private void createOperationLog(Long trainId, String trainName, int successCount, Long operatorId, String operatorName, Date now) {
        OperationLog log = new OperationLog();
        log.setBizId(trainId);
        log.setBizType(BizType.TRAIN);
        log.setOperationType("ASSIGN"); // 操作类型：指派培训
        
        // 操作详情
        Map<String, Object> detail = new HashMap<>();
        detail.put("trainId", trainId);
        detail.put("trainName", trainName);
        detail.put("assignCount", successCount);
        detail.put("operationTime", now);
        log.setOperationDetail(detail.toString());
        
        // 操作概要
        log.setOperationSummary("指派培训「" + trainName + "」给" + successCount + "名用户");
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationTime(now);
        log.setGmtCreate(now);
        log.setCreatorId(operatorId);
        log.setCreatorName(operatorName);
        log.setUpdaterId(operatorId);
        log.setUpdaterName(operatorName);
        log.setGmtModified(now);
        log.setIsDel(0);
        
        trainOperationLogMapper.insert(log);
    }
}
