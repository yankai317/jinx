package com.learn.constants;

/**
 * @author yujintao
 * @date 2025/5/8
 */
public interface AttributeKey {

    String DURATION = "duration";

    String TYPE = "type";

    String TITLE = "title";

    String BIZ_TYPE = "biz_type";

    String APPENDIX_PATH = "appendixPath";

    String MAP_ID = "map_id";




    /**
     * 当前学习阶段id
     */
    String currentTaskId = "current_stage_id";

    /**
     * 已完成阶段数
     */
    String allCount = "all_count";

    /**
     * 已完成必修任务数
     */
    String completedRequiredCount = "completed_required_count";

    /**
     * 已完成选修任务数
     */
    String completedElectiveCount = "completed_elective_count";
}
