package com.learn.service.train;

import com.learn.dto.train.TrainCreateRequest;
import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.user.UserInfoResponse;

/**
 * @author yujintao
 * @description 定义培训管理接口，例如培训的创建
 * @date 2025/4/21
 */
public interface TrainCreateService {
    
    /**
     * 创建培训
     * 1. 创建培训基本信息
     * 2. 关联培训内容
     * 3. 设置培训可见范围和协同管理
     *
     * @param request 创建培训请求
     * @return 创建的培训详情
     */
    TrainDetailDTO createTrain(TrainCreateRequest request, UserInfoResponse userInfo);
}
