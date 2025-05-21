package com.learn.dto.certificate;

import lombok.Data;

/**
 * 证书DTO
 */
@Data
public class CertificateDTO {
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
     * 过期时间
     */
    private String expireTime;

    /**
     * 创建人
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 已发放数量
     */
    private Integer issuedCount;
}
