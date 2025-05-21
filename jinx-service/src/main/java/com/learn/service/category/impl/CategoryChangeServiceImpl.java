package com.learn.service.category.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learn.constants.BizType;
import com.learn.dto.common.CategoryChangeRequest;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.service.category.CategoryChangeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 类目变更服务实现类
 *
 * @author yujintao
 * @date 2025/5/10
 */
@Service
@Slf4j
public class CategoryChangeServiceImpl implements CategoryChangeService {

    @Autowired
    private CoursesMapper coursesMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    /**
     * 更新业务对象的类目信息
     * 
     * @param request 类目变更请求
     * @return 是否更新成功
     */
    @Override
    public boolean changeCategory(CategoryChangeRequest request) {
        log.info("更新类目信息，业务ID：{}，业务类型：{}，类目：{}", 
                request.getBizId(), request.getBizType(), request.getCategoryIds());
        
        // 参数校验
        if (request.getBizId() == null || request.getBizId() <= 0) {
            log.error("业务ID不能为空且必须大于0");
            return false;
        }
        
        if (StringUtils.isBlank(request.getBizType())) {
            log.error("业务类型不能为空");
            return false;
        }
        
        if (StringUtils.isBlank(request.getCategoryIds())) {
            log.error("类目信息不能为空");
            return false;
        }
        
        // 将业务类型转为大写
        String bizType = request.getBizType().toUpperCase();
        
        try {
            boolean updated = false;
            
            // 根据业务类型更新对应的类目信息
            switch (bizType) {
                case BizType.COURSE:
                    // 更新课程类目
                    LambdaUpdateWrapper<Courses> courseWrapper = new LambdaUpdateWrapper<>();
                    courseWrapper.eq(Courses::getId, request.getBizId())
                            .set(Courses::getCategoryIds, request.getCategoryIds());
                    updated = coursesMapper.update(null, courseWrapper) > 0;
                    break;
                    
                case BizType.LEARNING_MAP:
                    // 更新学习地图类目
                    LambdaUpdateWrapper<LearningMap> mapWrapper = new LambdaUpdateWrapper<>();
                    mapWrapper.eq(LearningMap::getId, request.getBizId())
                            .set(LearningMap::getCategoryIds, request.getCategoryIds());
                    updated = learningMapMapper.update(null, mapWrapper) > 0;
                    break;
                    
                case BizType.TRAIN:
                    // 更新培训类目
                    LambdaUpdateWrapper<Train> trainWrapper = new LambdaUpdateWrapper<>();
                    trainWrapper.eq(Train::getId, request.getBizId())
                            .set(Train::getCategoryIds, request.getCategoryIds());
                    updated = trainMapper.update(null, trainWrapper) > 0;
                    break;
                    
                default:
                    log.error("不支持的业务类型：{}", bizType);
                    return false;
            }
            
            if (updated) {
                log.info("更新类目信息成功");
                return true;
            } else {
                log.error("未找到对应的业务对象");
                return false;
            }
        } catch (Exception e) {
            log.error("更新类目信息失败", e);
            return false;
        }
    }
}
