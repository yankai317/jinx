package com.learn.service.assignment.scheduler;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.learn.schedule.BaseJavaProcessor;
import com.learn.service.assignment.AssignmentDetailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 指派明细同步定时任务
 * 定期将指派范围中的部门和角色转换为具体的人员，并更新到指派明细表
 */
@Component
@Slf4j
public class AssignmentDetailSyncScheduler extends BaseJavaProcessor {

    @Resource
    private AssignmentDetailService assignmentDetailService;

    /**
     * 手动触发同步指派范围到指派明细
     * 用于测试或手动触发同步
     * 
     * @return 新增的指派明细数量
     */
    public int manualSyncAssignmentRangeToDetail() {
        log.info("手动触发指派明细同步任务");
        try {
            int insertedCount = assignmentDetailService.syncAssignmentRangeToDetail();
            log.info("指派明细同步任务执行完成，新增{}条记录", insertedCount);
            return insertedCount;
        } catch (Exception e) {
            log.error("指派明细同步任务执行失败", e);
            throw e;
        }
    }

    @Override
    public String setJobName(JobContext context) {
        return "AssignmentDetailSyncJob";
    }

    @Override
    public void execute(JobContext context) throws Exception {
        this.manualSyncAssignmentRangeToDetail();
    }
}
