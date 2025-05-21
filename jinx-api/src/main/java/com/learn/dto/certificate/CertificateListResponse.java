package com.learn.dto.certificate;

import lombok.Data;

import java.util.List;

/**
 * 证书列表查询响应
 */
@Data
public class CertificateListResponse {
    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 证书列表
     */
    private List<CertificateDTO> list;
}
