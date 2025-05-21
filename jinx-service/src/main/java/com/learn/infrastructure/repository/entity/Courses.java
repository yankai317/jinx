package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learn.infrastructure.repository.BaseAttribute;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName Courses
 */
@TableName(value ="Courses")
@Data
public class Courses extends BaseAttribute {
    /**
     * 课程ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 课程名称
     */
    private String title;

    /**
     * 课程类型,video, document, series, article
     */
    private String type;

    /**
     * 封面图路径
     */
    private String coverImage;

    /**
     * 讲师ID（仅视频/系列课）
     */
    private Integer instructorId;

    /**
     * 嘉宾名称
     */
    private String guestName;

    /**
     * 简介（最多500字）
     */
    private String description;

    /**
     * 学分
     */
    private Integer credit;

    /**
     * 分类ids,格式:1,2,3
     */
    private String categoryIds;

    /**
     * 状态, draft, published
     */
    private String status;

    /**
     * 是否允许评论：1-允许，0-不允许
     */
    private Integer allowComments;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 查看数
     */
    private Integer viewCount;

    /**
     * 完成人数
     */
    private Integer completeCount;

    /**
     * 文章
     */
    private String article;

    /**
     * 附件类型
     */
    private String appendixType;

    /**
     * 文件存储路径
     */
    private String appendixPath;

    /**
     * 发布时间
     */
    private Date publishTime;

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
     * 是否可引用：1-可引用，0-不可引用
     */
    private Boolean ifIsCitable;
}
