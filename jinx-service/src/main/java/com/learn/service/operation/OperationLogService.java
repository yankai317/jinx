package com.learn.service.operation;

import com.learn.common.dto.UserTokenInfo;

/**
 * 操作日志服务接口
 * 用于记录课程/培训/地图的操作日志
 */
public interface OperationLogService {
    
    /**
     * 记录创建操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordCreateOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录更新操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordUpdateOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录删除操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordDeleteOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录指派操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordAssignOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录取消指派操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordCancelAssignOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录发布操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordPublishOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
    
    /**
     * 记录取消发布操作日志
     * 
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    void recordUnpublishOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo);
}
