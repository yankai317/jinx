package com.learn.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 给角色添加人员请求
 */
@Data
public class RoleAddUsersRequest {
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 用户ID数组
     */
    private List<Long> userIds;
}
