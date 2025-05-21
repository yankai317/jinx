package com.learn.dto.common;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * 指派请求类
 */
@Data
public class AssignRequest {
    /**
     * 指派记录id
     */
    private Long assignRecordId;
    /**
     * 业务ID，例如courseId/trainId/mapId
     */
    private Long bizId;

    /**
     * 业务类型，例如course/train/map
     */
    private String bizType;
    
    /**
     * 用户ID列表
     */
    private List<Long> userIds;
    /**
     * 角色ids
     */
    private List<Long> roleIds;
    /**
     * 部门ids
     */
    private List<Long> departmentIds;
    /**
     * 根据导入工号
     */
    private List<String> workNos;
    /**
     * 三方类型
     */
    private String thirtyType;
    /**
     * 根据三方ids
     */
    private List<String> thirtyUserIds;
    
    /**
     * 截止时间
     */
    private String deadline;
    
    /**
     * 是否发送通知，默认true
     */
    private Boolean sendNotification;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    /**
     * 指派类型，比如：单次通知、自动通知
     */
    private String assignType;

    /**
     *  完成时间类型， 比如: 不制定、一周、两周、自定义时间
     */
    private String assignFinishedTimeType;
    /**
     * 自定义结束时间，只有 doFinishedTimeType 是自定义时间时，该字段才有效
     */
    private Integer customFinishedDay;
    /**
     * 是否通知已存在用户
     */
    private Boolean ifIsNotifyExistUser;
    /**
     * 通知当前时间之后的用户
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date notifyUserAfterJoinDate;

}
