package com.learn.dto.certificate;

import lombok.Data;

/**
 * 撤销证书请求
 */
@Data
public class CertificateRevokeRequest {
    /**
     * 用户证书ID
     */
    private Long userCertificateId;
    
    /**
     * 撤销原因
     */
    private String reason;
}
