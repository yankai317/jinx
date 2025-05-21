package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 部门表
 * @TableName department
 */
@TableName(value ="department")
@Data
@Accessors(chain = true)
public class Department {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 上级部门ID，顶级部门为0
     */
    private Long parentId;

    /**
     * 部门路径，格式：1,2,3
     */
    private String departmentPath;

    /**
     * 部门层级，从1开始
     */
    private Integer departmentLevel;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 外部系统部门ID
     */
    private String externalId;

    /**
     * 外部系统来源，如：dingtalk、feishu
     */
    private String externalSource;

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
