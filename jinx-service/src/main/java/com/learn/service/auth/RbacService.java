package com.learn.service.auth;

import com.learn.common.dto.RbacPermissionResponse;

import java.util.List;

/**
 * 权限服务器
 * 1. 需要实现权限获取接口: 根据用户获取权限
 * 2. 根据用户和权限点，判断用户是否有权限。没有就抛出异常
 * 对应需要操作的表: function_permission、function_role_permission、function_role_user
 */
public interface RbacService {
    /**
     * 获取用户权限
     *
     * @param userId 用户ID
     * @return 用户权限响应
     */
    RbacPermissionResponse getUserPermissions(Long userId);
    
    /**
     * 检查用户是否有特定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    Boolean checkPermission(Long userId, String permissionCode);
    
    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    List<RbacPermissionResponse.PermissionDTO> getAllPermissions();
}
