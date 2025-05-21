package com.learn.dto.certificate;

import lombok.Data;

/**
 * 获取证书获得者列表请求
 */
@Data
public class CertificateUsersRequest {
    /**
     * 部门ID
     */
    private Long departmentId;
    
    /**
     * 状态：all, valid, expired, revoked
     */
    private String status;
    
    /**
     * 来源类型：all, train, map
     */
    private String sourceType;
    
    /**
     * 关键词(用户名/工号)
     */
    private String keyword;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum;/**
     * 每页条数，默认10
     */
    private Integer pageSize;
}
