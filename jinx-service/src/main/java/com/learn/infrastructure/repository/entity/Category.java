package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 分类表
 * @TableName category
 */
@TableName(value ="category")
@Data
public class Category {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类id，顶级分类为0
     */
    private Long parentId;

    /**
     * 分类路径，格式：1,2,3
     */
    private String path;

    /**
     * 分类层级，从1开始
     */
    private Integer level;

    /**
     * 同级分类排序
     */
    private Integer sort;

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

    /**
     * 子分类列表，非数据库字段
     */
    @TableField(exist = false)
    private List<Category> children;
}
