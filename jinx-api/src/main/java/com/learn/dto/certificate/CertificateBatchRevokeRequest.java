package com.learn.dto.certificate;

import lombok.Data;

import java.util.List;

/**
 * 批量撤销证书请求
 */
@Data
public class CertificateBatchRevokeRequest {
    /**
     * 用户证书ID列表
     */
    private List<Long> userCertificateIds;
    
    /**
     * 撤销原因
     */
    private String reason;
}
