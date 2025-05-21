package com.learn.dto.assignment;

import lombok.Data;
import java.util.List;
import java.util.Date;

/**
 * 指派回显响应
 */
@Data
public class AssignmentDetailResponse {
    /**
     * 自动指派记录ID
     */
    private Long autoAssignRecordId;
    /**
     * 指派人员列表
     */
    private List<AssignUserInfo> assignUsers;

    /**
     * 部门列表
     */
    private List<AssignDepartmentInfo> departments;

    /**
     * 角色列表
     */
    private List<AssignRoleInfo> roles;

    /**
     * 完成时间类型， 比如: 不制定、一周、两周、自定义时间
     */
    private String assignFinishedTimeType;
    
    /**
     * 自定义结束时间，只有 assignFinishedTimeType 是自定义时间时，该字段才有效
     */
    private Integer customFinishedDay;
    
    /**
     * 是否通知已存在用户
     */
    private Boolean ifIsNotifyExistUser;
    
    /**
     * 通知当前时间之后的用户
     */
    private Date notifyUserAfterJoinDate;

    /**
     * 指派的用户信息
     */
    @Data
    public static class AssignUserInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名称
         */
        private String userName;

        /**
         * 用户头像
         */
        private String avatar;

        /**
         * 指派记录ID
         */
        private Long assignRecordId;
    }

    /**
     * 部门信息
     */
    @Data
    public static class AssignDepartmentInfo {
        /**
         * 部门ID
         */
        private Long departmentId;

        /**
         * 部门名称
         */
        private String departmentName;

        /**
         * 指派记录ID
         */
        private Long assignRecordId;


    }

    /**
     * 角色信息
     */
    @Data
    public static class AssignRoleInfo {
        /**
         * 角色ID
         */
        private Long roleId;

        /**
         * 角色名称
         */
        private String roleName;

        /**
         * 指派记录ID
         */
        private Long assignRecordId;
    }
} 
