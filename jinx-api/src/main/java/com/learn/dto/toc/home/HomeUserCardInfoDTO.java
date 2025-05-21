package com.learn.dto.toc.home;

import lombok.Data;

/**
 * 用户信息DTO
 */
@Data
public class HomeUserCardInfoDTO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatar;
    
    /**
     * 工号
     */
    private String employeeNo;
    
    /**
     * 部门名称
     */
    private String departmentName;
    
    /**
     * 总学习时长（分钟）
     */
    private Integer totalStudyDuration;
    
    /**
     * 已获得证书数量
     */
    private Integer acquireCertificateCount;
    /**
     * 总学习分数
     */
    private Long totalLearningScore;
    /**
     * 总完成培训数量
     */
    private Integer totalFinishedTrainCount;
    /**
     * 总完成学习地图数量
     */
    private Integer totalFinishedLearningMapCount;

}
