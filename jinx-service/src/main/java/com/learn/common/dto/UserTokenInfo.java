package com.learn.common.dto;

import com.learn.dto.user.UserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户令牌信息
 * 包含用户ID和企业ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenInfo {
    
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名称
     */
    private String name;

    /**
     * 企业ID
     */
    private String corpId;
}
