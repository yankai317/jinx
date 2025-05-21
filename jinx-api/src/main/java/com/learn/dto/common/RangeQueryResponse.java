package com.learn.dto.common;

import lombok.Data;
import java.util.List;

/**
 * 范围查询响应对象
 * 包含部门、角色、用户的详细信息
 */
@Data
public class RangeQueryResponse {
    /**
     * 范围类型：ALL(全员可见)、ALL_MANAGE(全部管理范围)、PART(部分可见)
     */
    private String type;
    
    /**
     * 部门信息列表
     */
    private List<RangeDepartment> departmentInfos;
    
    /**
     * 角色信息列表
     */
    private List<RangeRole> roleInfos;
    
    /**
     * 用户信息列表
     */
    private List<RangeUser> userInfos;
    
    /**
     * 指派的用户信息
     */
    @Data
    public static class RangeUser {
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
    }

    /**
     * 部门信息
     */
    @Data
    public static class RangeDepartment {
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
    public static class RangeRole {
        /**
         * 角色ID
         */
        private Long roleId;
        
        /**
         * 角色名称
         */
        private String roleName;
    }
}
