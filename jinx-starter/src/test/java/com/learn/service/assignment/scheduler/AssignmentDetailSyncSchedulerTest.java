package com.learn.service.assignment.scheduler;

import com.learn.StarterApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = StarterApplication.class)
class AssignmentDetailSyncSchedulerTest {
    @Resource
    private AssignmentDetailSyncScheduler assignmentDetailSyncScheduler;
    @Test
    void manualSyncAssignmentRangeToDetail() {
        assignmentDetailSyncScheduler.manualSyncAssignmentRangeToDetail();

    }
}
