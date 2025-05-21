package com.learn.controller.dingtalk;

import com.learn.dto.common.ApiResponse;
import com.learn.service.dingtalk.scheduler.DingTalkSyncScheduler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指派明细控制器
 * 提供手动触发同步任务的接口
 */
@RestController
@RequestMapping("/api/dingtalk")
@Slf4j
public class DingTalkController {

    @Resource
    private DingTalkSyncScheduler dingTalkSyncScheduler;

    /**
     * 手动触发同步指派范围到指派明细
     * 用于测试或手动触发同步
     *
     * @return API响应
     */
    @PostMapping("/sync")
    public ApiResponse<Void> syncDingTalkController() {
        log.info("收到手动触发指派明细同步请求");
        try {
            dingTalkSyncScheduler.scheduledSyncDingTalkData();
            return ApiResponse.success( "同步成功", null );
        } catch (Exception e) {
            log.error("手动触发指派明细同步失败", e);
            return ApiResponse.error("同步失败：" + e.getMessage());
        }
    }

}
