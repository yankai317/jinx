package com.learn.service.dto.org;

import com.learn.service.dto.base.BaseRequest;
import lombok.Data;

import java.util.List;

/**
 * 组织请求对象
 */
@Data
public class OrgRequest  {
    private Long id;
    private Integer pageSize;
    private Integer pageNum;
    private String keyword;
    private List<Long> orgIds;
    /**
     * 是否包含用户信息
     */
    private Boolean includeUsers;

} 
