package com.learn.service.assignment;

import com.learn.dto.assignment.AssignmentDetailRequest;
import com.learn.dto.assignment.AssignmentDetailResponse;
import com.learn.dto.common.AssignRecordRequest;
import com.learn.dto.common.AssignRecordResponse;
import com.learn.dto.common.AssignRequest;
import com.learn.infrastructure.repository.entity.AssignRecords;

import java.util.Date;
import java.util.List;

/**
 * 指派明细服务接口
 * 处理指派明细相关的业务逻辑
 */
public interface AssignmentDetailService {

    /**
     * 同步指派范围到指派明细
     * 将指派范围中的部门和角色转换为具体的人员，并更新到指派明细表
     * 
     * @return 新增的指派明细数量
     */
    int syncAssignmentRangeToDetail();
    
    /**
     * 根据指派ID获取已存在的用户ID列表
     * 
     * @param assignmentId 指派ID
     * @return 用户ID列表
     */
    List<Long> getExistingUserIdsByAssignmentId(Long assignmentId);
    
    /**
     * 批量插入指派明细
     * 
     * @param assignmentId 指派ID
     * @param userIds 用户ID列表
     * @param creatorId 创建人ID
     * @param creatorName 创建人名称
     * @return 插入的记录数
     */
    int batchInsertAssignmentDetail(Long assignmentId, List<Long> userIds, Long creatorId, String creatorName);
    
    /**
     * 查询指派记录
     * 根据业务ID查询指派记录列表
     *
     * @param businessId 业务ID
     * @param request 查询请求参数
     * @return 指派记录列表及分页信息
     */
    List<AssignRecordResponse> queryAssignRecords(Long businessId, AssignRecordRequest request);
    
    /**
     * 获取指派记录总数
     *
     * @param businessId 业务ID
     * @param request 查询请求参数
     * @return 记录总数
     */
    Long countAssignRecords(Long businessId, AssignRecordRequest request);
    
    /**
     * 保存指派记录
     *
     * @param bizType       业务类型
     * @param bizId         业务ID
     * @param deadline      截止时间
     * @param creatorId     创建人ID
     * @param creatorName   创建人名称
     * @param rangeIds      范围ID列表
     * @param assignEndTime
     * @return 保存的指派记录
     */
    AssignRecords saveAssignRecord(AssignRequest request, List<Long> rangeIds, Long userId, String nickname, Date assignEndTime);

    /**
     * 获取指派回显信息
     * 根据业务类型、业务ID和指派类型获取指派回显信息
     *
     * @param request 指派回显请求
     * @return 指派回显信息
     */
    AssignmentDetailResponse getAssignmentDetail(AssignmentDetailRequest request);

    /**
     * 根据ID获取指派记录
     *
     * @param assignRecordId 指派记录ID
     * @return 指派记录
     */
    AssignRecords getAssignRecordById(Long assignRecordId);

    /**
     * 更新指派记录
     *
     * @param assignRecord 指派记录
     * @return 更新结果
     */
    int updateAssignRecord(AssignRecords assignRecord);
}
