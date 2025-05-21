package com.learn.service.toc;

import com.learn.dto.toc.certificate.CertificateDetailResponse;
import com.learn.dto.toc.certificate.UserCertificatesRequest;
import com.learn.dto.toc.certificate.UserCertificatesResponse;

/**
 * 用户证书服务接口
 */
public interface UserCertificateService {
    
    /**
     * 获取用户证书列表
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @return 用户证书列表
     */
    UserCertificatesResponse getUserCertificates(Long userId, UserCertificatesRequest request);
    
    /**
     * 获取证书详情
     *
     * @param userId 用户ID
     * @param certificateId 用户证书ID
     * @return 证书详情
     */
    CertificateDetailResponse getCertificateDetail(Long userId, Long certificateId);
    
    /**
     * 下载证书
     *
     * @param userId 用户ID
     * @param certificateId 用户证书ID
     * @return 证书下载URL
     */
    String downloadCertificate(Long userId, Long certificateId);
}
