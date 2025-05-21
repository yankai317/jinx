package com.learn.dto.certificate;

import lombok.Data;

/**
 * 更新证书请求
 */
@Data
public class CertificateUpdateRequest {
    /**
     * 证书ID
     */
    private Long id;
    
    /**
     * 证书名称
     */
    private String name;
    
    /**
     * 证书描述
     */
    private String description;
    
    /**
     * 证书模板URL
     */
    private String templateUrl;
    
    /**
     * 过期时间，不填则永不过期
     */
    private String expireTime;
}
