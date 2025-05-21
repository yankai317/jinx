package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 培训操作记录表
 * @TableName train_operation_log
 */
@TableName(value ="operation_log")
@Data
public class OperationLog {
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
     * 业务类型
     */
    private String bizType;

    /**
     * 操作类型：CREATE-创建培训 UPDATE-修改培训 DELETE-删除培训 ASSIGN-指派培训 CANCEL-取消指派
     */
    private String operationType;

    /**
     * 操作详情JSON，记录变更前后的具体内容
     */
    private String operationDetail;

    /**
     * 操作概要，用于前端展示
     */
    private String operationSummary;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private Date operationTime;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 创建人id
     */
    private Long creatorId;

    /**
     * 属性
     */
    private String attributes;

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
