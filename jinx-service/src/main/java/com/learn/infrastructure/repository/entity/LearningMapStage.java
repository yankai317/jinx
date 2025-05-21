package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 学习地图阶段表
 * @TableName learning_map_stage
 */
@TableName(value ="learning_map_stage")
@Data
public class LearningMapStage {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学习地图ID
     */
    private Long mapId;

    /**
     * 阶段名称
     */
    private String name;

    /**
     * 阶段顺序号
     */
    private Integer stageOrder;

    /**
     * 开放类型：0-不设置开放时间 1-设置固定开放时间 2-设置学员学习期限
     */
    private Integer openType;

    /**
     * 固定开放开始时间
     */
    private Date startTime;

    /**
     * 固定开放结束时间
     */
    private Date endTime;

    /**
     * 学习期限(天)
     */
    private Integer durationDays;

    /**
     * 阶段学分(按阶段给分时使用)
     */
    private Integer credit;

    /**
     * 证书ID(按阶段发证时使用)
     */
    private Long certificateId;

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
}
