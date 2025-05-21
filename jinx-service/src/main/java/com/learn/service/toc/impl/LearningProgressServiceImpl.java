package com.learn.service.toc.impl;

import com.learn.constants.BizType;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.learning.RecordLearningProgressResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.toc.LearningProgressService;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

/**
 * 学习进度服务实现类
 */
@Service
@Slf4j
public class LearningProgressServiceImpl implements LearningProgressService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private UserStudyService userStudyService;

    @Override
    public RecordLearningProgressResponse recordProgress(Long userId, RecordLearningProgressRequest request) {
        log.info("记录学习进度: userId={}, request={}", userId, request);
        
        // 验证内容是否存在
        if (!validateContent(request)) {
            throw new IllegalArgumentException("内容不存在: type=" + request.getParentType() + ", contentId=" + request.getContentId());
        }
        
        // 查询或创建学习任务记录
        UserLearningTask learningTask = userStudyService.getOrCreateLearningTask(userId, request);

        // 更新学习进度
        userStudyService.updateLearningProgress(learningTask, request);
        
        // 构建响应
        return buildResponse(learningTask);
    }

    @Override
    public boolean validateContent(RecordLearningProgressRequest request) {
        if (BizType.SERIES_COURSE.equals(request.getContentType())) {
            // 兼容代码
            request.setContentType(BizType.COURSE);
        }
        String contentType = request.getContentType();
        Long contentId = request.getContentId();
        if (contentId == null) {
            return false;
        }

        switch (contentType) {
            case BizType.COURSE:
                // 验证课程是否存在
                Courses course = coursesMapper.selectById(contentId);
                return course != null && course.getIsDel() != 1;
            case BizType.TRAIN:
                // 验证培训是否存在
                Train train = trainMapper.selectById(contentId);
                return train != null && train.getIsDel() != 1;
            case BizType.LEARNING_MAP:
                // 验证学习地图是否存在
                LearningMap map = learningMapMapper.selectById(contentId);
                return map != null && map.getIsDel() != 1;
            case BizType.MAP_STAGE:
                // 验证学习地图是否存在
                LearningMapStage mapStage = learningMapStageMapper.selectById(contentId);
                return mapStage != null && mapStage.getIsDel() != 1;
            case BizType.APPENDIX_FILE:
                // 验证附件是否存在
                if (request.getParentId() == null) {
                    return false;
                }
                // 构建查询条件
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContentRelation> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                wrapper.eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                       .eq(ContentRelation::getContentId, contentId)
                       .eq(ContentRelation::getBizType, BizType.COURSE)
                       .eq(ContentRelation::getBizId, request.getParentId())
                       .eq(ContentRelation::getIsDel, 0); // 未删除的记录
                // 查询附件是否存在
                ContentRelation contentRelation = contentRelationMapper.selectOne(wrapper);
                return contentRelation != null;
            default:
                return false;
        }
    }

    /**
     * 构建响应对象
     */
    private RecordLearningProgressResponse buildResponse(UserLearningTask learningTask) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String status = learningTask.getStatus();
        
        return RecordLearningProgressResponse.builder()
                .progress(learningTask.getProgress())
                .studyDuration(learningTask.getStudyDuration())
                .status(status)
                .lastStudyTime(sdf.format(learningTask.getLastStudyTime()))
                .build();
    }
}
