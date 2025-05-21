package com.learn.dto.train;

import java.util.List;

/**
 * @author yujintao
 * @Description 统计数据DTO
 * @date 2025/4/21
 */
public class StatisticsDTO {

    private Long id;

    /**
     * 学习人数
     */
    private Integer learnerCount;

    /**
     * 完成人数
     */
    private Integer completionCount;

    /**
     * 完成率
     */
    private Float completionRate;

    /**
     * 平均学习时长(分钟)
     */
    private Integer avgDuration;

    /**
     * 总学习时长(分钟)
     */
    private Integer totalDuration;

    /**
     * 课程完成率
     */
    private Float courseCompletionRate;

    /**
     * 考试完成率
     */
    private Float examCompletionRate;

    /**
     * 作业完成率
     */
    private Float assignmentCompletionRate;

    /**
     * 调研完成率
     */
    private Float surveyCompletionRate;

    /**
     * 部门统计
     */
    private List<DepartmentStat> departmentStats;

    /**
     * 时间分布
     */
    private List<TimeDistribution> timeDistribution;

    /**
     * 部门统计数据
     */
    public static class DepartmentStat {
        /**
         * 部门名称
         */
        private String name;

        /**
         * 学习人数
         */
        private Integer count;

        /**
         * 完成人数
         */
        private Integer completionCount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Integer getCompletionCount() {
            return completionCount;
        }

        public void setCompletionCount(Integer completionCount) {
            this.completionCount = completionCount;
        }
    }

    /**
     * 时间分布数据
     */
    public static class TimeDistribution {
        /**
         * 日期
         */
        private String date;

        /**
         * 学习人数
         */
        private Integer count;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public Integer getLearnerCount() {
        return learnerCount;
    }

    public void setLearnerCount(Integer learnerCount) {
        this.learnerCount = learnerCount;
    }

    public Integer getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(Integer completionCount) {
        this.completionCount = completionCount;
    }

    public Float getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Float completionRate) {
        this.completionRate = completionRate;
    }

    public Integer getAvgDuration() {
        return avgDuration;
    }

    public void setAvgDuration(Integer avgDuration) {
        this.avgDuration = avgDuration;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Float getCourseCompletionRate() {
        return courseCompletionRate;
    }

    public void setCourseCompletionRate(Float courseCompletionRate) {
        this.courseCompletionRate = courseCompletionRate;
    }

    public Float getExamCompletionRate() {
        return examCompletionRate;
    }

    public void setExamCompletionRate(Float examCompletionRate) {
        this.examCompletionRate = examCompletionRate;
    }

    public Float getAssignmentCompletionRate() {
        return assignmentCompletionRate;
    }

    public void setAssignmentCompletionRate(Float assignmentCompletionRate) {
        this.assignmentCompletionRate = assignmentCompletionRate;
    }

    public Float getSurveyCompletionRate() {
        return surveyCompletionRate;
    }

    public void setSurveyCompletionRate(Float surveyCompletionRate) {
        this.surveyCompletionRate = surveyCompletionRate;
    }

    public List<DepartmentStat> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(List<DepartmentStat> departmentStats) {
        this.departmentStats = departmentStats;
    }

    public List<TimeDistribution> getTimeDistribution() {
        return timeDistribution;
    }

    public void setTimeDistribution(List<TimeDistribution> timeDistribution) {
        this.timeDistribution = timeDistribution;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
