package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learn.constants.BizType;
import lombok.Data;

import java.util.Date;

/**
 * 培训信息表
 * @TableName train
 */
@TableName(value = BizType.TRAIN)
@Data
public class Train {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 培训名称
     */
    private String name;

    /**
     * 封面图片地址
     */
    private String cover;

    /**
     * 培训简介
     */
    private String introduction;

    /**
     * 学分
     */
    private Integer credit;

    /**
     * 分类ids, 格式 1,2,3
     */
    private String categoryIds;

    /**
     * 是否允许评论: 0-不允许 1-允许
     */
    private Integer allowComment;

    /**
     * 证书ID
     */
    private Long certificateId;

    /**
     * 状态,draft, published
     */
    private String status;

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
     * 扩展属性字段，内容为json
     */
    private String attributes;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 逻辑删除: 0-正常 1-删除
     */
    private Integer isDel;
    
    /**
     * 是否可引用：1-可引用，0-不可引用
     */
    private Boolean ifIsCitable;
}
