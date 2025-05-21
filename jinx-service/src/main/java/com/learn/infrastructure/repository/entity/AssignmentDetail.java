package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 指派明细表
 * @TableName assignment_detail
 */
@TableName(value ="assignment_detail")
@Data
public class AssignmentDetail {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * common_visibility中类型为指派的id
     */
    private Long assignmentId;

    /**
     * 关联指派记录ID
     */
    private Long assignRecordId;

    /**
     * user表的用户id
     */
    private Long userid;

    /**
     * 业务id，根据类型拼接，比如 [type]_[id], train_id,用于处理同一个业务id被指派多次，但是只能通知一次
     */
    private String bizId;

    /**
     * 类型:courses[课程],map[地图],train[培训],exam[考试]
     */
    private String type;

    /**
     * 关联的id，可以是课程、培训、考试等id
     */
    private Long typeId;

    /**
     * 状态表
     */
    private String status;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 扩展属性字段，内容为json
     */
    private String attributes;

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
     * 任务结束时间
     */
    private Date finishTime;
}
