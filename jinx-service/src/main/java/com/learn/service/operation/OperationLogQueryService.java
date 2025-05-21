package com.learn.service.operation;

import com.learn.dto.operation.OperationLogQueryRequest;
import com.learn.dto.operation.OperationLogQueryResponse;

/**
 * 操作日志服务接口
 */
public interface OperationLogQueryService {
    
    /**
     * 查询操作记录
     * 
     * @param request 查询请求
     * @return 操作记录列表
     */
    OperationLogQueryResponse queryOperationLog(OperationLogQueryRequest request);
}
