package com.learn.service.operation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.operation.OperationLogQueryRequest;
import com.learn.dto.operation.OperationLogQueryResponse;
import com.learn.infrastructure.repository.entity.OperationLog;
import com.learn.infrastructure.repository.mapper.OperationLogMapper;
import com.learn.service.operation.OperationLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 */
@Service
public class OperationLogQueryServiceImpl implements OperationLogQueryService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 查询操作记录
     *
     * @param request 查询请求
     * @return 操作记录列表
     */
    @Override
    public OperationLogQueryResponse queryOperationLog(OperationLogQueryRequest request) {
        // 参数校验
        if (request.getBusinessType() == null || request.getBusinessType() == null || request.getBusinessType().isEmpty()) {
            throw new IllegalArgumentException("业务ID和业务类型不能为空");
        }

        // 构建查询条件
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperationLog::getBizId, request.getBusinessId())
                .eq(OperationLog::getBizType, request.getBusinessType())
                .eq(OperationLog::getIsDel, 0)
                .orderByDesc(OperationLog::getOperationTime);

        // 查询数据
        List<OperationLog> operationLogs = operationLogMapper.selectList(queryWrapper);

        // 转换为响应对象
        OperationLogQueryResponse response = new OperationLogQueryResponse();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<OperationLogQueryResponse.OperationLogItem> items = operationLogs.stream()
                .map(log -> {
                    OperationLogQueryResponse.OperationLogItem item = new OperationLogQueryResponse.OperationLogItem();
                    item.setTime(dateFormat.format(log.getOperationTime()));
                    item.setMessage(log.getOperationSummary());
                    item.setOperator(log.getOperatorName());
                    item.setOperateType(log.getOperationType()); // 设置操作类型
                    return item;
                })
                .collect(Collectors.toList());
        
        response.setLogs(items);
        response.setTotal(operationLogs.size()); // 设置总数
        return response;
    }
}
