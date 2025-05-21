package com.learn.service.train;

import com.learn.common.dto.UserTokenInfo;
import com.learn.dto.train.TrainBatchDeleteResponse;

import java.util.List;

/**
 * @author yujintao
 * @description 定义培训管理接口，例如培训的删除和批量删除
 * @date 2025/4/21
 */
public interface TrainDeleteService {
    
    /**
     * 删除培训
     *
     * @param id 培训ID
     * @return 是否删除成功
     */
    boolean deleteTrain(Long id, UserTokenInfo userInfo);
    
    /**
     * 批量删除培训
     *
     * @param ids 培训ID列表
     * @return 批量删除结果
     */
    TrainBatchDeleteResponse batchDeleteTrain(List<Long> ids, UserTokenInfo userInfo);
}
