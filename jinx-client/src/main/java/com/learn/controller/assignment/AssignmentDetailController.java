package com.learn.controller.assignment;

import com.learn.dto.assignment.AssignmentDetailRequest;
import com.learn.dto.assignment.AssignmentDetailResponse;
import com.learn.dto.common.ApiResponse;
import com.learn.service.assignment.AssignmentDetailService;
import com.learn.service.assignment.scheduler.AssignmentDetailSyncScheduler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指派明细控制器
 * 提供手动触发同步任务的接口
 */
@RestController
@RequestMapping("/api/assignment")
@Slf4j
public class AssignmentDetailController {

    @Resource
    private AssignmentDetailSyncScheduler assignmentDetailSyncScheduler;
    
    @Resource
    private AssignmentDetailService assignmentDetailService;

    /**
     * 手动触发同步指派范围到指派明细
     * 用于测试或手动触发同步
     *
     * @return API响应
     */
    @PostMapping("/sync")
    public ApiResponse<Integer> manualSyncAssignmentRangeToDetail() {
        log.info("收到手动触发指派明细同步请求");
        try {
            int insertedCount = assignmentDetailSyncScheduler.manualSyncAssignmentRangeToDetail();
            log.info("手动触发指派明细同步成功，新增{}条记录", insertedCount);
            return ApiResponse.success( "同步成功，新增" + insertedCount + "条记录", insertedCount);
        } catch (Exception e) {
            log.error("手动触发指派明细同步失败", e);
            return ApiResponse.error("同步失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取指派回显信息
     * 根据业务类型、业务ID和指派类型获取指派回显信息
     *
     * @param request 指派回显请求
     * @return 指派回显响应
     */
    @PostMapping("/detail")
    public ApiResponse<AssignmentDetailResponse> getAssignmentDetail(@RequestBody AssignmentDetailRequest request) {
        log.info("收到指派回显请求: {}", request);
        try {
            AssignmentDetailResponse response = assignmentDetailService.getAssignmentDetail(request);
            return ApiResponse.success("获取指派回显成功", response);
        } catch (Exception e) {
            log.error("获取指派回显失败", e);
            return ApiResponse.error("获取指派回显失败：" + e.getMessage());
        }
    }
}
