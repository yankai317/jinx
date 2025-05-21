package com.learn.dto.toc.certificate;

import lombok.Data;

/**
 * 证书详情响应
 */
@Data
public class CertificateDetailResponse {
    /**
     * 用户证书ID
     */
    private Long id;
    
    /**
     * 证书ID
     */
    private Long certificateId;
    
    /**
     * 证书名称
     */
    private String name;
    
    /**
     * 证书编号
     */
    private String certificateNo;
    
    /**
     * 证书模板URL
     */
    private String templateUrl;
    
    /**
     * 颁发时间
     */
    private String issueTime;
    
    /**
     * 过期时间，null表示永不过期
     */
    private String expireTime;
    
    /**
     * 证书来源类型：train-培训, map-学习地图, exam-考试
     */
    private String sourceType;
    
    /**
     * 证书来源名称
     */
    private String sourceName;
    
    /**
     * 证书来源ID
     */
    private Long sourceId;
    
    /**
     * 状态：0-有效，1-已过期，2-已撤销
     */
    private Integer status;
    
    /**
     * 证书描述
     */
    private String description;
}
