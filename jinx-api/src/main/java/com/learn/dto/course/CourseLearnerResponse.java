package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 课程学习人员列表响应
 */
@Data
public class CourseLearnerResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 学习人员列表
     */
    private List<LearnerDTO> list;
    
    /**
     * 学习人员信息
     */
    @Data
    public static class LearnerDTO {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户姓名
         */
        private String userName;
        
        /**
         * 工号
         */
        private String employeeNo;
        
        /**
         * 部门
         */
        private String department;
        
        /**
         * 学习时长(分钟)
         */
        private Integer studyDuration;
        
        /**
         * 是否完成
         */
        private Boolean completed;

        /**
         * 学习进度
         */
        private Integer progress;

        private String startTime;

        private String completionTime;

        private String status;
        
        /**
         * 最后学习时间
         */
        private String lastStudyTime;
    }
}
