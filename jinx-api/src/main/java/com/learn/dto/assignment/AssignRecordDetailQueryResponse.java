package com.learn.dto.assignment;

import lombok.Data;

import java.util.Date;

/**
 * 指派记录明细查询响应
 */
@Data
public class AssignRecordDetailQueryResponse {
    /**
     * 明细ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名称
     */
    private String userName;
    
    /**
     * 通知状态：0-待通知、1-通知成功、2-通知失败
     */
    private String status;
    
    /**
     * 失败原因
     */
    private String failReason;
    
    /**
     * 创建时间
     */
    private Date gmtCreate;
    
    /**
     * 更新时间
     */
    private Date gmtModified;
}
