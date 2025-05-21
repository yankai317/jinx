package com.learn.service.train;

import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.dto.train.TrainAssignRequest;
import com.learn.dto.train.TrainAssignResponse;

/**
 * @author yujintao
 * @Description 定义培训的指派接口，例如培训的指派
 * @date 2025/4/21
 */
public interface TrainAssignService {
    /**
     * 指派培训给用户
     * 1. 创建培训指派记录
     * 2. 为每个用户创建学习完成记录和学习记录
     * 3. 记录操作日志
     *
     * @param request 指派培训请求
     * @return 指派培训响应
     */
    AssignResponse assignTrain(AssignRequest request);
}
