package com.learn.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yujintao
 * @date 2025/5/6
 */
@Data
public class UserStudyRecordDTO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 学习来源：ASSIGN-指派 SELF-自学
     */
    private String source;

    /**
     * 学习业务类型，COURSE-课程，SERIES_COURSE-系列课，LEARNING_MAP-学习地图,MAP_STAGE-学习阶段
     */
    private String bizType;

    /**
     * 学习业务id，课程id/系列课id/学习地图id/学习阶段id
     */
    private Long bizId;

    /**
     * 父节点业务类型，COURSE-课程，SERIES_COURSE-系列课，LEARNING_MAP-学习地图,MAP_STAGE-学习阶段
     */
    private String parentType;

    /**
     * 父节点学习业务id，课程id/系列课id/学习地图id/学习阶段id
     */
    private Long parentId;

    /**
     * 已获得学分
     */
    private Long earnedCredit;

    /**
     * 是否已发放证书：0-否 1-是
     */
    private Integer certificateIssued;

    /**
     * 关联证书id
     */
    private Long certificateId;

    /**
     * 完成状态：0-未开始 1-学习中 2-已完成,-1-超时未完成
     */
    private Integer status;

    /**
     * 学习时长(分钟)
     */
    private Integer studyDuration;

    /**
     * 学习进度(百分比)
     */
    private Integer progress;

    /**
     * 得分(考试/作业)
     */
    private BigDecimal score;

    /**
     * 通过状态：0-未通过 1-已通过
     */
    private Integer passStatus;

    /**
     * 指派时间
     */
    private Date assignTime;

    /**
     * 开始学习时间
     */
    private Date startTime;

    /**
     * 学习截止时间
     */
    private Date deadline;

    /**
     * 完成时间
     */
    private Date completionTime;

    /**
     * 最后学习时间
     */
    private Date lastStudyTime;

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
     * 扩展属性字段，内容为json。需要记录已完成阶段数，已完成必修任务数，已完成选修任务数，已获得学分
     */
    private String attributes;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     *逻辑删除: 0-正常 1-删除
     */
    private Integer isDel;

     /**
      * 已完成必学任务数
     */
    private Integer requiredTaskFinished;

    /**
     * 必学任务总数
     */
    private Integer requiredTaskTotal;
}
