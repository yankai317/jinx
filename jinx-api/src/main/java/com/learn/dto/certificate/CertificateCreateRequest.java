package com.learn.dto.certificate;

import lombok.Data;

/**
 * 创建证书请求DTO
 */
@Data
public class CertificateCreateRequest {
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
