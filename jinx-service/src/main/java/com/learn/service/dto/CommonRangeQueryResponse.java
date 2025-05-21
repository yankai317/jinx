package com.learn.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用范围查询响应对象
 */
@Data
public class CommonRangeQueryResponse {
    /**
     * rangId
     */
    private Long id;
    /**
     * 业务ID
     */
    private Long typeId;


    /**
     * 类型:courses[课程],map[地图],train[培训],exam[考试]
     */
    private String type;
    
    /**
     * 部门IDs
     */
    private List<Long> departmentIds;
    
    /**
     * 角色IDs
     */
    private List<Long> roleIds;
    
    /**
     * 用户IDs
     */
    private List<Long> userIds;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;

    private Long creatorId;
}
