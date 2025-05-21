package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.learn.common.dto.ContentBO;
import com.learn.common.enums.LearningTaskStatusEnums;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.dto.toc.home.HomeUserCardInfoDTO;
import com.learn.dto.toc.personal.*;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.toc.ContentService;
import com.learn.service.toc.PersonalCenterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 个人中心服务实现类
 */
@Service
@Slf4j
public class PersonalCenterServiceImpl implements PersonalCenterService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    @Autowired
    private CoursesMapper coursesMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private CertificateMapper certificateMapper;@Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DepartmentUserMapper departmentUserMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ContentService contentService;

    @Override
    public UserLearningRecordsResponse getUserLearningRecords(Long userId, UserLearningRecordsRequest request) {
        UserLearningRecordsResponse response = new UserLearningRecordsResponse();
        // 设置分页信息
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        // 构建查询条件
        LambdaQueryWrapper<UserLearningTask> wrapper = buildQueryCondition(userId, request);

        // 分页查询
        Page<UserLearningTask> page = new Page<>(pageNum, pageSize);
        Page<UserLearningTask> resultPage = userLearningTaskMapper.selectPage(page, wrapper);// 转换为DTO
        response.setTotal(resultPage.getTotal());
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPages((int) Math.ceil((double) resultPage.getTotal() / pageSize));
        if (CollectionUtils.isEmpty(resultPage.getRecords())) {
            response.setList(new ArrayList<>());
            return response;
        }

        // 查询内容信息
        List<Pair<String, Long>> contentSearchKeys = resultPage.getRecords().stream()
                .map(v -> Pair.of(v.getBizType(), v.getBizId()))
                .collect(Collectors.toList());
        Map<String, ContentBO> contentMap = contentService.getContentMap(contentSearchKeys);

        List<UserLearningRecordsResponse.LearningRecordDTO> recordList = new ArrayList<>();
        for (UserLearningTask task : resultPage.getRecords()) {
            UserLearningRecordsResponse.LearningRecordDTO dto = new UserLearningRecordsResponse.LearningRecordDTO();
            dto.setId(task.getId());
            dto.setSource(task.getSource());
            dto.setContentId(task.getBizId());
            dto.setType(task.getBizType());
            dto.setStudyDuration(task.getStudyDuration());
            dto.setProgress(task.getProgress());
            // 设置状态
            dto.setStatus(task.getStatus());
            // 设置时间
            if (task.getStartTime() != null) {
                dto.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getStartTime()));
            }
            if (task.getLastStudyTime() != null) {
                dto.setLastStudyTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getLastStudyTime()));
            }
            if (task.getCompletionTime() != null) {
                dto.setCompletionTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getCompletionTime()));
            }
            ContentBO contentBO = contentMap.get(task.getBizType() + "_" + task.getBizId());
            if (Objects.nonNull(contentBO)) {
                dto.setContentName(contentBO.getTitle());
                dto.setCoverUrl(contentBO.getCoverImg());
                dto.setDescription(contentBO.getDescription());
                dto.setCredit(contentBO.getCredit());
            }
            
            recordList.add(dto);
        }
        
        // 设置响应
        response.setList(recordList);

        return response;
    }


    /**
     * 添加查询条件
     * @param userId
     * @param request
     * @return
     */
    private LambdaQueryWrapper<UserLearningTask> buildQueryCondition(Long userId, UserLearningRecordsRequest request) {
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
                .in(UserLearningTask::getBizType, Lists.newArrayList(BizType.TRAIN, BizType.LEARNING_MAP))
                .eq(UserLearningTask::getIsDel, 0);

        // 添加类型过滤条件
        String type = request.getType();
        if (StringUtils.hasText(type) && !"all".equals(type)) {
            wrapper.eq(UserLearningTask::getBizType, type);
        }
        if (StringUtils.hasText(request.getSource())) {
            wrapper.eq(UserLearningTask::getSource, request.getSource());
        }
        // 按最后学习时间降序排序
        wrapper.orderByDesc(UserLearningTask::getLastStudyTime);
        return wrapper;
    }

    /**
     * 获取用户基本信息
     *
     * @param userId 用户ID
     * @return 用户信息DTO
     */
    public HomeUserCardInfoDTO getUserCardInfo(Long userId) {
        HomeUserCardInfoDTO userInfoDTO = new HomeUserCardInfoDTO();

        // 查询用户基本信息
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUserId, userId)
                        .eq(User::getIsDel, 0)
        );

        if (user == null) {
            log.error("用户不存在，userId: {}", userId);
            return userInfoDTO;
        }

        userInfoDTO.setUserId(user.getUserId());
        userInfoDTO.setNickname(user.getNickname());
        userInfoDTO.setAvatar(user.getAvatar());
        userInfoDTO.setEmployeeNo(user.getEmployeeNo());


        // 查询用户所有学习记录的学习时长
        List<UserLearningTask> userLearningTasks = userLearningTaskMapper.selectList(
                new LambdaQueryWrapper<UserLearningTask>()
                        .eq(UserLearningTask::getUserId, userId)
                        .eq(UserLearningTask::getIsDel, 0)
        );

        // 累加所有学习记录的学习时长
        // 计算总学习时长 - 使用新表 user_learning_task
        int totalStudyDuration = 0;
        int acquireCertificateCount = 0;
        long totalLearningScore = 0;
        int totalFinishedTrainCount = 0;
        int totalFinishedLearningMapCount = 0;

        for (UserLearningTask task : userLearningTasks) {
            if (BizType.COURSE.equalsIgnoreCase(task.getBizType())
                    || BizType.APPENDIX_FILE.equalsIgnoreCase(task.getBizType())) {
                totalStudyDuration += task.getStudyDuration() != null ? task.getStudyDuration() : 0;
            }

            if (!LearningTaskStatusEnums.COMPLETED.getCode().equals(task.getStatus())) {
                continue;
            }
            //下面需要完成的才能统计
            acquireCertificateCount += task.getCertificateId() != null ? 1 : 0;
            totalLearningScore += task.getEarnedCredit() != null ? task.getEarnedCredit() : 0;
            if (Objects.equals(task.getBizType(), BizType.TRAIN)) {
                totalFinishedTrainCount++;
            } else if (Objects.equals(task.getBizType(), BizType.LEARNING_MAP)) {
                totalFinishedLearningMapCount++;
            }
        }

        double totalStudyDurationInMinutes = totalStudyDuration / 60.0;
        userInfoDTO.setTotalStudyDuration((int) Math.ceil(totalStudyDurationInMinutes));
        userInfoDTO.setAcquireCertificateCount(acquireCertificateCount);
        userInfoDTO.setTotalLearningScore(totalLearningScore);
        userInfoDTO.setTotalFinishedTrainCount(totalFinishedTrainCount);
        userInfoDTO.setTotalFinishedLearningMapCount(totalFinishedLearningMapCount);

        return userInfoDTO;
    }
    /**
     * 获取用户个人中心信息
     *
     * @param userId 用户ID
     * @return 用户个人中心信息响应
     */
    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        // 1. 查询用户基本信息
        User user = getUserById(userId);
        if (user == null) {
            log.error("用户不存在, userId: {}", userId);
            return UserProfileResponse.error("用户不存在");
        }

        // 2. 查询用户部门信息
        String departmentName = getUserDepartmentName(userId);

        // 3. 构建用户等级 (假设根据总学分计算等级，每100学分提升一级，从1级开始)
        // 4. 构建用户基本信息
        UserProfileResponse.UserInfo userInfo = UserProfileResponse.UserInfo.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .departmentName(departmentName)
                .build();

        // 5. 查询用户学习统计数据
        // 5.1 查询总学习时长
        // 5.2 查询已完成课程数量
        // 5.3 查询证书数量
        HomeUserCardInfoDTO userCardInfo = this.getUserCardInfo(userId);

        // 5.4 构建学习统计数据
        UserProfileResponse.Statistics statistics = UserProfileResponse.Statistics.builder()
                .totalCredit(userCardInfo.getTotalLearningScore())
                .totalStudyTime(userCardInfo.getTotalStudyDuration())
                .completedTrainCount(userCardInfo.getTotalFinishedTrainCount())
                .completedMapCount(userCardInfo.getTotalFinishedLearningMapCount())
                .certificateCount(userCardInfo.getAcquireCertificateCount())
                .build();

        // 6. 查询用户最近获得的证书（最多3个）
        List<UserProfileResponse.Certificate> certificates = getRecentCertificates(userId, 3);

        // 7. 查询用户最近学习记录（最多5条）
        List<UserProfileResponse.LearningRecord> recentLearning = getRecentLearningRecords(userId, 5);

        // 8. 构建用户个人中心数据
        UserProfileResponse.UserProfileData data = UserProfileResponse.UserProfileData.builder()
                .userInfo(userInfo)
                .statistics(statistics)
                .certificates(certificates)
                .recentLearning(recentLearning)
                .build();

        return UserProfileResponse.success(data);
    }

    /**
     * 获取用户学习记录总数
     * 使用一个SQL查询同时获取课程、地图和培训的总数
     *
     * @param userId 用户ID
     * @return 用户学习记录总数响应
     */
    @Override
    public UserLearningTotalResponse getUserLearningTotal(Long userId, boolean countSelective) {
        UserLearningTotalResponse response = new UserLearningTotalResponse();
        
        try {
            // 使用一个SQL查询同时获取所有类型的学习记录数量
            Map<String, Object> countResult = userLearningTaskMapper.countUserLearningByTypes(userId, countSelective);
            
            // 从结果中获取各类型的数量
            Long mapTotal = ((Number) countResult.getOrDefault("mapTotal", 0)).longValue();
            Long trainTotal = ((Number) countResult.getOrDefault("trainTotal", 0)).longValue();

            // 设置响应
            response.setMapTotal(mapTotal);
            response.setTrainTotal(trainTotal);
            response.setAllTotal(mapTotal + trainTotal);
            
        } catch (Exception e) {
            log.error("获取用户学习记录总数失败", e);
        }
        
        return response;
    }


    /**
     * 获取学习时长趋势（最近7天）
     */
    private Map<String, Integer> getStudyDurationTrend(Long userId) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        
        // 获取最近7天的日期
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // 初始化趋势图数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.put(date.format(formatter), 0);
        }
        
        // 获取最近7天的学习记录
        Date startDate = Date.from(today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
               .ge(UserLearningTask::getLastStudyTime, startDate)
               .lt(UserLearningTask::getLastStudyTime, endDate)
               .eq(UserLearningTask::getIsDel, 0)
               .select(UserLearningTask::getLastStudyTime, UserLearningTask::getStudyDuration);
        
        List<UserLearningTask> tasks = userLearningTaskMapper.selectList(wrapper);
        
        // 统计每天的学习时长
        for (UserLearningTask task : tasks) {
            if (task.getLastStudyTime() != null) {
                LocalDate recordDate = task.getLastStudyTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                String dateKey = recordDate.format(formatter);
                if (trend.containsKey(dateKey) && task.getStudyDuration() != null) {
                    trend.put(dateKey, trend.get(dateKey) + task.getStudyDuration());
                }
            }
        }
        
        return trend;
    }
    
    /**
     * 获取学习内容分布
     */
    private List<UserLearningStatisticsResponse.ContentDistribution> getContentDistribution(Long userId) {
        List<UserLearningStatisticsResponse.ContentDistribution> distribution = new ArrayList<>();
        
        // 获取课程学习记录数量
        LambdaQueryWrapper<UserLearningTask> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(UserLearningTask::getUserId, userId).eq(UserLearningTask::getBizType, BizType.COURSE)
                    .eq(UserLearningTask::getIsDel, 0);
        Long courseCount = userLearningTaskMapper.selectCount(courseWrapper);
        
        // 获取培训学习记录数量
        LambdaQueryWrapper<UserLearningTask> trainWrapper = new LambdaQueryWrapper<>();
        trainWrapper.eq(UserLearningTask::getUserId, userId)
                   .eq(UserLearningTask::getBizType, BizType.TRAIN)
                   .eq(UserLearningTask::getIsDel, 0);
        Long trainCount = userLearningTaskMapper.selectCount(trainWrapper);
        
        // 获取学习地图记录数量
        LambdaQueryWrapper<UserLearningTask> mapWrapper = new LambdaQueryWrapper<>();
        mapWrapper.eq(UserLearningTask::getUserId, userId)
                 .eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                 .eq(UserLearningTask::getIsDel, 0);
        Long mapCount = userLearningTaskMapper.selectCount(mapWrapper);
        
        // 计算总数
        Long total = courseCount + trainCount + mapCount;
        
        // 添加课程分布
        if (total > 0) {
            UserLearningStatisticsResponse.ContentDistribution courseDistribution = new UserLearningStatisticsResponse.ContentDistribution();
            courseDistribution.setType(BizType.COURSE);
            courseDistribution.setTypeName("课程");
            courseDistribution.setCount(courseCount);
            courseDistribution.setPercentage(Math.round((double) courseCount / total * 100) / 100.0);
            distribution.add(courseDistribution);
            
            // 添加培训分布
            UserLearningStatisticsResponse.ContentDistribution trainDistribution = new UserLearningStatisticsResponse.ContentDistribution();
            trainDistribution.setType(BizType.TRAIN);
            trainDistribution.setTypeName("培训");
            trainDistribution.setCount(trainCount);
            trainDistribution.setPercentage(Math.round((double) trainCount / total * 100) / 100.0);
            distribution.add(trainDistribution);
            
            // 添加学习地图分布
            UserLearningStatisticsResponse.ContentDistribution mapDistribution = new UserLearningStatisticsResponse.ContentDistribution();
            mapDistribution.setType(BizType.LEARNING_MAP);
            mapDistribution.setTypeName("学习地图");
            mapDistribution.setCount(mapCount);
            mapDistribution.setPercentage(Math.round((double) mapCount / total * 100) / 100.0);
            distribution.add(mapDistribution);
        }
        
        return distribution;
    }
    
    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private User getUserById(Long userId) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserId, userId)
                .eq(User::getIsDel, 0);
        return userMapper.selectOne(userWrapper);
    }

    /**
     * 获取用户部门名称
     *
     * @param userId 用户ID
     * @return 部门名称
     */
    private String getUserDepartmentName(Long userId) {
        // 查询用户部门关联
        LambdaQueryWrapper<DepartmentUser> departmentUserWrapper = new LambdaQueryWrapper<>();
        departmentUserWrapper.eq(DepartmentUser::getUserId, userId)
                .eq(DepartmentUser::getIsDel, 0);

        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(departmentUserWrapper);
        if (departmentUsers.isEmpty()) {
            return "";
        }

        // 获取第一个部门信息
        Long departmentId = departmentUsers.get(0).getDepartmentId();
        LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.eq(Department::getId, departmentId)
                .eq(Department::getIsDel, 0);

        Department department = departmentMapper.selectOne(departmentWrapper);
        return department != null ? department.getDepartmentName() : "";
    }
    
    /**
     * 获取已完成课程数量
     *
     * @param userId 用户ID
     * @return 已完成课程数量
     */
    private Integer getCompletedCourseCount(Long userId) {
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
               .eq(UserLearningTask::getBizType, BizType.COURSE)
               .eq(UserLearningTask::getStatus, 2) // 已完成
               .eq(UserLearningTask::getIsDel, 0);
        
        return userLearningTaskMapper.selectCount(wrapper).intValue();
    }
    
    /**
     * 获取用户最近获得的证书
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 证书列表
     */
    private List<UserProfileResponse.Certificate> getRecentCertificates(Long userId, int limit) {
        List<UserProfileResponse.Certificate> certificates = new ArrayList<>();// 查询用户证书
        LambdaQueryWrapper<UserCertificate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCertificate::getUserId, userId)
                .eq(UserCertificate::getStatus, 0) // 0-有效
                .eq(UserCertificate::getIsDel, 0)
                .orderByDesc(UserCertificate::getIssueTime)
                .last("LIMIT " + limit);
        
        List<UserCertificate> userCertificates = userCertificateMapper.selectList(wrapper);
        
        // 转换为DTO
        for (UserCertificate userCertificate : userCertificates) {
            // 查询证书详情
            Certificate certificate = certificateMapper.selectById(userCertificate.getCertificateId());
            if (certificate != null) {
                UserProfileResponse.Certificate certificateDTO = UserProfileResponse.Certificate.builder()
                        .certificateId(certificate.getId())
                        .certificateName(certificate.getName())
                        .certificateImageUrl(certificate.getTemplateUrl())
                        .certificateNo(userCertificate.getCertificateNo())
                        .issueTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(userCertificate.getIssueTime()))
                        .build();
                certificates.add(certificateDTO);
            }
        }
        
        return certificates;
    }
    
    /**
     * 获取用户最近学习记录
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 学习记录列表
     */
    private List<UserProfileResponse.LearningRecord> getRecentLearningRecords(Long userId, int limit) {
        List<UserProfileResponse.LearningRecord> records = new ArrayList<>();
        
        // 查询最近的学习记录
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
               .eq(UserLearningTask::getIsDel, 0)
               .orderByDesc(UserLearningTask::getLastStudyTime)
               .last("LIMIT " + limit);
        
        List<UserLearningTask> tasks = userLearningTaskMapper.selectList(wrapper);
        
        // 转换为DTO
        for (UserLearningTask task : tasks) {
            String contentName = "";
            String coverUrl = "";
            
            // 根据不同的业务类型获取内容信息
            if (BizType.COURSE.equals(task.getBizType())) {
                // 获取课程信息
                Courses course = coursesMapper.selectById(task.getBizId());
                if (course != null) {
                    contentName = course.getTitle();
                    coverUrl = course.getCoverImage();
                }
            } else if (BizType.TRAIN.equals(task.getBizType())) {
                // 获取培训信息
                Train train = trainMapper.selectById(task.getBizId());
                if (train != null) {
                    contentName = train.getName();
                    coverUrl = train.getCover();
                }
            } else if (BizType.LEARNING_MAP.equals(task.getBizType())) {
                // 获取学习地图信息
                LearningMap map = learningMapMapper.selectById(task.getBizId());
                if (map != null) {
                    contentName = map.getName();
                    coverUrl = map.getCover();
                }
            }
            
            UserProfileResponse.LearningRecord recordDTO = UserProfileResponse.LearningRecord.builder()
                    .id(task.getId())
                    .contentId(task.getBizId())
                    .contentName(contentName)
                    .contentType(task.getBizType())
                    .coverUrl(coverUrl)
                    .studyDuration(task.getStudyDuration())
                    .startTime(task.getStartTime() != null ? 
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getStartTime()) : "")
                    .progress(task.getProgress())
                    .build();
            
            records.add(recordDTO);
        }
        
        return records;
    }
}
