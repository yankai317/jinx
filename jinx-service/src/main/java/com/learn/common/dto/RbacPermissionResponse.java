package com.learn.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 权限响应DTO
 */
@Data
public class RbacPermissionResponse {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 权限代码列表
     */
    private List<String> permissionCodes;
    
    /**
     * 菜单权限列表
     */
    private List<PermissionDTO> menuPermissions;
    
    /**
     * 按钮权限列表
     */
    private List<PermissionDTO> buttonPermissions;
    
    /**
     * API权限列表
     */
    private List<PermissionDTO> apiPermissions;
    
    /**
     * 权限DTO
     */
    @Data
    public static class PermissionDTO {
        /**
         * 权限ID
         */
        private Long id;
        
        /**
         * 权限名称
         */
        private String name;
        
        /**
         * 权限编码
         */
        private String code;
        
        /**
         * 权限类型：menu-菜单，button-按钮，api-接口
         */
        private String type;
        
        /**
         * 父权限ID
         */
        private Long parentId;
        
        /**
         * 权限路径
         */
        private String path;
        
        /**
         * 资源路径
         */
        private String resourcePath;
        
        /**
         * 子权限列表
         */
        private List<PermissionDTO> children;
    }
} 