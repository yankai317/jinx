package com.learn.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户信息响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 工号
     */
    private String employeeNo;
    
    /**
     * 部门列表
     */
    private List<DepartmentInfo> departments;
    
    /**
     * 角色列表
     */
    private List<RoleInfo> roles;
    
    /**
     * 权限列表
     */
    private List<String> permissions;
    
    /**
     * 部门信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentInfo {
        /**
         * 部门ID
         */
        private Long id;
        
        /**
         * 部门名称
         */
        private String name;
    }
    
    /**
     * 角色信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        /**
         * 角色ID
         */
        private Long id;
        
        /**
         * 角色名称
         */
        private String name;
    }
}
