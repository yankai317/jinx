package com.learn.dto.toc.certificate;

import lombok.Data;

import java.util.List;

/**
 * 获取用户证书列表响应
 */
@Data
public class UserCertificatesResponse {
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 证书列表
     */
    private List<UserCertificateDTO> list;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 用户证书DTO
     */
    @Data
    public static class UserCertificateDTO {
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
         * 证书来源类型
         */
        private String sourceType;

        /**
         * 证书来源类型
         */
        private String sourceTypeName;
        
        /**
         * 证书来源名称
         */
        private String sourceName;
        
        /**
         * 颁发时间
         */
        private String issueTime;
        
        /**
         * 过期时间
         */
        private String expireTime;
        
        /**
         * 状态：0-有效，1-已过期，2-已撤销
         */
        private Integer status;
        
        /**
         * 证书模板URL
         */
        private String templateUrl;
    }
}
