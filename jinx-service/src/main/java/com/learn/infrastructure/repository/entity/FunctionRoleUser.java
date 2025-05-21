package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 功能权限角色用户关联表
 * @TableName function_role_user
 */
@TableName(value ="function_role_user")
@Data
public class FunctionRoleUser {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 功能权限角色ID，关联function_role表
     */
    private Long functionRoleId;

    /**
     * 用户ID，关联user表
     */
    private Long userId;

    /**
     * 部门ID，关联department表，表示用户在该部门下拥有此角色权限
     */
    private Long departmentId;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 创建人id
     */
    private Long creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 更新人id
     */
    private Long updaterId;

    /**
     * 更新人名称
     */
    private String updaterName;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 逻辑删除字段: 是否删除 1-已删 0-正常
     */
    private Integer isDel;
}
