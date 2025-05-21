package com.learn.service.dingtalk.scheduler;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.learn.schedule.BaseJavaProcessor;
import com.learn.service.dingtalk.DingTalkDepartmentSyncService;
import com.learn.service.dingtalk.DingTalkRoleSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 钉钉数据同步定时任务
 *按照以下顺序执行：
 * 1. 先同步部门
 * 2. 然后同步部门下的人
 * 3. 然后同步角色
 * 4. 最后同步角色下的人
 */
@Component
@Slf4j
public class DingTalkSyncScheduler extends BaseJavaProcessor {

    @Autowired
    private DingTalkDepartmentSyncService dingTalkDepartmentSyncService;

    @Autowired
    private DingTalkRoleSyncService dingTalkRoleSyncService;

    /**
     * 定时同步钉钉数据
     * cron表达式：秒 分 时 日 月 周
     * 默认每天凌晨2点执行
     */
//    @Scheduled(cron = "11 13 1 * * ?")
    public void scheduledSyncDingTalkData() {
        log.info("开始执行钉钉数据同步定时任务");
        try {
//             1. 先同步部门
//            log.info("开始同步钉钉部门");
//            dingTalkDepartmentSyncService.syncDepartments();
//            log.info("钉钉部门同步完成");

            // 2. 然后同步部门下的人
            log.info("开始同步钉钉部门用户");
            dingTalkDepartmentSyncService.syncDepartmentUsers();
            log.info("钉钉部门用户同步完成");

//            // 3. 然后同步角色
//            log.info("开始同步钉钉角色");
//            dingTalkRoleSyncService.syncRoles();
//            log.info("钉钉角色同步完成");
//
//            // 4. 最后同步角色下的人
//            log.info("开始同步钉钉角色用户");
//            dingTalkRoleSyncService.syncRoleUsers();
//            log.info("钉钉角色用户同步完成");

            log.info("钉钉数据同步定时任务执行完成");
        } catch (Exception e) {
            log.error("钉钉数据同步定时任务执行异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String setJobName(JobContext context) {
        return "DingTalkSyncJob";
    }

    @Override
    public void execute(JobContext context) throws Exception {
        this.scheduledSyncDingTalkData();
    }
}
