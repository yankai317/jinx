package com.learn.controller.operation;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.operation.OperationLogQueryRequest;
import com.learn.dto.operation.OperationLogQueryResponse;
import com.learn.service.operation.OperationLogQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 操作记录控制器
 */
@RestController
@RequestMapping("/api/operation")
@CrossOrigin
@Slf4j
public class OperationController {

    @Autowired
    private OperationLogQueryService operationLogQueryService;

    /**
     * 查询操作记录
     *
     * @param request 查询操作记录请求
     * @return 操作记录列表
     */
    @PostMapping("/logs")
    public ApiResponse<OperationLogQueryResponse> queryOperationLog(@RequestBody @Valid OperationLogQueryRequest request) {
        log.info("查询操作记录请求入参: bizId={}, bizType={}", request.getBusinessType(), request.getBusinessType());
        
        // 参数校验
        if (request.getBusinessType() == null) {
            return ApiResponse.error(400, "业务ID不能为空");
        }
        if (request.getBusinessType() == null || request.getBusinessType().trim().isEmpty()) {
            return ApiResponse.error(400, "业务类型不能为空");
        }
        
        // 调用服务查询操作记录
        OperationLogQueryResponse response = operationLogQueryService.queryOperationLog(request);
        
        return ApiResponse.success("查询成功", response);
    }
}
