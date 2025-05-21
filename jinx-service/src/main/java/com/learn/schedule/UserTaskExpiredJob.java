package com.learn.schedule;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learn.constants.LearningStatus;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 用户学习任务过期处理定时任务
 * @author yujintao
 * @date 2025/5/20
 */
@Slf4j
@Component
public class UserTaskExpiredJob extends BaseJavaProcessor {

    private static final String JOB_NAME = "UserTaskExpiredJob";

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Override
    public String setJobName(JobContext context) {
        return JOB_NAME;
    }

    @Override
    public void execute(JobContext context) throws Exception {
        log.info("开始执行用户学习任务过期处理定时任务");
        
        // 当前时间
        Date now = new Date();
        
        // 1. 查询需要处理的记录：deadline不为空，且deadline小于当前时间，且状态不是EXPIRED的记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNotNull(UserLearningTask::getDeadline)
                .lt(UserLearningTask::getDeadline, now)
                .ne(UserLearningTask::getStatus, LearningStatus.EXPIRED)
                .ne(UserLearningTask::getStatus, LearningStatus.COMPLETED);
        
        List<UserLearningTask> expiredTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        if (expiredTasks.isEmpty()) {
            log.info("没有需要处理的过期学习任务");
            return;
        }
        
        log.info("找到{}条过期学习任务需要处理", expiredTasks.size());
        
        // 2. 批量更新状态为EXPIRED
        for (UserLearningTask task : expiredTasks) {
            // 更新状态为EXPIRED
            LambdaUpdateWrapper<UserLearningTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(UserLearningTask::getId, task.getId())
                    .set(UserLearningTask::getStatus, LearningStatus.EXPIRED)
                    .set(UserLearningTask::getGmtModified, now);
            
            int updated = userLearningTaskMapper.update(null, updateWrapper);
            if (updated > 0) {
                log.info("已将学习任务(ID:{}, 用户ID:{}, 业务类型:{}, 业务ID:{})状态更新为过期", 
                        task.getId(), task.getUserId(), task.getBizType(), task.getBizId());
            } else {
                log.warn("更新学习任务(ID:{})状态失败", task.getId());
            }
        }
        
        log.info("用户学习任务过期处理定时任务执行完成，共处理{}条记录", expiredTasks.size());
    }
}
