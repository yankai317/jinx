package com.learn.dto.operation;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 查询操作记录请求
 */
@Data
public class OperationLogQueryRequest {
    /**
     * 业务id
     */
    private Long businessId;

    /**
     * 业务类型
     */
    @NotNull(message = "业务类型不能为空")
    private String businessType;


    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
