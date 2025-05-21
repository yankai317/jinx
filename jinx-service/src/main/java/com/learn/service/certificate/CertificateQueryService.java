package com.learn.service.certificate;

import com.learn.dto.certificate.CertificateDetailDTO;
import com.learn.dto.certificate.CertificateListRequest;
import com.learn.dto.certificate.CertificateListResponse;
import com.learn.dto.certificate.CertificateUsersRequest;
import com.learn.dto.certificate.CertificateUsersResponse;

/**
 * @author yujintao
 * @description 定义证书查询接口
 * @date 2025/4/21
 */
public interface CertificateQueryService {
    
    /**
     * 获取证书列表
     *
     * @param request 查询条件
     * @return 证书列表
     */
    CertificateListResponse getCertificateList(CertificateListRequest request);
    
    /**
     * 获取证书详情
     *
     * @param id 证书ID
     * @return 证书详情
     */
    CertificateDetailDTO getCertificateDetail(Long id);
    /**
     * 获取证书获得者列表
     *
     * @param id 证书ID
     * @param request 查询条件
     * @return 证书获得者列表
     */
    CertificateUsersResponse getCertificateUsers(Long id, CertificateUsersRequest request);
}
