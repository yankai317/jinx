package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learn.constants.LearningStatus;
import com.learn.infrastructure.repository.BaseAttribute;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * 用户学习记录表
 * @TableName user_learning_task
 */
@TableName(value ="user_learning_task")
@Data
public class UserLearningTask extends BaseAttribute {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
    private Integer earnedCredit;

    /**
     * 是否已发放证书：0-否 1-是
     */
    private Integer certificateIssued;

    /**
     * 关联证书id
     */
    private Long certificateId;

    /**
     * 完成状态：
     * @see com.learn.constants.LearningStatus
     */
    private String status;

    /**
     * 学习时长(秒)
     */
    private Integer studyDuration;

    /**
     * 学习进度(百分比)
     */
    private Integer progress;

    /**
     * 得分(考试/作业)
     */
    private Integer score;

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
     * 更新时间
     */
    private Date gmtModified;

    /**
     *逻辑删除: 0-正常 1-删除
     */
    private Integer isDel;

    private String searchKey;


    public boolean isCompleted() {
        return this.progress == 100;
    }

    public String buildUserSearchKey() {
        return this.bizType + "_" + this.bizId + "_" + this.userId;
    }

    public String buildContentSearchKey() {
        return this.bizType + "_" + this.bizId;
    }

    public String convertStatus(Integer progress) {
        if (Objects.isNull(progress)) {
            return this.getStatus();
        }
        if (progress == 0) {
            return LearningStatus.NOT_STARTED;
        } else if (progress > 0 && progress < 100) {
            return LearningStatus.LEARNING;
        } else if (progress != 100 && this.isExpired()) {
            return LearningStatus.EXPIRED;
        } else {
            return LearningStatus.COMPLETED;
        }
    }

    public boolean isExpired() {
        return this.deadline != null && this.deadline.before(new Date());
    }
}
