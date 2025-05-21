package com.learn.controller.assignment;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.assignment.AssignRecordDetailQueryRequest;
import com.learn.dto.assignment.AssignRecordDetailQueryResponse;
import com.learn.dto.assignment.AssignRecordsQueryRequest;
import com.learn.dto.assignment.AssignRecordsQueryResponse;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.user.UserInfoResponse;
import com.learn.service.assignment.AssignRecordsService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指派记录控制器
 * 提供指派记录查询和指派记录明细查询的接口
 */
@RestController
@RequestMapping("/api/assignment/records")
@Slf4j
public class AssignRecordsController {

    @Resource
    private AssignRecordsService assignRecordsService;
    @Resource
    private UserTokenUtil userTokenUtil;

    /**
     * 查询指派记录
     * 根据type_{type_id}查询assign_records中删除状态为false的数据
     * 并根据range_ids查询门店、角色、人员的名称
     *
     * @param request 查询请求参数
     * @return 指派记录列表及分页信息
     */
    @PostMapping("/query")
    public ApiResponse<List<AssignRecordsQueryResponse>> queryAssignRecords(
            @RequestBody AssignRecordsQueryRequest request) {
        log.info("收到指派记录查询请求: {}", request);
        try {
            // 获取当前用户信息

            // 查询指派记录
            List<AssignRecordsQueryResponse> records = assignRecordsService.queryAssignRecords(request);
            
            // 查询总数
            Long total = assignRecordsService.countAssignRecords(request);

            log.info("查询指派记录成功，共{}条记录", records.size());
            return ApiResponse.success("查询指派记录成功", records, total);
        } catch (Exception e) {
            log.error("查询指派记录失败", e);
            return ApiResponse.error("查询指派记录失败：" + e.getMessage());
        }
    }

    /**
     * 查询指派记录明细
     * 根据指派记录ID查询assignment_detail中的明细
     *
     * @param request 查询请求参数
     * @return 指派记录明细列表及分页信息
     */
    @PostMapping("/detail")
    public ApiResponse<List<AssignRecordDetailQueryResponse>> queryAssignRecordDetails(
            @RequestBody AssignRecordDetailQueryRequest request) {
        log.info("收到指派记录明细查询请求: {}", request);
        try {

            // 查询指派记录明细
            List<AssignRecordDetailQueryResponse> details = assignRecordsService.queryAssignRecordDetails(request);
            
            // 查询总数
            Long total = assignRecordsService.countAssignRecordDetails(request);
            
            log.info("查询指派记录明细成功，共{}条记录", details.size());
            return ApiResponse.success("查询指派记录明细成功", details, total);
        } catch (Exception e) {
            log.error("查询指派记录明细失败", e);
            return ApiResponse.error("查询指派记录明细失败：" + e.getMessage());
        }
    }
}
