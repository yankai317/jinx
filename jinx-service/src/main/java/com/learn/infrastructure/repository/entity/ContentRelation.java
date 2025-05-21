package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learn.infrastructure.repository.BaseAttribute;
import lombok.Data;

import java.util.Date;

/**
 * 培训内容关联表
 * @TableName train_content_relation
 */
@TableName(value ="content_relation")
@Data
public class ContentRelation extends BaseAttribute {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 业务类型：培训/地图阶段
     */
    private String bizType;

    /**
     * 关联内容类型: EXAM-考试 ASSIGNMENT-作业 SURVEY-调研 COURSE-课程 TRAIN-培训
     */
    private String contentType;

    /**
     * 关联内容ID（外部ID）
     */
    private Long contentId;

    /**
     * 关联内容URL地址
     */
    private String contentUrl;

    /**
     *
     */
    private String contentBizType;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否必修：0-选修 1-必修
     */
    private Integer isRequired;

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
     * 逻辑删除: 0-正常 1-删除
     */
    private Integer isDel;

    public String getBizSearchKey() {
        return this.bizType + "_" + this.bizId;
    }

    public String getContentSearchKey() {
        return this.contentType + "_" + this.contentId;
    }
}
