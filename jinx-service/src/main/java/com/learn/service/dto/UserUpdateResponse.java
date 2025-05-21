package com.learn.service.dto;

import com.learn.service.dto.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户更新响应对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdateResponse extends BaseResponse {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 是否更新成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
