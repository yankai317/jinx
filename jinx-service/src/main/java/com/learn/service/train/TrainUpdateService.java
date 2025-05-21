package com.learn.service.train;

import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.train.TrainUpdateRequest;

import java.util.List;

/**
 * @author yujintao
 * @description 定义培训管理接口，例如培训的编辑相关操作
 * @date 2025/4/21
 */
public interface TrainUpdateService {
    /**
     * 更新培训信息
     *
     * @param request 更新培训请求
     * @return 更新后的培训详情
     */
    boolean updateTrain(TrainUpdateRequest request, Long userId);


    /**
     * 批量更新创建人
     * @param ids
     * @param newCreatorId
     * @param newCreatorName
     * @param operatorId
     * @param operatorName
     * @return
     */
    boolean batchUpdateCreator(List<Long> ids, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName);
}
