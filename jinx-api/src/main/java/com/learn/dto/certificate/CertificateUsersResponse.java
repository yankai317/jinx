package com.learn.dto.certificate;

import lombok.Data;

import java.util.List;

/**
 * 获取证书获得者列表响应
 */
@Data
public class CertificateUsersResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 证书获得者列表
     */
    private List<CertificateUserDTO> list;
    
    /**
     * 证书获得者DTO
     */
    @Data
    public static class CertificateUserDTO {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户姓名
         */
        private String userName;
        
        /**
         * 工号
         */
        private String employeeNo;
        
        /**
         * 部门
         */
        private String department;
        
        /**
         * 证书编号
         */
        private String certificateNo;
        
        /**
         * 来源类型
         */
        private String sourceType;
        
        /**
         * 来源名称
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
         * 状态：0-有效 1-已过期 2-已撤销
         */
        private Integer status;

        private String statusName;

        public String getStatusName() {
            if (null == this.status) {
                return "";
            }
            if (status == 0) {
                return "有效";
            } else  {
                return "已失效";
            }
        }
    }
}
