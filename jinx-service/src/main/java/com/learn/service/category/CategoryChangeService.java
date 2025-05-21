package com.learn.service.category;

import com.learn.dto.common.CategoryChangeRequest;

/**
 * 类目变更服务接口
 *
 * @author yujintao
 * @date 2025/5/10
 */
public interface CategoryChangeService {
    
    /**
     * 更新业务对象的类目信息
     * 
     * @param request 类目变更请求
     * @return 是否更新成功
     */
    boolean changeCategory(CategoryChangeRequest request);
}
