package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.dto.toc.learning.LearningTaskDTO;
import com.learn.dto.toc.learning.LearningTasksResponse;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.toc.LearningTaskService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习任务服务实现类
 */
@Service
@Slf4j
public class LearningTaskServiceImpl implements LearningTaskService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 获取用户的学习任务列表
     *
     * @param userId   用户ID
     * @param type     任务类型：train-培训，map-学习地图，默认全部
     * @param status   状态筛选：all-全部，required-必修，elective-选修，默认全部
     * @param pageNum  页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 学习任务列表响应
     */
    @Override
    public LearningTasksResponse getLearningTasks(Long userId, String type, String status, Integer pageNum, Integer pageSize) {
        // 参数处理
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        
        // 获取所有学习任务
        List<LearningTaskDTO> allTasks = new ArrayList<>();
        
        // 根据类型获取学习任务
        if (StringUtils.isEmpty(type) || BizType.TRAIN.equals(type)) {
            allTasks.addAll(getTrainTasks(userId, status));
        }
        
        if (StringUtils.isEmpty(type) || BizType.LEARNING_MAP.equals(type)) {
            allTasks.addAll(getLearningMapTasks(userId, status));
        }
        
        // 按置顶和开始时间排序
        allTasks.sort(Comparator.comparing(LearningTaskDTO::getIsTop).reversed()
                .thenComparing(task -> task.getStartTime() != null ? task.getStartTime() : new java.util.Date(0)));
        
        // 分页处理
        int total = allTasks.size();
        int totalPages = (total + pageSize - 1) / pageSize;
        
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<LearningTaskDTO> pagedTasks = fromIndex < total 
                ? allTasks.subList(fromIndex, toIndex) 
                : new ArrayList<>();
        
        // 构建响应
        return LearningTasksResponse.builder()
                .total(total)
                .list(pagedTasks)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
    
    /**
     * 获取培训任务列表
     *
     * @param userId 用户ID
     * @param status 状态筛选
     * @return 培训任务列表
     */
    private List<LearningTaskDTO> getTrainTasks(Long userId, String status) {
        // 查询用户的培训学习记录
        LambdaQueryWrapper<UserLearningTask> learningTaskQuery = new LambdaQueryWrapper<>();
        learningTaskQuery.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getSource, BizConstants.SOURCE_ASSIGN)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(learningTaskQuery);
        
        if (learningTasks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取培训ID列表
        List<Long> trainIds = learningTasks.stream()
                .map(UserLearningTask::getBizId)
                .collect(Collectors.toList());
        
        // 查询培训信息
        LambdaQueryWrapper<Train> trainQuery = new LambdaQueryWrapper<>();
        trainQuery.in(Train::getId, trainIds)
                .eq(Train::getIsDel, 0);
        List<Train> trains = trainMapper.selectList(trainQuery);

        // 按状态筛选
        if ("required".equals(status)) {
            // 必修培训，通过指派方式判断
            learningTasks = learningTasks.stream()
                    .filter(lt -> "ASSIGN".equals(lt.getSource()))
                    .collect(Collectors.toList());
        } else if ("elective".equals(status)) {
            // 选修培训，通过自学方式判断
            learningTasks = learningTasks.stream()
                    .filter(lt -> "SELF".equals(lt.getSource()))
                    .collect(Collectors.toList());
        }
        
        // 构建培训任务DTO
        Map<Long, UserLearningTask> learningTaskMap = learningTasks.stream()
                .collect(Collectors.toMap(UserLearningTask::getBizId, lt -> lt, (lt1, lt2) -> lt1));
        
        return trains.stream()
                .filter(train -> learningTaskMap.containsKey(train.getId()))
                .map(train -> {
                    UserLearningTask learningTask = learningTaskMap.get(train.getId());
                    
                    // 获取扩展属性
                    Map<String, Object> attributes = Json.fromJson(learningTask.getAttributes(), Map.class);
                    
                    // 获取已完成课程数
                    int completedCourseCount = attributes != null && attributes.containsKey("completedCourseCount") 
                            ? ((Number) attributes.get("completedCourseCount")).intValue() : 0;
                    int completedRequiredCourseCount = attributes != null && attributes.containsKey("completedRequiredCourseCount") 
                            ? ((Number) attributes.get("completedRequiredCourseCount")).intValue() : 0;
                    int completedOptionalCourseCount = attributes != null && attributes.containsKey("completedOptionalCourseCount") 
                            ? ((Number) attributes.get("completedOptionalCourseCount")).intValue() : 0;
                    
                    // 计算任务组成数量
                    int taskCount = completedCourseCount;
                    
                    return LearningTaskDTO.builder()
                            .id(train.getId())
                            .title(train.getName())
                            .type(BizType.TRAIN)
                            .coverImage(train.getCover())
                            .credit(train.getCredit())
                            .isRequired("ASSIGN".equals(learningTask.getSource()) ? 1 : 0)
                            .progress(learningTask.getProgress())
                            .startTime(learningTask.getStartTime())
                            .endTime(learningTask.getDeadline())
                            .isTop(0) // 默认不置顶
                            .taskCount(taskCount) // 添加任务组成数量
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取学习地图任务列表
     *
     * @param userId 用户ID
     * @param status 状态筛选
     * @return 学习地图任务列表
     */
    private List<LearningTaskDTO> getLearningMapTasks(Long userId, String status) {
        // 查询用户的学习地图进度记录
        LambdaQueryWrapper<UserLearningTask> learningTaskQuery = new LambdaQueryWrapper<>();
        learningTaskQuery.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                .eq(UserLearningTask::getSource, BizConstants.SOURCE_ASSIGN)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(learningTaskQuery);
        
        if (learningTasks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取学习地图ID列表
        List<Long> mapIds = learningTasks.stream()
                .map(UserLearningTask::getBizId)
                .collect(Collectors.toList());
        
        // 查询学习地图信息
        LambdaQueryWrapper<LearningMap> mapQuery = new LambdaQueryWrapper<>();
        mapQuery.in(LearningMap::getId, mapIds)
                .eq(LearningMap::getIsDel, 0);
        List<LearningMap> maps = learningMapMapper.selectList(mapQuery);
        
        // 检查用户是否有权限访问这些学习地图
        maps = filterMapsByPermission(maps, userId);
        
        // 按状态筛选
        if ("required".equals(status)) {
            // 必修学习地图，通过指派方式判断
            learningTasks = learningTasks.stream()
                    .filter(lt -> "ASSIGN".equals(lt.getSource()))
                    .collect(Collectors.toList());
        } else if ("elective".equals(status)) {
            // 选修学习地图，通过自学方式判断
            learningTasks = learningTasks.stream()
                    .filter(lt -> "SELF".equals(lt.getSource()))
                    .collect(Collectors.toList());
        }
        
        // 构建学习地图任务DTO
        Map<Long, UserLearningTask> learningTaskMap = learningTasks.stream()
                .collect(Collectors.toMap(UserLearningTask::getBizId, lt -> lt, (lt1, lt2) -> lt1));
        
        return maps.stream()
                .filter(map -> learningTaskMap.containsKey(map.getId()))
                .map(map -> {
                    UserLearningTask learningTask = learningTaskMap.get(map.getId());
                    
                    // 获取扩展属性
                    Map<String, Object> attributes = Json.fromJson(learningTask.getAttributes(), Map.class);
                    
                    // 获取已完成阶段数和任务数
                    int completedStageCount = attributes != null && attributes.containsKey("completedStageCount") 
                            ? ((Number) attributes.get("completedStageCount")).intValue() : 0;
                    int completedRequiredTaskCount = attributes != null && attributes.containsKey("completedRequiredTaskCount") 
                            ? ((Number) attributes.get("completedRequiredTaskCount")).intValue() : 0;
                    int completedOptionalTaskCount = attributes != null && attributes.containsKey("completedOptionalTaskCount") 
                            ? ((Number) attributes.get("completedOptionalTaskCount")).intValue() : 0;
                    
                    // 计算任务总数
                    int taskCount = completedRequiredTaskCount + completedOptionalTaskCount;
                    
                    return LearningTaskDTO.builder()
                            .id(map.getId())
                            .title(map.getName())
                            .type(BizType.LEARNING_MAP)
                            .coverImage(map.getCover())
                            .credit(map.getRequiredCredit().intValue() + map.getElectiveCredit().intValue())
                            .isRequired("ASSIGN".equals(learningTask.getSource()) ? 1 : 0)
                            .progress(learningTask.getProgress())
                            .startTime(learningTask.getStartTime())
                            .endTime(learningTask.getDeadline())
                            .isTop(0) // 默认不置顶
                            .taskCount(taskCount) // 添加任务组成数量
                            .build();
                })
                .collect(Collectors.toList());
    }

    


    /**
     * 根据权限过滤学习地图列表
     *
     * @param maps   学习地图列表
     * @param userId 用户ID
     * @return 过滤后的学习地图列表
     */
    private List<LearningMap> filterMapsByPermission(List<LearningMap> maps, Long userId) {
        return maps.stream()
                .filter(map -> {
                    // 构建权限检查请求
                    CommonRangeQueryRequest request = new CommonRangeQueryRequest();
                    request.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
                    request.setType(BizType.LEARNING_MAP);
                    request.setTypeId(map.getId());
                    request.setUserId(userId.toString());
                    
                    // 检查用户是否有权限访问该学习地图
                    return commonRangeInterface.checkUserHasRight(request);
                })
                .collect(Collectors.toList());
    }
}
