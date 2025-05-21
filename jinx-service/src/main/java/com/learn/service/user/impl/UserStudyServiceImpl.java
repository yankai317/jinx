package com.learn.service.user.impl;

import com.alibaba.mos.boot.event.utils.MosBootEventUtils;
import com.alibaba.mos.boot.kv.ability.MosBootLockAbility;
import com.alibaba.mos.boot.util.MosBootSpiLoader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.user.UserStudyRecordCreateDTO;
import com.learn.event.CourseCompletedEvent;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.user.UserInfoService;
import com.learn.service.user.UserStudyService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY;
import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY_EXPIRE_TIME;

/**
 * 用户学习服务实现类
 *
 * @author yujintao
 * @date 2025/5/6
 */
@Service
@Slf4j
public class UserStudyServiceImpl implements UserStudyService {

    private static final List<String> EXIST_CERTIFICATE_TYPES = Lists.newArrayList(BizType.TRAIN, BizType.MAP_STAGE, BizType.LEARNING_MAP);

    private static final List<String> EXIST_CREDIT_TYPES = Lists.newArrayList(BizType.COURSE, BizType.MAP_STAGE, BizType.LEARNING_MAP, BizType.TRAIN);

    private final MosBootLockAbility lock = MosBootSpiLoader.getSpiService(MosBootLockAbility.class);


    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private TrainMapper trainMapper;

    /**
     * 创建用户学习记录，当用户自学或者被指派课程时，需要创建用户学习记录
     * 1. 业务类型为课程（非系列课），对应表插入记录： user_learning_task。source仅为自学
     * 2. 业务类型为课程且为系列课，对应表插入记录： user_learning_task。source仅为自学
     * 3. 业务类型为培训，对应表插入记录： user_learning_task。培训中的课程也生成记录，parentId为培训id。
     * 4. 业务类型为地图，对应表插入记录： user_learning_task。地图的阶段也生成记录，parentId为地图id。地图阶段内的课程也生成记录，parentId为地图阶段id
     * 5. 如果是培训和地图，支持指派。查询指派记录表assignment_detail来确定是否为指派。
     * 6. 地图类型的扩展属性中需要记录 已完成阶段数，已完成必修任务数，已完成选修任务数，已获得学分；培训类型的扩展属性中需要记录 已完成课程数，已完成必修课程数，已完成选修课程数，已获得学分
     *
     * @param userStudyRecordCreateDTO 用户学习记录创建DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUserStudyRecord(UserStudyRecordCreateDTO userStudyRecordCreateDTO) {
        log.info("创建用户学习记录开始，参数：{}", userStudyRecordCreateDTO);

        // 参数校验
        if (userStudyRecordCreateDTO == null) {
            log.error("创建用户学习记录失败，参数为空");
            throw new IllegalArgumentException("参数不能为空");
        }

        if (userStudyRecordCreateDTO.getUserId() == null) {
            log.error("创建用户学习记录失败，用户ID为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (StringUtils.isBlank(userStudyRecordCreateDTO.getBizType())) {
            log.error("创建用户学习记录失败，业务类型为空");
            throw new IllegalArgumentException("业务类型不能为空");
        }

        if (StringUtils.isBlank(userStudyRecordCreateDTO.getBizId())) {
            log.error("创建用户学习记录失败，业务ID为空");
            throw new IllegalArgumentException("业务ID不能为空");
        }

        // 转换业务ID为Long类型
        Long bizId = Long.parseLong(userStudyRecordCreateDTO.getBizId());

        // 检查是否已存在学习记录，避免重复创建
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userStudyRecordCreateDTO.getUserId())
                .eq(UserLearningTask::getBizType, userStudyRecordCreateDTO.getBizType())
                .eq(UserLearningTask::getBizId, bizId)
                .eq(UserLearningTask::getIsDel, 0);

        UserLearningTask existingTask = userLearningTaskMapper.selectOne(queryWrapper);
        if (existingTask != null) {
            log.info("用户学习记录已存在，不再创建，用户ID：{}，业务类型：{}，业务ID：{}",
                    userStudyRecordCreateDTO.getUserId(),
                    userStudyRecordCreateDTO.getBizType(),
                    bizId);
            return;
        }

        // 创建用户学习记录
        UserLearningTask userLearningTask = new UserLearningTask();
        userLearningTask.setUserId(userStudyRecordCreateDTO.getUserId());
        userLearningTask.setBizType(userStudyRecordCreateDTO.getBizType());
        userLearningTask.setBizId(bizId);

        // 设置学习来源
        String source = userStudyRecordCreateDTO.getSource();
        if (StringUtils.isBlank(source)) {
            // 如果是培训或学习地图，检查是否为指派
            if (BizType.TRAIN.equals(userStudyRecordCreateDTO.getBizType())
                    || BizType.LEARNING_MAP.equals(userStudyRecordCreateDTO.getBizType())) {
                // 查询指派记录
                String bizUniqueId = userStudyRecordCreateDTO.getBizType() + "_" + bizId;
                LambdaQueryWrapper<AssignmentDetail> assignWrapper = new LambdaQueryWrapper<>();
                assignWrapper.eq(AssignmentDetail::getBizId, bizUniqueId)
                        .eq(AssignmentDetail::getUserid, userStudyRecordCreateDTO.getUserId())
                        .eq(AssignmentDetail::getIsDel, 0);

                AssignmentDetail assignmentDetail = assignmentDetailMapper.selectOne(assignWrapper);
                if (assignmentDetail != null) {
                    source = "ASSIGN"; // 指派// 设置指派时间
                    userLearningTask.setAssignTime(assignmentDetail.getGmtCreate());
                    userLearningTask.setDeadline(assignmentDetail.getFinishTime());
                } else {
                    source = "SELF"; // 自学
                }
            } else {
                source = "SELF"; // 默认为自学
            }
        }
        userLearningTask.setSource(source);

        // 设置初始状态
        userLearningTask.setStatus(LearningStatus.NOT_STARTED); // 未开始
        userLearningTask.setStudyDuration(0); // 学习时长初始为0
        userLearningTask.setProgress(0); // 学习进度初始为0
        userLearningTask.setEarnedCredit(0); // 已获得学分初始为0
        userLearningTask.setCertificateIssued(0); // 未发放证书
        // 设置创建时间
        Date now = new Date();
        userLearningTask.setGmtCreate(now);
        userLearningTask.setGmtModified(now);
        userLearningTask.setIsDel(0); // 未删除

        // 根据业务类型设置扩展属性
        setAttributes(userLearningTask);// 设置子节点信息
        userLearningTask.setSearchKey(userLearningTask.buildUserSearchKey());
        userLearningTaskMapper.insert(userLearningTask);

        log.info("创建用户学习记录成功，用户ID：{}，业务类型：{}，业务ID：{}",
                userStudyRecordCreateDTO.getUserId(),
                userStudyRecordCreateDTO.getBizType(),
                bizId);
    }

    @Override
    public UserLearningTask getLearningTask(Long userId, RecordLearningProgressRequest request) {
        // 1. 查询学习任务
        log.info("处理学习任务: userId={}, contentType={}, contentId={}, parentId={}",
                userId, request.getParentType(), request.getContentId(), request.getParentId());
        // 查询或创建培训学习任务
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, request.getContentType())
                .eq(UserLearningTask::getBizId, request.getContentId())
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectOne(queryWrapper);
    }

    /**
     * 设置扩展属性
     *
     * @param userLearningTask 用户学习任务
     */
    private void setAttributes(UserLearningTask userLearningTask) {
        Map<String, Object> attributes = new HashMap<>();

        // 根据业务类型设置不同的扩展属性
        if (BizType.LEARNING_MAP.equals(userLearningTask.getBizType())) {
            // 地图类型的扩展属性
            attributes.put("completedStageCount", 0); // 已完成阶段数
            attributes.put("completedRequiredTaskCount", 0); // 已完成必修任务数
            attributes.put("completedOptionalTaskCount", 0); // 已完成选修任务数
            attributes.put("earnedCredit", 0); // 已获得学分
        } else if (BizType.TRAIN.equals(userLearningTask.getBizType())) {
            // 培训类型的扩展属性
            attributes.put("completedCourseCount", 0); // 已完成课程数
            attributes.put("completedRequiredCourseCount", 0); // 已完成必修课程数
            attributes.put("completedOptionalCourseCount", 0); // 已完成选修课程数
            attributes.put("earnedCredit", 0); // 已获得学分
        }

        userLearningTask.setAttributes(Json.toJson(attributes));
    }

    @Override
    public UserLearningTask getOrCreateLearningTask(Long userId, RecordLearningProgressRequest request) {
        // 1. 查询学习任务
        log.info("处理学习任务: userId={}, contentType={}, contentId={}, parentId={}",
                userId, request.getParentType(), request.getContentId(), request.getParentId());
        // 查询或创建培训学习任务
        UserLearningTask learningTask = getLearningTask(userId, request);
        if (learningTask != null) {
            if (request.isInitByAssign() && BizConstants.SOURCE_SELF.equals(learningTask.getSource())) {
                // 更新为指派任务
                setAssignInfo(userId, request.getContentType(), request.getContentId(), learningTask);
                userLearningTaskMapper.updateById(learningTask);
            }
            return learningTask;
        }

        learningTask = new UserLearningTask();
        learningTask.setUserId(userId);
        learningTask.setBizType(request.getContentType());
        learningTask.setBizId(request.getContentId());
        if (StringUtils.isNotBlank(request.getParentType())) {
            learningTask.setParentType(request.getParentType());
        }
        if (null != request.getParentId() && 0 != request.getParentId() ) {
            learningTask.setParentId(request.getParentId());
        }

        // 设置指派信息
        Date now = new Date();
        if (request.isInitByAssign()) {
            setAssignInfo(userId, request.getContentType(), request.getContentId(), learningTask);
            learningTask.setStatus(LearningStatus.NOT_STARTED);
        } else {
            learningTask.setSource(BizConstants.SOURCE_SELF);
            learningTask.setStatus(LearningStatus.LEARNING); // 学习中
            learningTask.setStartTime(now); // 设置开始学习时间
        }

        learningTask.setStudyDuration(0);
        learningTask.setProgress(0);
        learningTask.setCertificateIssued(0);
        learningTask.setGmtCreate(now);
        learningTask.setGmtModified(now);
        learningTask.setIsDel(0);

        // 设置学分
        setCredit(learningTask);

        // 查询关联项目
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ContentRelation::getBizId, learningTask.getBizId())
                .eq(ContentRelation::getBizType, learningTask.getBizType())
                .eq(ContentRelation::getIsDel, 0);
        Long count = contentRelationMapper.selectCount(relationQueryWrapper);

        // 设置培训类型的扩展属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AttributeKey.completedRequiredCount, 0); // 已完成课程数
        attributes.put(AttributeKey.completedElectiveCount, 0); // 已完成必修课程数
        attributes.put(AttributeKey.allCount, count);
        attributes.put(AttributeKey.currentTaskId, 0);
        learningTask.setAttributes(Json.toJson(attributes));
        learningTask.setSearchKey(learningTask.buildUserSearchKey());
        userLearningTaskMapper.insert(learningTask);
        log.info("创建学习任务成功: userId={}, trainId={}", userId, request.getContentId());
        return learningTask;
    }

    /**
     * 设置学习记录的学分
     * 1. bizType = SERIES 或者 COURSE，需要查询courses获取学分
     * 2. bizType = Learning_map，需要查询学习地图获取学分
     * 3. bizType = MAP_STAGE，需要查询学习地图阶段获取学分
     * 4. bizType = TRAIN，需要查询培训获取学分
     * 5. 其他类型设置为0
     *
     * @param learningTask 学习任务
     */
    private void setCredit(UserLearningTask learningTask) {
        if (learningTask == null || learningTask.getBizId() == null || StringUtils.isBlank(learningTask.getBizType())) {
            return;
        }

        String bizType = learningTask.getBizType();
        Long bizId = learningTask.getBizId();
        Integer credit = 0;

        try {
            if (BizType.COURSE.equals(bizType)) {
                // 查询课程获取学分
                Courses course = coursesMapper.selectById(bizId);
                if (course != null && course.getCredit() != null) {
                    credit = course.getCredit();
                }
            } else if (BizType.LEARNING_MAP.equals(bizType)) {
                // 查询学习地图获取学分
                LearningMap learningMap = learningMapMapper.selectById(bizId);
                if (learningMap != null) {
                    // 学习地图的学分是必修学分和选修学分的总和
                    int requiredCredit = learningMap.getRequiredCredit() != null ? learningMap.getRequiredCredit() : 0;
                    int electiveCredit = learningMap.getElectiveCredit() != null ? learningMap.getElectiveCredit() : 0;
                    credit = (requiredCredit + electiveCredit);
                }
            } else if (BizType.MAP_STAGE.equals(bizType)) {
                // 查询学习地图阶段获取学分
                LearningMapStage stage = learningMapStageMapper.selectById(bizId);
                if (stage != null && stage.getCredit() != null) {
                    credit = stage.getCredit();
                }
            } else if (BizType.TRAIN.equals(bizType)) {
                // 查询培训获取学分
                Train train = trainMapper.selectById(bizId);
                if (train != null && train.getCredit() != null) {
                    credit = train.getCredit();
                }
            }
        } catch (Exception e) {
            log.error("设置学分异常: bizType={}, bizId={}", bizType, bizId, e);
        }

        learningTask.setEarnedCredit(credit);
        log.info("设置学分: bizType={}, bizId={}, credit={}", bizType, bizId, credit);
    }

    /**
     * 更新学习进度
     * 优化点：
     * 1. 减少数据库查询次数，使用批量操作
     * 2. 优化递归逻辑，避免重复处理
     * 3. 提高代码可读性和维护性
     *
     * @param learningTask 学习任务
     * @param request 请求参数
     */
    @Override
    @Transactional
    public void updateLearningProgress(UserLearningTask learningTask, RecordLearningProgressRequest request) {
        // 参数校验
        if (learningTask == null || request == null) {
            log.error("更新学习进度失败，参数为空: learningTask={}, request={}", learningTask, request);
            throw new IllegalArgumentException("参数不能为空");
        }

        // 记录原始状态，用于判断是否完成
        boolean wasCompleted = learningTask.isCompleted();
        
        // 设置学习进度
        Date now = new Date();
        learningTask.setLastStudyTime(now);
        learningTask.setGmtModified(now);
        
        // 计算并设置学习进度
        calculateAndSetProgress(learningTask, request);

        // 更新学习任务
        userLearningTaskMapper.updateById(learningTask);

        // 如果任务刚完成，处理完成后的逻辑
        if (!wasCompleted && learningTask.isCompleted()) {
            // 使用异步处理或批处理优化性能
            CourseCompletedEvent completedEvent = new CourseCompletedEvent();
            completedEvent.setUserId(learningTask.getUserId());
            completedEvent.setContentId(learningTask.getBizId());
            completedEvent.setContentType(learningTask.getBizType());
            String result = MosBootEventUtils.publish(completedEvent);
            log.info("内容完成消息发送消息id: {}", result);
        }

        log.info("更新学习任务进度成功: userId={}, bizType={}, bizId={}, progress={}",
                learningTask.getUserId(), learningTask.getBizType(), learningTask.getBizId(), learningTask.getProgress());
    }

    @Override
    public void processCompletionTasks(Long userId, String bizType, Long bizId) {
        // 查询学习任务
        LambdaQueryWrapper<UserLearningTask> learningTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        learningTaskLambdaQueryWrapper
                .eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizId, bizId)
                .eq(UserLearningTask::getBizType, bizType)
                .eq(UserLearningTask::getIsDel, 0);
        UserLearningTask userLearningTask = userLearningTaskMapper.selectOne(learningTaskLambdaQueryWrapper);
        if (Objects.isNull(userLearningTask)) {
            return;
        }
        processCompletionTasks(userLearningTask);
    }

    /**
     * 处理学习任务完成后的相关任务
     * 
     * @param learningTask 已完成的学习任务
     */
    private void processCompletionTasks(UserLearningTask learningTask) {
        if (!lock.tryLock(LEARNING_RECORD_KEY, learningTask.getBizType() + "_" + learningTask.getBizId(), LEARNING_RECORD_KEY_EXPIRE_TIME)) {
            throw new RuntimeException("正在记录学习进度中～～");
        }
        try {
            // 1. 处理证书
            if (EXIST_CERTIFICATE_TYPES.contains(learningTask.getBizType())) {
                handleCertificate(learningTask);
            }

            // 2. 处理学分
            if (EXIST_CREDIT_TYPES.contains(learningTask.getBizType())) {
                handleCredit(learningTask);
            }

            // 3. 处理父节点进度更新
            updateParentProgress(learningTask);
        } finally {
            lock.unlock(LEARNING_RECORD_KEY, learningTask.getBizType() + "_" + learningTask.getBizId());
        }
    }
    
    /**
     * 更新父节点进度
     * 优化：使用迭代替代递归，避免栈溢出风险
     * 
     * @param learningTask 完成的学习任务
     */
    private void updateParentProgress(UserLearningTask learningTask) {
        // 使用队列存储需要处理的任务，避免递归调用
        Queue<UserLearningTask> taskQueue = new LinkedList<>();
        taskQueue.offer(learningTask);
        
        // 记录已处理的任务ID，避免重复处理
        Set<String> processedTasks = new HashSet<>();
        String taskKey = learningTask.getBizType() + "_" + learningTask.getBizId();
        processedTasks.add(taskKey);
        
        while (!taskQueue.isEmpty()) {
            UserLearningTask currentTask = taskQueue.poll();

            // 查询当前任务的父节点关系
            List<ContentRelation> parentRelations = getParentRelations(currentTask);
            if (CollectionUtils.isEmpty(parentRelations)) {
                continue;
            }
            
            // 批量处理父节点
            for (ContentRelation parentRelation : parentRelations) {
                String parentKey = parentRelation.getBizType() + "_" + parentRelation.getBizId();
                
                // 避免重复处理
                if (processedTasks.contains(parentKey)) {
                    continue;
                }
                processedTasks.add(parentKey);
                
                // 更新父节点进度
                if (!lock.tryLock(LEARNING_RECORD_KEY, learningTask.getBizType() + "_" + learningTask.getBizId(), LEARNING_RECORD_KEY_EXPIRE_TIME)) {
                    throw new RuntimeException("正在记录学习进度中～～");
                }
                try {
                    UserLearningTask parentTask = updateParentTaskProgress(currentTask.getUserId(), parentRelation);

                    // 如果父节点完成，则加入队列继续处理
                    if (parentTask != null && parentTask.isCompleted()) {
                        // 处理证书和学分
                        if (EXIST_CERTIFICATE_TYPES.contains(parentTask.getBizType())) {
                            handleCertificate(parentTask);
                        }

                        if (EXIST_CREDIT_TYPES.contains(parentTask.getBizType())) {
                            handleCredit(parentTask);
                        }

                        // 将父节点加入队列
                        taskQueue.offer(parentTask);
                    }
                } finally {
                    lock.unlock(LEARNING_RECORD_KEY, learningTask.getBizType() + "_" + learningTask.getBizId());
                }
            }
        }
    }
    
    /**
     * 获取学习任务的父节点关系
     * 
     * @param task 学习任务
     * @return 父节点关系列表
     */
    private List<ContentRelation> getParentRelations(UserLearningTask task) {
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ContentRelation::getContentId, task.getBizId())
                .eq(ContentRelation::getContentType, task.getBizType())
                .eq(ContentRelation::getIsDel, 0);
        return contentRelationMapper.selectList(relationQueryWrapper);
    }

    /**
     * 处理学分
     * 1. bizType = SERIES 或者 COURSE，需要查询courses获取学分
     * 2. bizType = Learning_map，需要查询学习地图获取学分
     * 3. bizType = MAP_STAGE，需要查询学习地图阶段获取学分
     * 4. bizType = TRAIN，需要查询培训获取学分
     * 5. 其他类型设置为0
     *
     * @param learningTask 学习任务
     */
    private void handleCredit(UserLearningTask learningTask) {
        if (learningTask == null || learningTask.getBizId() == null || StringUtils.isBlank(learningTask.getBizType())) {
            return;
        }

        // 如果不是支持学分的类型，直接返回
        if (!EXIST_CREDIT_TYPES.contains(learningTask.getBizType())) {
            return;
        }

        String bizType = learningTask.getBizType();
        Long bizId = learningTask.getBizId();
        Integer credit = 0;

        try {
            if (BizType.COURSE.equals(bizType)) {
                // 查询课程获取学分
                Courses course = coursesMapper.selectById(bizId);
                if (course != null && course.getCredit() != null) {
                    credit = course.getCredit();
                }
            } else if (BizType.LEARNING_MAP.equals(bizType)) {
                // 查询学习地图获取学分
                LearningMap learningMap = learningMapMapper.selectById(bizId);
                if (learningMap != null) {
                    // 学习地图的学分是必修学分和选修学分的总和
                    int requiredCredit = learningMap.getRequiredCredit() != null ? learningMap.getRequiredCredit() : 0;
                    int electiveCredit = learningMap.getElectiveCredit() != null ? learningMap.getElectiveCredit() : 0;
                    credit = (requiredCredit + electiveCredit);
                }
            } else if (BizType.MAP_STAGE.equals(bizType)) {
                // 查询学习地图阶段获取学分
                LearningMapStage stage = learningMapStageMapper.selectById(bizId);
                if (stage != null && stage.getCredit() != null) {
                    credit = stage.getCredit();
                }
            } else if (BizType.TRAIN.equals(bizType)) {
                // 查询培训获取学分
                Train train = trainMapper.selectById(bizId);
                if (train != null && train.getCredit() != null) {
                    credit = train.getCredit();
                }
            }
        } catch (Exception e) {
            log.error("处理学分异常: bizType={}, bizId={}", bizType, bizId, e);
        }

        // 更新学习任务的学分
        learningTask.setEarnedCredit(credit);
        log.info("处理学分完成: bizType={}, bizId={}, credit={}", bizType, bizId, credit);
    }

    private void setAssignInfo(Long userId, String bizType, Long bizId, UserLearningTask trainTask) {
        LambdaQueryWrapper<AssignmentDetail> assignWrapper = new LambdaQueryWrapper<>();
        assignWrapper.eq(AssignmentDetail::getType, bizType)
                .eq(AssignmentDetail::getTypeId, bizId)
                .eq(AssignmentDetail::getStatus, 1)
                .eq(AssignmentDetail::getUserid, userId)
                .eq(AssignmentDetail::getIsDel, 0);

        AssignmentDetail assignmentDetail = assignmentDetailMapper.selectOne(assignWrapper);
        trainTask.setAssignTime(assignmentDetail.getGmtCreate());
        trainTask.setSource(BizConstants.SOURCE_ASSIGN);
        trainTask.setDeadline(assignmentDetail.getFinishTime());
    }

    /**
     * 计算并设置学习进度
     * 优化：提取为独立方法，提高代码可读性
     *
     * @param learningTask 学习任务
     * @param request 请求参数
     */
    private void calculateAndSetProgress(UserLearningTask learningTask, RecordLearningProgressRequest request) {
        String bizType = learningTask.getBizType();
        Long bizId = learningTask.getBizId();
        if (learningTask.isCompleted()) {
            return;
        }
        
        // 获取课程类型和总时长
        CourseInfo courseInfo = getCourseInfo(bizType, bizId);
        String courseType = courseInfo.getType();
        int totalDuration = courseInfo.getDuration();
        
        // 计算进度和学习时长
        int progress = request.getProgress();
        int studyDuration = learningTask.getStudyDuration() + Math.max(request.getDuration(), 1);
        
        // 根据课程类型计算进度
        if ("video".equals(courseType)) {
            // 视频类型，根据时长计算进度
            int duration = learningTask.getStudyDuration();
            if (totalDuration > 0) {
                progress = (int) ((double) 100 * duration / totalDuration);
                progress = Math.min(progress, 100); // 确保不超过100%
            }
            studyDuration = Math.max(duration, request.getDuration());
        } else if ("series".equals(courseType) || StringUtils.isBlank(courseType)) {
            // 系列课或其他类型，根据子任务完成情况计算进度
            progress = calculateSeriesProgress(learningTask);
        }
        
        // 设置进度和学习时长
        learningTask.setProgress(progress);
        learningTask.setStudyDuration(studyDuration);
        
        // 更新学习状态
        Date now = new Date();
        if (progress >= 100) {
            // 如果进度达到100%，标记为已完成
            learningTask.setStatus(LearningStatus.COMPLETED);
            learningTask.setCompletionTime(now);
        } else if (LearningStatus.NOT_STARTED.equals(learningTask.getStatus())) {
            // 如果状态为未开始，更新为学习中
            learningTask.setStatus(LearningStatus.LEARNING);
            learningTask.setStartTime(now);
        }
    }
    
    /**
     * 获取课程信息（类型和时长）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 课程信息
     */
    private CourseInfo getCourseInfo(String bizType, Long bizId) {
        CourseInfo info = new CourseInfo();
        
        if (BizType.APPENDIX_FILE.equals(bizType)) {
            // 查询附件关联信息
            LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ContentRelation::getContentId, bizId)
                    .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                    .eq(ContentRelation::getIsDel, 0);
            ContentRelation relation = contentRelationMapper.selectOne(queryWrapper);
            
            if (relation != null) {
                info.setType(relation.getAttribute(AttributeKey.BIZ_TYPE, String.class));
                Integer duration = Objects.isNull(relation.getAttribute(AttributeKey.DURATION, Integer.class)) ? 0 : relation.getAttribute(AttributeKey.DURATION, Integer.class);
                info.setDuration(duration);
            }
        } else if (BizType.COURSE.equals(bizType)) {
            // 查询课程信息
            Courses course = coursesMapper.selectById(bizId);
            if (course != null) {
                info.setType(course.getType());
                Integer duration = Objects.isNull(course.getAttribute(AttributeKey.DURATION, Integer.class)) ? 0 : course.getAttribute(AttributeKey.DURATION, Integer.class);
                info.setDuration(duration);
            }
        }
        
        return info;
    }
    
    /**
     * 计算系列课程的进度
     * 
     * @param learningTask 学习任务
     * @return 计算后的进度
     */
    private int calculateSeriesProgress(UserLearningTask learningTask) {
        // 查询关联的子任务
        LambdaQueryWrapper<ContentRelation> contentRelationQueryWrapper = new LambdaQueryWrapper<>();
        contentRelationQueryWrapper.eq(ContentRelation::getBizType, learningTask.getBizType())
                .eq(ContentRelation::getBizId, learningTask.getBizId())
                .in(ContentRelation::getContentType, Lists.newArrayList(BizType.MAP_STAGE, BizType.TRAIN, BizType.COURSE, BizType.APPENDIX_FILE))
                .eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(contentRelationQueryWrapper);
        
        if (CollectionUtils.isEmpty(contentRelations)) {
            return 0;
        }
        
        // 构建搜索键
        List<String> searchKeys = contentRelations.stream()
                .map(v -> v.getContentSearchKey() + "_" + learningTask.getUserId())
                .collect(Collectors.toList());
        
        // 批量查询子任务的学习记录
        LambdaQueryWrapper<UserLearningTask> userLearningTaskQueryWrapper = new LambdaQueryWrapper<>();
        userLearningTaskQueryWrapper.in(UserLearningTask::getSearchKey, searchKeys)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> userLearningTasks = userLearningTaskMapper.selectList(userLearningTaskQueryWrapper);
        
        // 计算进度
        if (CollectionUtils.isEmpty(userLearningTasks)) {
            return 0;
        }
        
        long completedCount = userLearningTasks.stream().filter(UserLearningTask::isCompleted).count();
        return (int)(completedCount * 100 / contentRelations.size());
    }
    
    /**
     * 课程信息类，用于封装课程类型和时长
     */
    private static class CourseInfo {
        private String type;
        private int duration;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getDuration() {
            return duration;
        }
        
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    /**
     * 处理证书
     *
     * @param learningTask
     */
    private void handleCertificate(UserLearningTask learningTask) {
        if (learningTask == null || learningTask.getProgress() < 100) {
            return;
        }
        if (!EXIST_CERTIFICATE_TYPES.contains(learningTask.getBizType())) {
            return;
        }

        log.info("处理证书发放逻辑: userId={}, bizType={}, bizId={}",
                learningTask.getUserId(), learningTask.getBizType(), learningTask.getBizId());

        String bizType = learningTask.getBizType();
        Long bizId = learningTask.getBizId();

        // 根据类型查询对应的内容
        Long certificateId = null;
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizType, bizType)
                .eq(ContentRelation::getBizId, bizId)
                .eq(ContentRelation::getContentType, BizType.CERTIFICATE)
                .eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> certificateRelations = contentRelationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(certificateRelations)) {
            return;
        }
        certificateId = certificateRelations.getFirst().getContentId();
        Certificate certificate = certificateMapper.selectById(certificateId);
        if (certificate == null) {
            log.warn("证书不存在，证书id:{}", certificateId);
            return;
        }

        // 检查是否已经颁发过证书
        LambdaQueryWrapper<UserCertificate> userCertQueryWrapper = new LambdaQueryWrapper<>();
        userCertQueryWrapper.eq(UserCertificate::getUserId, learningTask.getUserId())
                .eq(UserCertificate::getCertificateId, certificateId)
                .eq(UserCertificate::getIsDel, 0);
        UserCertificate existingCertificate = userCertificateMapper.selectOne(userCertQueryWrapper);
        if (Objects.nonNull(existingCertificate)) {
            log.info("用户已存在证书:证书id{}", certificateId);
            return;
        }
        // 设置证书id
        learningTask.setCertificateIssued(1);
        learningTask.setCertificateId(certificateId);
        // 创建用户证书记录
        UserCertificate userCertificate = new UserCertificate();
        userCertificate.setUserId(learningTask.getUserId());
        userCertificate.setCertificateId(certificateId);
        userCertificate.setSourceType(bizType);
        userCertificate.setSourceId(bizId);
        if (BizType.MAP_STAGE.equals(bizType)) {
            LearningMapStage learningMapStage = learningMapStageMapper.selectById(bizId);
            if (Objects.nonNull(learningMapStage)) {
                userCertificate.putAttribute(AttributeKey.MAP_ID, learningMapStage.getMapId());
            }
        }

        Date now = new Date();
        userCertificate.setIssueTime(now);
        userCertificate.setExpireTime(certificate.getExpireTime()); // 使用证书模板的过期时间
        // 生成证书编号
        String certificateNo = generateCertificateNo(learningTask.getUserId(), certificateId);
        userCertificate.setCertificateNo(certificateNo);
        userCertificate.setStatus(0); // 有效
        userCertificate.setGmtCreate(now);
        userCertificate.setGmtModified(now);
        userCertificate.setIsDel(0);
        // 设置创建人和更新人信息
        userCertificate.setCreatorId(learningTask.getUserId());
        userCertificate.setUpdaterId(learningTask.getUserId());
        // 获取用户信息
        try {
            com.learn.dto.user.UserInfoResponse userInfo = userInfoService.getUserInfo(learningTask.getUserId());
            if (userInfo != null) {
                userCertificate.setCreatorName(userInfo.getNickname());
                userCertificate.setUpdaterName(userInfo.getNickname());
            }
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}", learningTask.getUserId(), e);
        }
        // 保存用户证书记录
        userCertificateMapper.insert(userCertificate);
        // 更新证书记录
        userLearningTaskMapper.updateById(learningTask);
    }


    /**
     * 生成证书编号
     *
     * @param userId 用户ID
     * @param certificateId 证书ID
     * @return 证书编号
     */
    private String generateCertificateNo(Long userId, Long certificateId) {
        // 生成证书编号：前缀 + 时间戳 + 用户ID后4位 + 随机数
        String prefix = "CERT";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(0, 10);
        String userIdStr = String.format("%04d", userId % 10000);
        String randomStr = java.util.UUID.randomUUID().toString().substring(0, 4);

        return prefix + timestamp + userIdStr + randomStr;
    }

    /**
     * 更新父任务进度
     * 
     * @param userId 用户ID
     * @param parentRelation 父节点关系
     * @return 更新后的父任务
     */
    private UserLearningTask updateParentTaskProgress(Long userId, ContentRelation parentRelation) {
        // 查询父节点关联的所有子任务
        List<ContentRelation> contentRelations = getChildRelations(parentRelation);
        if (CollectionUtils.isEmpty(contentRelations)) {
            return null;
        }
        
        // 批量查询学习记录
        Map<String, UserLearningTask> learningRecordMap = getChildLearningTasks(userId, contentRelations);
        
        // 计算进度数据
        ProgressStats stats = calculateProgressStats(contentRelations, learningRecordMap);
        
        // 查询父节点任务
        UserLearningTask parentTask = getUserLearningTask(userId, parentRelation.getBizType(), parentRelation.getBizId());
        if (parentTask == null) {
            return null;
        }
        
        // 更新父节点任务进度
        updateTaskWithStats(parentTask, stats, contentRelations);
        
        return parentTask;
    }
    
    /**
     * 获取子任务关系
     * 
     * @param parentRelation 父节点关系
     * @return 子任务关系列表
     */
    private List<ContentRelation> getChildRelations(ContentRelation parentRelation) {
        LambdaQueryWrapper<ContentRelation> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ContentRelation::getBizType, parentRelation.getBizType())
                .eq(ContentRelation::getBizId, parentRelation.getBizId())
                .in(ContentRelation::getContentType, Lists.newArrayList(BizType.COURSE, BizType.APPENDIX_FILE, BizType.MAP_STAGE))
                .eq(ContentRelation::getIsDel, 0)
                .orderByAsc(ContentRelation::getSortOrder);
        return contentRelationMapper.selectList(relationQueryWrapper);
    }
    
    /**
     * 批量获取子任务的学习记录
     * 
     * @param userId 用户ID
     * @param contentRelations 内容关系列表
     * @return 学习记录映射
     */
    private Map<String, UserLearningTask> getChildLearningTasks(Long userId, List<ContentRelation> contentRelations) {
        List<String> recordSearchKeys = contentRelations.stream()
                .map(x -> x.getContentType() + "_" + x.getContentId() + "_" + userId)
                .collect(Collectors.toList());
                
        if (CollectionUtils.isEmpty(recordSearchKeys)) {
            return Collections.emptyMap();
        }
        
        LambdaQueryWrapper<UserLearningTask> learningRecordQueryCondition = new LambdaQueryWrapper<>();
        learningRecordQueryCondition.eq(UserLearningTask::getUserId, userId)
                .in(UserLearningTask::getSearchKey, recordSearchKeys)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> learningRecords = userLearningTaskMapper.selectList(learningRecordQueryCondition);
        
        return learningRecords.stream()
                .collect(Collectors.toMap(UserLearningTask::buildContentSearchKey, v -> v, (v1, v2) -> v1));
    }
    
    /**
     * 计算进度统计数据
     * 
     * @param contentRelations 内容关系列表
     * @param learningRecordMap 学习记录映射
     * @return 进度统计数据
     */
    private ProgressStats calculateProgressStats(List<ContentRelation> contentRelations, Map<String, UserLearningTask> learningRecordMap) {
        ProgressStats stats = new ProgressStats();
        
        for (ContentRelation relation : contentRelations) {
            // 统计必修课总数
            if (1 == relation.getIsRequired()) {
                stats.totalRequired++;
            }
            
            UserLearningTask learningRecord = learningRecordMap.get(relation.getContentSearchKey());
            if (learningRecord == null) {
                continue;
            }
            
            // 累计学习时长
            stats.totalDuration += (learningRecord.getStudyDuration() != null ? learningRecord.getStudyDuration() : 0);
            
            // 统计完成数量
            if (learningRecord.isCompleted()) {
                if (1 == relation.getIsRequired()) {
                    stats.completedRequired++;
                } else {
                    stats.completedElective++;
                }
            }
            
            // 记录当前学习任务
            if (stats.currentTaskId == null && LearningStatus.LEARNING.equals(learningRecord.getStatus())) {
                stats.currentTaskId = learningRecord.getBizId();
            }
        }
        
        // 如果没有找到当前任务，使用最后一个任务
        if (stats.currentTaskId == null && !contentRelations.isEmpty()) {
            stats.currentTaskId = contentRelations.get(contentRelations.size() - 1).getContentId();
        }
        
        return stats;
    }
    
    /**
     * 获取用户学习任务
     * 
     * @param userId 用户ID
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 学习任务
     */
    private UserLearningTask getUserLearningTask(Long userId, String bizType, Long bizId) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, bizType)
                .eq(UserLearningTask::getBizId, bizId)
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectOne(queryWrapper);
    }
    
    /**
     * 使用统计数据更新任务
     * 
     * @param task 任务
     * @param stats 统计数据
     * @param contentRelations 内容关系列表
     */
    private void updateTaskWithStats(UserLearningTask task, ProgressStats stats, List<ContentRelation> contentRelations) {
        // 避免除零错误
        long effectiveTotalRequired = Math.max(1, stats.totalRequired);
        
        // 计算进度
        int progress = (int) (((double)stats.completedRequired / effectiveTotalRequired) * 100);
        progress = Math.min(progress, 100); // 确保不超过100%
        
        Date now = new Date();
        // 更新完成状态
        if (!task.isCompleted() && progress >= 100) {
            task.setCompletionTime(now);
            task.setProgress(100);
        } else {
            task.setLastStudyTime(now);
            task.setProgress(progress);
        }
        
        // 更新学习时长和状态
        task.setStudyDuration(stats.totalDuration);
        task.setStatus(task.convertStatus(progress));
        
        // 更新扩展属性
        task.putAttribute(AttributeKey.currentTaskId, stats.currentTaskId);
        task.putAttribute(AttributeKey.completedElectiveCount, stats.completedElective);
        task.putAttribute(AttributeKey.completedRequiredCount, stats.completedRequired);
        task.putAttribute(AttributeKey.allCount, contentRelations.size());
        
        // 更新到数据库
        userLearningTaskMapper.updateById(task);
    }
    
    /**
     * 进度统计数据类
     */
    private static class ProgressStats {
        int totalDuration = 0;
        long totalRequired = 0;
        long completedElective = 0;
        long completedRequired = 0;
        Long currentTaskId = null;
    }
}
