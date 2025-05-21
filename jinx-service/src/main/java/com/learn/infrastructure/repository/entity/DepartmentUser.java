package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 部门用户关联表
 * @TableName department_user
 */
@TableName(value ="department_user")
@Data
public class DepartmentUser {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 部门ID，关联department表
     */
    private Long departmentId;

    /**
     * 用户ID，关联user表
     */
    private Long userId;

    /**
     * 部门路径，格式：,1,2,3, 表示从顶级部门到当前部门的ID路径
     */
    private String departmentPath;

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
