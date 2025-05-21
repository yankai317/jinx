package com.learn.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 修改角色请求
 */
@Data
public class RoleUpdateRequest {
    /**
     * 角色ID
     */
    private Long id;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色描述
     */
    private String roleDescription;
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 权限ID数组
     */
    private List<Long> permissionIds;
}
