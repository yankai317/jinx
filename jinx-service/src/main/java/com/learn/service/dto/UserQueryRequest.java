package com.learn.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 查询用户请求对象
 */
@Data
@Accessors(chain = true)
public class UserQueryRequest {
    /**
     * 用户id列表
     */
    private List<Long> ids;
    /**
     * 用户ID列表
     */
    private List<Long> userIds;

    /**
     * 包含删除数据
     */
    private boolean containDeleted;
    /**
     * 第三方平台类型，如：dingtalk、feishu
     */
    private String thirdPartyType;

    /**
     * 第三方平台用户唯一标识列表
     */
    private List<String> thirdPartyUserIds;
    
    /**
     * 关键词搜索，可匹配用户昵称或工号
     */
    private String keyword;
    
    /**
     * 页码，从1开始
     */
    private Integer pageNum;

    /**
     * 仅查询管理员
     */
    private Boolean queryAdmin;
    
    /**
     * 每页记录数
     */
    private Integer pageSize;
}
