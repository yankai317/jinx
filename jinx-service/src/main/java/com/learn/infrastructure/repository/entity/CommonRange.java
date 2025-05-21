package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 考试可见范围表
 * @TableName common_range
 */
@TableName(value ="common_range")
@Data
public class CommonRange {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 可见性、指派、协同管理
     */
    private String modelType;

    /**
     * 关联的id，可以是课程、培训、考试等id
     */
    private Long typeId;

    /**
     * 类型:courses[课程],map[地图],train[培训],exam[考试]
     */
    private String type;

    /**
     * 业务类型和ID的组合，格式：{type}_{typeId}
     */
    private String typeBizId;

    /**
     * 目标类型：1-department[部门]，2-role[角色]，3-user[用户]
     */
    private String targetType;

    /**
     * 目标ids,例[1,2,3,4]，根据 target_type 关联不同表
     */
    private String targetIds;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 完成时间
     */
    private Date endTime;

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
}
