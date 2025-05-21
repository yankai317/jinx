package com.learn.dto.operation;

import lombok.Data;

import java.util.List;

/**
 * 查询操作记录响应
 */
@Data
public class OperationLogQueryResponse {

    private Integer total;

    /**
     * 操作记录列表
     */
    private List<OperationLogItem> logs;

    /**
     * 操作记录项
     */
    @Data
    public static class OperationLogItem {
        /**
         * 操作时间
         */
        private String time;

        private String operateType;

        /**
         * 操作记录
         */
        private String message;

        /**
         * 操作人名称
         */
        private String operator;
    }
}
