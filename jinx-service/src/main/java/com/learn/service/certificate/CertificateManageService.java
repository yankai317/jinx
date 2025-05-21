package com.learn.service.certificate;

import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.UserCertificate;

import java.util.List;

/**
 * @author yujintao
 * @description 定义证书管理接口，证书的创建/删除/编辑
 * @date 2025/4/21
 */
public interface CertificateManageService {
    
    /**
     * 创建证书
     *
     * @param certificate 证书信息
     * @param creatorId 创建人ID
     * @param creatorName 创建人名称
     * @return 创建的证书
     */
    Certificate createCertificate(Certificate certificate, Long creatorId, String creatorName);
    
    /**
     * 更新证书
     *
     * @param certificate 证书信息
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 更新后的证书
     */
    Certificate updateCertificate(Certificate certificate, Long updaterId, String updaterName);
    
    /**
     * 删除证书
     *
     * @param id 证书ID
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 是否删除成功
     */
    boolean deleteCertificate(Long id, Long updaterId, String updaterName);
    
    /**
     * 撤销用户证书
     *
     * @param userCertificateId 用户证书ID
     * @param reason 撤销原因
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 撤销后的用户证书
     */
    UserCertificate revokeCertificate(Long userCertificateId, String reason, Long updaterId, String updaterName);
    
    /**
     * 批量撤销用户证书
     *
     * @param userCertificateIds 用户证书ID列表
     * @param reason 撤销原因
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 撤销后的用户证书列表
     */
    void batchRevokeCertificate(List<Long> userCertificateIds, String reason, Long updaterId, String updaterName);
}
