package com.learn.schedule;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.infrastructure.repository.entity.AssignmentDetail;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.AssignmentDetailMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户学习指派任务定时处理
 * @author yujintao
 * @date 2025/5/7
 */
@Slf4j
@Component
public class UserLearnAssignJob extends BaseJavaProcessor {

    private static final String JOB_NAME = "UserLearnAssignJob";


    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;

    @Autowired
    private UserStudyService userStudyService;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;


    @Override
    public String setJobName(JobContext context) {
        return JOB_NAME;
    }

    @Override
    public void execute(JobContext context) throws Exception {
        log.info("开始执行指派记录生成学习记录任务");

        try {
            // 1. 查询最近十五分钟的 status = 1的 assignment_detail
            List<AssignmentDetail> assignmentDetails = queryRecentAssignmentDetails();
            if (CollectionUtils.isEmpty(assignmentDetails)) {
                log.info("没有需要处理的指派记录");
                return;
            }

            log.info("查询到{}条需要处理的指派记录", assignmentDetails.size());

            // 2. 遍历指派记录，生成学习任务
            Map<String, AssignmentDetail> assignmentDetailMap = assignmentDetails.stream()
                    .collect(Collectors.toMap(v -> v.getType() + "_" + v.getTypeId() + "_" + v.getUserid().toString(),
                            v -> v));
            List<String> searchKeys = assignmentDetailMap.keySet().stream().toList();
            List<UserLearningTask> tasks = getUserLearningTasks(searchKeys);
            Map<String, UserLearningTask> taskMap = tasks.stream()
                    .collect(Collectors.toMap(UserLearningTask::getSearchKey,
                            v -> v));

            for (Map.Entry<String, AssignmentDetail> entry : assignmentDetailMap.entrySet()) {
                AssignmentDetail detail = entry.getValue();
                try {
                    UserLearningTask task = taskMap.get(entry.getKey());
                    if (Objects.isNull(task)) {
                        processAssignmentDetail(entry.getValue());
                    }
                    log.info("处理指派记录成功, id={}, bizId={}", detail.getId(), detail.getBizId());
                } catch (Exception e) {
                    log.error("处理指派记录失败, id={}, bizId={}", detail.getId(), detail.getBizId(), e);
                }
            }

            log.info("指派记录生成学习记录任务执行完成");
        } catch (Exception e) {
            log.error("指派记录生成学习记录任务执行异常", e);
        }
    }

    /**
     * 查询最近十五分钟的指派记录
     *
     * @return 指派记录列表
     */
    private List<AssignmentDetail> queryRecentAssignmentDetails() {
        // 计算15分钟前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -15);
        Date fiveMinutesAgo = calendar.getTime();

        // 查询条件：status = 1 且 创建时间在5分钟内
        LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignmentDetail::getStatus, "1")
                .ge(AssignmentDetail::getGmtCreate, fiveMinutesAgo)
                .eq(AssignmentDetail::getIsDel, 0);

        return assignmentDetailMapper.selectList(queryWrapper);
    }

    /**
     * 处理单条指派记录
     *
     * @param detail 指派记录
     */
    private void processAssignmentDetail(AssignmentDetail detail) {
        // 创建学习任务
        RecordLearningProgressRequest request = new RecordLearningProgressRequest();
        request.setContentId(detail.getTypeId());
        request.setContentType(detail.getType());
        request.setInitByAssign(true);
        userStudyService.getOrCreateLearningTask(detail.getUserid(), request);
    }

    /**
     * 检查用户是否已存在学习任务
     *
     * @param searchKeys 搜索key
     * @return 是否存在
     */
    private List<UserLearningTask> getUserLearningTasks(List<String> searchKeys) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserLearningTask::getSearchKey, searchKeys)
                .eq(UserLearningTask::getIsDel, 0);

        return userLearningTaskMapper.selectList(queryWrapper);
    }
}
