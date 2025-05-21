package com.learn.service.common.strategy.impl;

import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.strategy.AssignStrategy;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.train.TrainAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.learn.constants.BizType.TRAIN;

/**
 * 培训指派策略实现
 */
@Component
@Slf4j
public class TrainAssignStrategy implements AssignStrategy {

    @Autowired
    private TrainAssignService trainAssignService;
    @Autowired
    private CommonRangeInterface commonRangeInterface;


    @Override
    public AssignResponse assign(AssignRequest request) {
        log.info("执行培训指派策略，培训ID: {}", request.getBizId());

        Long userId = request.getOperatorId();
        commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                .setType(TRAIN)
                .setTypeId(request.getBizId())
                .setUserId(userId.toString())
                .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
        return trainAssignService.assignTrain(request);
    }

    @Override
    public String getBizType() {
        return BizType.TRAIN;
    }
}
