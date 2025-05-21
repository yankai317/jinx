package com.learn.dto.assignment;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 指派记录查询响应
 */
@Data
public class AssignRecordsQueryResponse {
    /**
     * 指派记录ID
     */
    private Long id;
    
    /**
     * 业务类型
     */
    private String type;
    
    /**
     * 业务ID
     */
    private Long typeId;
    
    /**
     * 范围IDs
     */
    private String rangeIds;
    
    /**
     * 截止时间
     */
    private Date deadline;
    
    /**
     * 指派类型: once-单次通知 auto-自动通知
     */
    private String assignType;
    
    /**
     * 通知状态：待通知、通知中、已通知
     */
    private String status;
    
    /**
     * 创建时间
     */
    private Date gmtCreate;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 部门列表
     */
    private List<DepartmentInfo> departments;
    
    /**
     * 角色列表
     */
    private List<RoleInfo> roles;
    
    /**
     * 用户列表
     */
    private List<UserInfo> users;
    
    /**
     * 部门信息
     */
    @Data
    public static class DepartmentInfo {
        /**
         * 部门ID
         */
        private Long departmentId;
        
        /**
         * 部门名称
         */
        private String departmentName;
    }
    
    /**
     * 角色信息
     */
    @Data
    public static class RoleInfo {
        /**
         * 角色ID
         */
        private Long roleId;
        
        /**
         * 角色名称
         */
        private String roleName;
    }
    
    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户名称
         */
        private String userName;
    }
}
