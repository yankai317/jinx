package com.learn.service.course.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.BizType;
import com.learn.dto.course.CourseRelateDTO;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.service.course.RelateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程关联服务实现类
 */
@Service
@Slf4j
public class RelateServiceImpl implements RelateService {

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    /**
     * 查询课程关联的内容
     *
     * @param bizId 课程ID
     * @return 关联内容列表
     */
    @Override
    public List<CourseRelateDTO> queryContentRelate(Long bizId, String type) {
        log.info("查询{}关联内容，课程ID：{}", type, bizId);

        List<CourseRelateDTO> result = new ArrayList<>();

        // 查询课程关联的内容
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getContentId, bizId)
                .eq(ContentRelation::getContentType, type)
                .eq(ContentRelation::getIsDel, 0);

        List<ContentRelation> contentRelations = contentRelationMapper.selectList(queryWrapper);

        if (contentRelations == null || contentRelations.isEmpty()) {
            return result;
        }

        // 获取所有培训ID
        List<Long> trainIds = contentRelations.stream()
                .filter(relation -> BizType.TRAIN.equals(relation.getBizType()))
                .map(ContentRelation::getBizId)
                .collect(Collectors.toList());

        // 获取所有地图阶段ID
        List<Long> stageIds = contentRelations.stream()
                .filter(relation -> BizType.MAP_STAGE.equals(relation.getBizType()))
                .map(ContentRelation::getBizId)
                .collect(Collectors.toList());

        // 查询培训信息
        Map<Long, Train> trainMap = new HashMap<>();
        if (!trainIds.isEmpty()) {
            LambdaQueryWrapper<Train> trainQueryWrapper = new LambdaQueryWrapper<>();
            trainQueryWrapper.in(Train::getId, trainIds)
                    .eq(Train::getIsDel, 0);
            List<Train> trains = trainMapper.selectList(trainQueryWrapper);
            trainMap = trains.stream().collect(Collectors.toMap(Train::getId, Function.identity()));
        }

        // 查询地图阶段信息
        Map<Long, LearningMapStage> stageMap = new HashMap<>();
        List<Long> mapIds = contentRelations.stream()
                .filter(relation -> BizType.LEARNING_MAP.equals(relation.getBizType()))
                .map(ContentRelation::getBizId)
                .collect(Collectors.toList());
        if (!stageIds.isEmpty()) {
            LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
            stageQueryWrapper.in(LearningMapStage::getId, stageIds)
                    .eq(LearningMapStage::getIsDel, 0);
            List<LearningMapStage> stages = learningMapStageMapper.selectList(stageQueryWrapper);
            stageMap = stages.stream().collect(Collectors.toMap(LearningMapStage::getId, Function.identity()));

            // 获取所有地图ID
            mapIds.addAll(stages.stream()
                    .map(LearningMapStage::getMapId)
                    .distinct()
                    .collect(Collectors.toList()));
        }

        // 查询学习地图信息
        Map<Long, LearningMap> mapMap = new HashMap<>();
        if (!mapIds.isEmpty()) {
            LambdaQueryWrapper<LearningMap> mapQueryWrapper = new LambdaQueryWrapper<>();
            mapQueryWrapper.in(LearningMap::getId, mapIds)
                    .eq(LearningMap::getIsDel, 0);
            List<LearningMap> maps = learningMapMapper.selectList(mapQueryWrapper);
            mapMap = maps.stream().collect(Collectors.toMap(LearningMap::getId, Function.identity()));
        }

        // 格式化日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 构建结果
        for (ContentRelation relation : contentRelations) {
            String bizType = relation.getBizType();
            Long relateBizId = relation.getBizId();

            CourseRelateDTO dto = new CourseRelateDTO();
            dto.setId(relation.getId());
            dto.setContentId(bizId);

            // 设置创建人信息
            dto.setCreatorId(relation.getCreatorId());
            dto.setCreatorName(relation.getCreatorName());

            // 设置创建时间
            if (relation.getGmtCreate() != null) {
                dto.setGmtCreate(dateFormat.format(relation.getGmtCreate()));
            }

            // 根据业务类型设置名称和类型
            if (BizType.TRAIN.equals(bizType) && trainMap.containsKey(relation.getBizId())) {
                Train train = trainMap.get(relation.getBizId());
                dto.setName(train.getName());
                dto.setType(BizType.TRAIN);
            } else if(BizType.LEARNING_MAP.equals(bizType) && mapMap.containsKey(relation.getBizId())) {
                LearningMap map = mapMap.get(relation.getBizId());
                dto.setName(map.getName());
                dto.setType(BizType.LEARNING_MAP);
            } else if (BizType.MAP_STAGE.equals(bizType) && stageMap.containsKey(relateBizId)) {
                LearningMapStage stage = stageMap.get(relateBizId);
                Long mapId = stage.getMapId();

                // 设置阶段名称
                dto.setName(stage.getName());

                // 如果地图存在，添加地图名称
                if (mapMap.containsKey(mapId)) {
                    LearningMap map = mapMap.get(mapId);
                    dto.setName(stage.getName() + " - " + map.getName());
                }

                // 给前端展示用
                dto.setType(BizType.LEARNING_MAP);
            } else {
                // 跳过无效的关联
                continue;
            }

            result.add(dto);
        }

        return result;
    }
}
