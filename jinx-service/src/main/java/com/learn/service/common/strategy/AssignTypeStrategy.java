package com.learn.service.common.strategy;

import com.learn.common.enums.AssignFinishedTimeTypeEnums;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.CommonRange;

import java.util.List;
import java.util.Set;

/**
 * 指派类型策略接口
 * 定义不同指派类型（单次通知、自动通知）的处理策略
 */
public interface AssignTypeStrategy {
    
    /**
     * 处理指派记录
     * 
     * @param assignRecord 指派记录
     * @param rangeList 范围列表
     * @param assignFinishedTimeType 指派完成时间类型
     * @param customFinishedTime 自定义完成时间
     * @param ifIsNotifyExistUser 是否通知已存在的用户
     * @param notifyUserAfterJoinDate 通知入职日期之后的用户
     * @return 处理的用户数量
     */
    int processAssignRecord(AssignRecords assignRecord, List<CommonRange> rangeList, AssignFinishedTimeTypeEnums assignFinishedTimeType,
                            Integer customFinishedDay,
                            Boolean ifIsNotifyExistUser,
                            java.util.Date notifyUserAfterJoinDate);
    
    /**
     * 获取指派类型代码
     * 
     * @return 指派类型代码
     */
    String getAssignTypeCode();
    
    /**
     * 获取用户可见范围
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 可见范围内的用户ID列表
     */
    Set<Long> getVisibleUserIds(Long bizId, String bizType);
    
    /**
     * 获取已存在的用户
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 已存在的用户ID列表
     */
    List<Long> getExistingUserIds(Long bizId, String bizType);
} 
