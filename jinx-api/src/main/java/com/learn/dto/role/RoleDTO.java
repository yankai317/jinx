package com.learn.dto.role;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 角色数据传输对象
 */
@Data
public class RoleDTO {
    /**
     * 角色ID
     */
    private Long id;
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 角色描述
     */
    private String roleDescription;
    
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
    private String creatorName;/**
     * 权限ID列表
     */
    private List<Long> permissionIds;
    /**
     * 用户ID列表
     */
    private List<Long> userIds;
}
