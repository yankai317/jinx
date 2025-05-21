package com.learn.dto.toc.certificate;

import lombok.Data;

/**
 * 获取用户证书列表请求
 */
@Data
public class UserCertificatesRequest {
    /**
     * 证书状态：all-全部，valid-有效，expired-已过期，revoked-已撤销，默认全部
     */
    private String status;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;/**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}
