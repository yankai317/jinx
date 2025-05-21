package com.learn.dto.common;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 指派记录响应类
 */
@Data
public class AssignRecordResponse {
    /**
     * 记录ID
     */
    private Long id;
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;/**
     * 操作范围摘要
     * 例如: 指派人员1个，指派部门0个，指派角色0个，指派职位0个，指派用户组0个
     */
    private String operationScope;
    
    /**
     * 指派时设置的完成时间
     */
    private Date deadline;
    
    /**
     * 提醒状态：已提醒/不提醒
     */
    private String reminderStatus;
    
    /**
     * 指派成功人数
     */
    private Integer successCount;/**
     * 指派成功人员列表
     */
    private List<UserInfo> successUsers;
    
    /**
     * 指派失败人数
     */
    private Integer failedCount;
    
    /**
     * 指派失败人员列表
     */
    private List<UserInfo> failedUsers;
    
    /**
     * 状态：指派成功/指派失败
     */
    private String status;
    
    /**
     * 操作时间
     */
    private Date operationTime;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;/**
         * 用户名称
         */
        private String userName;
        
        /**
         * 部门名称
         */
        private String departmentName;}
}
