package com.learn.service.common.strategy.impl;

import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.strategy.AssignStrategy;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.map.LearnMapAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.learn.constants.BizType.LEARNING_MAP;
import static com.learn.constants.BizType.TRAIN;

/**
 * 学习地图指派策略实现
 */
@Component
@Slf4j
public class LearningMapAssignStrategy implements AssignStrategy {

    @Autowired
    private LearnMapAssignService learnMapAssignService;

    @Autowired
    private CommonRangeInterface commonRangeInterface;
    @Override
    public AssignResponse assign(AssignRequest request) {

        commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                .setType(LEARNING_MAP)
                .setTypeId(request.getBizId())
                .setUserId(request.getOperatorId().toString())
                .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
        log.info("执行学习地图指派策略，学习地图ID: {}", request.getBizId());
        return learnMapAssignService.assignLearningMap(request);
    }

    @Override
    public String getBizType() {
        return LEARNING_MAP;
    }
}
