package com.learn.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 从角色中批量删除用户的请求
 */
@Data
public class RoleRemoveUsersRequest {
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 用户ID数组
     */
    private List<Long> userIds;
}
