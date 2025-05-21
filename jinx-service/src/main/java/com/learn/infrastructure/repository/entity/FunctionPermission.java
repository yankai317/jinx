package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 功能权限表
 * @TableName function_permission
 */
@TableName(value ="function_permission")
@Data
public class FunctionPermission {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限类型：menu-菜单，button-按钮，api-接口
     */
    private String permissionType;

    /**
     * 父权限ID，顶级权限为0
     */
    private Long parentId;

    /**
     * 权限路径，格式：1,2,3
     */
    private String permissionPath;

    /**
     * 权限层级，从1开始
     */
    private Integer permissionLevel;

    /**
     * 资源路径，如菜单路径、接口路径
     */
    private String resourcePath;

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
