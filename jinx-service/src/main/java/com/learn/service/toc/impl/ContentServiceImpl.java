package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.learn.common.dto.ContentBO;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.service.toc.ContentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author yujintao
 * @date 2025/5/14
 */
@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Override
    public Map<String, ContentBO> getContentMap(List<Pair<String, Long>> contentSearchKeys) {
        if (contentSearchKeys == null || contentSearchKeys.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, ContentBO> results = new HashMap<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 查询课程
            CompletableFuture<Void> courseTotalFuture = CompletableFuture.runAsync(() -> putCourseContent(results, contentSearchKeys), executor);

            // 查询培训
            CompletableFuture<Void> trainTotalFuture = CompletableFuture.runAsync(() -> putTrainContent(results, contentSearchKeys), executor);

            // 查询地图阶段
            CompletableFuture<Void> mapStageTotalFuture = CompletableFuture.runAsync(() -> putMapStageContent(results, contentSearchKeys), executor);

            // 查询地图
            CompletableFuture<Void> mapTotalFuture = CompletableFuture.runAsync(() -> putMapContent(results, contentSearchKeys), executor);

            // 等待所有任务完成并获取结果
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    courseTotalFuture, trainTotalFuture, mapStageTotalFuture, mapTotalFuture);

            // 阻塞等待所有任务完成
            allFutures.join();

            //汇总
            return results;
        } catch (Exception e) {
            throw new RuntimeException("查询关联内容信息失败");
        }
    }

    @Override
    public ContentBO getContent(String contentType, Long contentId) {
        if (BizType.COURSE.equals(contentType)) {
            List<Courses> coursesList = getCourses(Lists.newArrayList(contentId));
            if (CollectionUtils.isNotEmpty(coursesList)) {
                return ContentBO.builder()
                        .id(coursesList.getFirst().getId())
                        .title(coursesList.getFirst().getTitle())
                        .coverImg(coursesList.getFirst().getCoverImage())
                        .description(coursesList.getFirst().getDescription())
                        .credit(coursesList.getFirst().getCredit())
                        .contentType(BizType.COURSE)
                        .type(coursesList.getFirst().getType())
                        .build();
            }
        } else if (BizType.TRAIN.equals(contentType)) {
            List<Train> trains = getTrain(Lists.newArrayList(contentId));
            if (CollectionUtils.isNotEmpty(trains)) {
                return ContentBO.builder()
                        .id(trains.getFirst().getId())
                        .title(trains.getFirst().getName())
                        .coverImg(trains.getFirst().getCover())
                        .description(trains.getFirst().getIntroduction())
                        .credit(trains.getFirst().getCredit())
                        .contentType(BizType.TRAIN)
                        .build();
            }
        } else if (BizType.LEARNING_MAP.equals(contentType)) {
            List<LearningMap> maps = getMaps(Lists.newArrayList(contentId));
            if (CollectionUtils.isNotEmpty(maps)) {
                return ContentBO.builder()
                        .id(maps.getFirst().getId())
                        .title(maps.getFirst().getName())
                        .coverImg(maps.getFirst().getCover())
                        .description(maps.getFirst().getIntroduction())
                        .credit(maps.getFirst().getRequiredCredit())
                        .contentType(BizType.LEARNING_MAP)
                        .build();
            }
        }
        return null;
    }

    private void putCourseContent(Map<String, ContentBO> results, List<Pair<String, Long>> contentSearchKeys) {
        List<Long> courseIds = contentSearchKeys.stream()
                .filter(v -> BizType.COURSE.equals(v.getLeft()))
                .map(Pair::getRight)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(courseIds)) {
            return;
        }
        // 查询课程
        List<Courses> courses = getCourses(courseIds);
        for (Courses course : courses) {
            results.put(BizType.COURSE + "_" + course.getId().toString(), new ContentBO(course.getId(), course.getTitle(), course.getCoverImage(), course.getDescription(), course.getCredit(), BizType.COURSE, course.getType()));
        }
    }

    private void putTrainContent(Map<String, ContentBO> results, List<Pair<String, Long>> contentSearchKeys) {
        List<Long> trainIds = contentSearchKeys.stream()
                .filter(v -> BizType.TRAIN.equals(v.getLeft()))
                .map(Pair::getRight)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(trainIds)) {
            return;
        }
        // 查询培训
        List<Train> trains = getTrain(trainIds);
        if (CollectionUtils.isEmpty(trains)) {
            return;
        }
        for (Train train : trains) {
            results.put(BizType.TRAIN + "_" + train.getId().toString(), new ContentBO(train.getId(), train.getName(), train.getCover(), train.getIntroduction(), train.getCredit(), BizType.TRAIN, null));
        }
    }

    private void putMapContent(Map<String, ContentBO> results, List<Pair<String, Long>> contentSearchKeys) {
        List<Long> mapIds = contentSearchKeys.stream()
                .filter(v -> BizType.LEARNING_MAP.equals(v.getLeft()))
                .map(Pair::getRight)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mapIds)) {
            return;
        }
        // 查询培训
        List<LearningMap> maps = getMaps(mapIds);
        if (CollectionUtils.isEmpty(maps)) {
            return;
        }
        for (LearningMap map : maps) {
            results.put(BizType.LEARNING_MAP + "_" + map.getId().toString(), new ContentBO(map.getId(), map.getName(), map.getCover(), map.getIntroduction(), map.getRequiredCredit(), BizType.LEARNING_MAP, null));
        }
    }

    private void putMapStageContent(Map<String, ContentBO> results, List<Pair<String, Long>> contentSearchKeys) {
        List<Long> mapStageIds = contentSearchKeys.stream()
                .filter(v -> BizType.MAP_STAGE.equals(v.getLeft()))
                .map(Pair::getRight)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mapStageIds)) {
            return;
        }
        // 查询培训
        List<LearningMapStage> mapStages = getMapStage(mapStageIds);
        if (CollectionUtils.isEmpty(mapStages)) {
            return;
        }
        for (LearningMapStage mapStage : mapStages) {
            results.put(BizType.MAP_STAGE + "_" + mapStage.getId().toString(), new ContentBO(mapStage.getId(), mapStage.getName(), null, null, mapStage.getCredit(), BizType.MAP_STAGE, null));
        }
    }


    private List<Courses> getCourses(List<Long> courseIds) {
        if(CollectionUtils.isEmpty(courseIds)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Courses::getId, courseIds);
        queryWrapper.eq(Courses::getIsDel, 0);
        return coursesMapper.selectList(queryWrapper);
    }

    private List<Train> getTrain(List<Long> trainIds) {
        if(CollectionUtils.isEmpty(trainIds)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Train> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Train::getId, trainIds);
        queryWrapper.eq(Train::getIsDel, 0);
        return trainMapper.selectList(queryWrapper);
    }

    private List<LearningMap> getMaps(List<Long> mapIds) {
        if(CollectionUtils.isEmpty(mapIds)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<LearningMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(LearningMap::getId, mapIds);
        queryWrapper.eq(LearningMap::getIsDel, 0);
        return learningMapMapper.selectList(queryWrapper);
    }

    private List<LearningMapStage> getMapStage(List<Long> mapStageIds) {
        if(CollectionUtils.isEmpty(mapStageIds)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(LearningMapStage::getId, mapStageIds);
        queryWrapper.eq(LearningMapStage::getIsDel, 0);
        return learningMapStageMapper.selectList(queryWrapper);
    }

}
