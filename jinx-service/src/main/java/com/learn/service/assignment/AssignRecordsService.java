package com.learn.service.assignment;

import com.learn.dto.assignment.AssignRecordDetailQueryRequest;
import com.learn.dto.assignment.AssignRecordDetailQueryResponse;
import com.learn.dto.assignment.AssignRecordsQueryRequest;
import com.learn.dto.assignment.AssignRecordsQueryResponse;

import java.util.List;

/**
 * 指派记录服务接口
 * 处理指派记录相关的业务逻辑
 */
public interface AssignRecordsService {

    /**
     * 查询指派记录
     * 根据type_{type_id}查询assign_records中删除状态为false的数据
     * 并根据range_ids查询门店、角色、人员的名称
     *
     * @param request 查询请求参数
     * @return 指派记录列表
     */
    List<AssignRecordsQueryResponse> queryAssignRecords(AssignRecordsQueryRequest request);

    /**
     * 查询指派记录总数
     *
     * @param request 查询请求参数
     * @return 记录总数
     */
    Long countAssignRecords(AssignRecordsQueryRequest request);

    /**
     * 查询指派记录明细
     * 根据指派记录ID查询assignment_detail中的明细
     *
     * @param request 查询请求参数
     * @return 指派记录明细列表
     */
    List<AssignRecordDetailQueryResponse> queryAssignRecordDetails(AssignRecordDetailQueryRequest request);

    /**
     * 查询指派记录明细总数
     *
     * @param request 查询请求参数
     * @return 记录总数
     */
    Long countAssignRecordDetails(AssignRecordDetailQueryRequest request);
}
