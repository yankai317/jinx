package com.learn.service.train.impl;

import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.course.sub.TargetDTO;
import com.learn.dto.train.TrainVisibilityRequest;
import com.learn.infrastructure.repository.entity.OperationLog;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.TrainOperationLogMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.dto.CommonRangeUpdateRequest;
import com.learn.service.dto.CommonRangeUpdateResponse;
import com.learn.service.train.TrainVisibilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 培训可见范围设置服务实现类
 */
@Service
@Slf4j
public class TrainVisibilityServiceImpl implements TrainVisibilityService {

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private TrainOperationLogMapper trainOperationLogMapper;


    /**
     * 获取培训可见范围
     *
     * @param trainId 培训ID
     * @return 可见范围设置
     */
    @Override
    public TrainVisibilityRequest getTrainVisibility(Long trainId) {
        // 参数校验
        if (trainId == null || trainId <= 0) {
            throw new CommonException("培训ID不能为空或小于等于0");
        }

        // 查询培训可见范围
        CommonRangeQueryRequest queryRequest = new CommonRangeQueryRequest();
        queryRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        queryRequest.setType(BizType.TRAIN);
        queryRequest.setTypeId(trainId);

        CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(queryRequest);

        // 构建可见范围设置
        TrainVisibilityRequest visibility = new TrainVisibilityRequest();

        // 如果没有配置可见范围，默认为全员可见
        if (response == null || 
            (CollectionUtils.isEmpty(response.getDepartmentIds()) && 
             CollectionUtils.isEmpty(response.getRoleIds()) && 
             CollectionUtils.isEmpty(response.getUserIds()))) {
            visibility.setType("ALL");
            visibility.setTargets(new ArrayList<>());
            return visibility;
        }

        // 设置为部分可见
        visibility.setType("PART");
        List<TargetDTO> targets = new ArrayList<>();

        // 添加部门目标
        if (!CollectionUtils.isEmpty(response.getDepartmentIds())) {
            TargetDTO target = new TargetDTO();
            target.setType("department");
            target.setIds(response.getDepartmentIds());
            targets.add(target);
        }

        // 添加角色目标
        if (!CollectionUtils.isEmpty(response.getRoleIds())) {
            TargetDTO target = new TargetDTO();
            target.setType("role");
            target.setIds(response.getRoleIds());
            targets.add(target);
        }

        // 添加用户目标
        if (!CollectionUtils.isEmpty(response.getUserIds())) {
            TargetDTO target = new TargetDTO();
            target.setType("user");
            target.setIds(response.getUserIds());
            targets.add(target);
        }

        visibility.setTargets(targets);
        return visibility;
    }

}
