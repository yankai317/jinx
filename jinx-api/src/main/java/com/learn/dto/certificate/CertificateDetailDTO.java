package com.learn.dto.certificate;

import lombok.Data;
import java.util.List;

/**
 * 证书详情DTO
 */
@Data
public class CertificateDetailDTO {
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
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 已发放数量
     */
    private Long issuedCount;

    /**
     * 使用情况
     */
    private List<CertificateUsageDTO> usages;

    /**
     * 证书使用情况DTO
     */
    @Data
    public static class CertificateUsageDTO {
        /**
         * 使用类型：train-培训, map-学习地图
         */
        private String type;

        /**
         * 使用ID
         */
        private Long id;

        /**
         * 使用名称
         */
        private String name;
    }
}
