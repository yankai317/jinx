package com.learn.dto.train;

import lombok.Data;

import java.util.List;

/**
 * 培训列表项DTO
 */
@Data
public class TrainListDTO {
    /**
     * 培训ID
     */
    private Long id;

    /**
     * 培训名称
     */
    private String title;


    /**
     * 封面图URL
     */
    private String cover;

    /**
     * 学分
     */
    private Integer credit;

    /**
     * 状态
     */
    private String status;

    /**
     * 证书ID
     */
    private Long certificateId;

    /**
     * 证书名称
     */
    private String certificateName;

    /**
     * 创建人
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 创建时间
     */
    private String gmtModified;

    /**
     * 分类名称列表
     */
    private List<String> categoryNames;

    /**
     * 类目id
     */
    private String categoryIds;

    /**
     * 学习人数
     */
    private Integer learnerCount;

    /**
     * 完成人数
     */
    private Integer completionCount;
}
