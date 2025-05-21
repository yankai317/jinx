package com.learn.dto.role;

import lombok.Data;

/**
 * 给角色添加人员响应
 */
@Data
public class RoleAddUsersResponse {
    /**
     * 总数
     */
    private Integer total;
    
    /**
     * 成功数
     */
    private Integer success;
    
    /**
     * 失败数
     */
    private Integer fail;
    
    /**
     * 失败的用户ID列表
     */
    private java.util.List<Long> failUsers;
}
