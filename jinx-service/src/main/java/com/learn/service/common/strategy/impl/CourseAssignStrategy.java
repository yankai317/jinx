package com.learn.service.common.strategy.impl;

import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.strategy.AssignStrategy;
import com.learn.service.course.CourseUserService;
import com.learn.service.dto.CommonRangeQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.learn.constants.BizType.COURSE;
import static com.learn.constants.BizType.LEARNING_MAP;

/**
 * 课程指派策略实现
 */
@Component
@Slf4j
public class CourseAssignStrategy implements AssignStrategy {

    @Autowired
    private CourseUserService courseUserService;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Override
    public AssignResponse assign(AssignRequest request) {
        commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                .setType(COURSE)
                .setTypeId(request.getBizId())
                .setUserId(request.getOperatorId().toString())
                .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
        log.info("执行课程指派策略，课程ID: {}", request.getBizId());
        return courseUserService.assignCourse(request);
    }

    @Override
    public String getBizType() {
        return COURSE;
    }
}
