package com.learn.dto.map;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学习地图DTO
 */
@Data
public class LearningMapDTO {
    /**
     * 地图ID
     */
    private Long id;
    
    /**
     * 地图名称
     */
    private String name;
    
    /**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 必修学分
     */
    private Integer requiredCredit;/**
     * 选修学分
     */
    private Integer electiveCredit;/**
     * 证书规则：0-不发放 1-整体发放 2-按阶段发放
     */
    private Integer certificateRule;
    
    /**
     * 证书ID
     */
    private Long certificateId;
    
    /**
     * 证书名称
     */
    private String certificateName;
    
    /**
     * 开放开始时间
     */
    private String startTime;
    
    /**
     * 开放结束时间
     */
    private String endTime;
    
    /**
     * 解锁方式：0-按阶段和任务 1-按阶段 2-自由模式
     */
    private Integer unlockMode;
    
    /**
     * 主题
     */
    private String theme;
    
    /**
     * 创建人
     */
    private String creatorName;
    
    /**
     * 创建时间
     */
    private String gmtCreate;
    
    /**
     * 分类名称列表
     */
    private List<String> categoryNames;

    /**
     * 类目id
     */
    private String categoryIds;

    /**
     * 阶段数量
     */
    private Integer stageCount;
    
    /**
     * 学习人数
     */
    private Integer learnerCount;
    
    /**
     * 完成人数
     */
    private Integer completionCount;
    /**
     * 是否开启自动分配
     */
    private Boolean enableAutoAssign;
}
