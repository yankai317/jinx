package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 指派记录表
 * @TableName assign_records
 */
@TableName(value ="assign_records")
@Data
public class AssignRecords {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型:courses[课程],map[地图],train[培训],exam[考试]
     */
    private String type;

    /**
     * 关联的id，可以是课程、培训、考试等id
     */
    private Long typeId;

    /**
     * 范围ids
     */
    private String rangeIds;

    /**
     * 自动指派截止时间
     */
    private Date deadline;

    /**
     * 指派类型: once-单次通知 auto-自动通知
     */
    private String assignType;

    /**
     * 通知状态：待通知、通知中、已通知
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
    private Boolean isDel;

    /**
     * 通知完成时间类型：custom-自定义时间、one_week-1周、two_week-2周、four_week-4周
     */
    private String assignFinishedTimeType;

    /**
     * 自定义结束时间
     */
    private Integer customFinishedDay;

    /**
     * 是否通知已存在用户
     */
    private Boolean ifIsNotifyExistUser;

    /**
     * 通知当前时间之后的用户
     */
    private Date notifyUserAfterJoinDate;

}
