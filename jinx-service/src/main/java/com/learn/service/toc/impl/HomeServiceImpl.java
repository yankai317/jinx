package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.enums.LearningTaskStatusEnums;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.home.*;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.toc.HomeService;
import com.learn.service.toc.TrainService;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 首页服务实现类
 */
@Service
@Slf4j
public class HomeServiceImpl implements HomeService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;
    
    @Autowired
    private PersonalCenterServiceImpl personalCenterService;
    @Autowired
    private TrainService trainService;

    /**
     * 获取首页数据
     *
     * @param userId 用户ID
     * @return 首页数据
     */
    @Override
    public HomeDataDTO getHomeData(Long userId) {
        // 创建返回对象
        HomeDataDTO homeData = new HomeDataDTO();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 获取用户基本信息
            CompletableFuture<HomeUserCardInfoDTO> userInfo = CompletableFuture.supplyAsync(() -> personalCenterService.getUserCardInfo(userId), executor);

            // 获取学习任务数据
            CompletableFuture<LearningTasksDTO> learningTasks = CompletableFuture.supplyAsync(() -> getLearningTasks(userId), executor);

            // 获取推荐课程数据
            CompletableFuture<RecommendedCoursesDTO> recommendedCourses = CompletableFuture.supplyAsync(this::getRecommendedCourses, executor);

            // 等待所有任务完成并获取结果
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    userInfo, learningTasks, recommendedCourses);

            // 阻塞等待所有任务完成
            allFutures.join();
            homeData.setUserInfo(userInfo.get());
            homeData.setLearningTasks(learningTasks.get());
            homeData.setRecommendedCourses(recommendedCourses.get());

            return homeData;
        } catch (Exception e) {
            log.error("获取首页数据失败", e);
            throw new RuntimeException("获取首页数据失败");
        }
    }


    /**
     * 获取学习任务数据
     *
     * @param userId 用户ID
     * @return 学习任务DTO
     */
    private LearningTasksDTO getLearningTasks(Long userId) {
        LearningTasksDTO learningTasksDTO = new LearningTasksDTO();

        // 2. 获取用户的学习记录，用于后续判断指派是否已存在学习记录
        List<UserLearningTask> trainLearningTasks = userLearningTaskMapper.getLearningTask(userId, BizType.TRAIN);
        List<UserLearningTask> mapLearningTasks = userLearningTaskMapper.getLearningTask(userId, BizType.LEARNING_MAP);
        if (CollectionUtils.isEmpty(trainLearningTasks) && CollectionUtils.isEmpty(mapLearningTasks)) {
            return learningTasksDTO;
        }

        if (CollectionUtils.isNotEmpty(trainLearningTasks)) {
            List<LearningTaskItemDTO> trainItems = createTaskItem(BizType.TRAIN, trainLearningTasks);
            learningTasksDTO.setTrainings(trainItems);
        }
        if (CollectionUtils.isNotEmpty(mapLearningTasks)) {
            List<LearningTaskItemDTO> mapItems = createTaskItem(BizType.LEARNING_MAP, mapLearningTasks);
            learningTasksDTO.setLearningMaps(mapItems);
        }
        return learningTasksDTO;
    }
    
    /**
     * 创建学习任务项
     *
     * @param bizType 业务类型
     * @param learningTasks 业务
     * @return 学习任务项DTO
     */
    private List<LearningTaskItemDTO> createTaskItem(String bizType, List<UserLearningTask> learningTasks) {
        List<Long> bizIds = learningTasks.stream().map(UserLearningTask::getBizId).collect(Collectors.toList());
        Map<Long, UserLearningTask> learningTaskMap = learningTasks.stream().collect(Collectors.toMap(UserLearningTask::getBizId, v -> v));
        switch (bizType) {
            case BizType.COURSE:
                // 查询课程信息
                LambdaQueryWrapper<Courses> courseWrapper = new LambdaQueryWrapper<>();
                courseWrapper.in(Courses::getId, bizIds)
                        .eq(Courses::getIsDel, 0);
                List<Courses> course = coursesMapper.selectList(courseWrapper);
                return course.stream().map(v -> {
                    LearningTaskItemDTO courseItem = new LearningTaskItemDTO();
                    courseItem.setId(v.getId());
                    courseItem.setSource(BizConstants.SOURCE_ASSIGN);
                    courseItem.setName(v.getTitle());
                    courseItem.setType(BizType.COURSE);
                    courseItem.setCover(v.getCoverImage());
                    courseItem.setCredit(v.getCredit());
                    // 设置学习进度和状态
                    UserLearningTask learningTask = learningTaskMap.get(v.getId());
                    courseItem.setProgress(learningTask.getProgress());
                    courseItem.setStatus(learningTask.getStatus());
                    courseItem.setDeadline(learningTask.getDeadline());
                    return courseItem;
                }).toList();
            case BizType.TRAIN:
                // 查询培训信息
                LambdaQueryWrapper<Train> trainWrapper = new LambdaQueryWrapper<>();
                trainWrapper.in(Train::getId, bizIds)
                        .eq(Train::getIsDel, 0);
                List<Train> trains = trainMapper.selectList(trainWrapper);
                return trains.stream().map(v -> {
                    LearningTaskItemDTO trainItem = new LearningTaskItemDTO();
                    trainItem.setId(v.getId());
                    trainItem.setSource(BizConstants.SOURCE_ASSIGN);
                    trainItem.setName(v.getName());
                    trainItem.setType(BizType.TRAIN);
                    trainItem.setCover(v.getCover());
                    trainItem.setCredit(v.getCredit());
                    // 设置学习进度和状态
                    UserLearningTask learningTask = learningTaskMap.get(v.getId());
                    trainItem.setProgress(learningTask.getProgress());
                    trainItem.setStatus(learningTask.getStatus());
                    trainItem.setDeadline(learningTask.getDeadline());
                    return trainItem;
                }).toList();
            case BizType.LEARNING_MAP:
                // 查询培训信息
                LambdaQueryWrapper<LearningMap> mapWrapper = new LambdaQueryWrapper<>();
                mapWrapper.in(LearningMap::getId, bizIds)
                        .eq(LearningMap::getIsDel, 0);
                List<LearningMap> maps = learningMapMapper.selectList(mapWrapper);
                return maps.stream().map(v -> {
                    LearningTaskItemDTO mapItem = new LearningTaskItemDTO();
                    mapItem.setId(v.getId());
                    mapItem.setSource(BizConstants.SOURCE_ASSIGN);
                    mapItem.setName(v.getName());
                    mapItem.setType(BizType.LEARNING_MAP);
                    mapItem.setCover(v.getCover());
                    mapItem.setCredit(v.getRequiredCredit());
                    // 设置学习进度和状态
                    UserLearningTask learningTask = learningTaskMap.get(v.getId());
                    mapItem.setProgress(learningTask.getProgress());
                    mapItem.setStatus(learningTask.getStatus());
                    mapItem.setDeadline(learningTask.getDeadline());
                    return mapItem;
                }).toList();
                
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取推荐课程数据
     *
     * @return 推荐课程DTO
     */
    private RecommendedCoursesDTO getRecommendedCourses() {
        RecommendedCoursesDTO recommendedCoursesDTO = new RecommendedCoursesDTO();

        // 获取推荐课程
        List<CourseItemDTO> courses = new ArrayList<>();
        List<Courses> coursesList = coursesMapper.selectList(
                new LambdaQueryWrapper<Courses>()
                        .eq(Courses::getStatus, "published")
                        .eq(Courses::getIsDel, 0)
                        .orderByDesc(Courses::getViewCount)
                        .last("LIMIT 6") // 取前6个
        );

        for (Courses course : coursesList) {
            CourseItemDTO courseItem = new CourseItemDTO();
            courseItem.setId(course.getId());
            courseItem.setName(course.getTitle());
            courseItem.setType("COURSE");
            courseItem.setCover(course.getCoverImage());
            courseItem.setIntroduction(course.getDescription());
            courseItem.setCredit(course.getCredit());
            courseItem.setViewCount(course.getViewCount());
            courseItem.setCompleteCount(course.getCompleteCount());
            courses.add(courseItem);
        }
        recommendedCoursesDTO.setCourses(courses);
//
//        // 获取推荐培训
//        List<CourseItemDTO> trainings = new ArrayList<>();
//        List<Train> trainList = trainMapper.selectList(
//                new LambdaQueryWrapper<Train>()
//                        .eq(Train::getStatus, "published")
//                        .eq(Train::getIsDel, 0)
//                        .orderByDesc(Train::getGmtCreate)
//                        .last("LIMIT 6") // 取前6个
//        );
//
//        for (Train train : trainList) {
//            CourseItemDTO courseItem = new CourseItemDTO();
//            courseItem.setId(train.getId());
//            courseItem.setName(train.getName());
//            courseItem.setType("TRAIN");
//            courseItem.setCover(train.getCover());
//            courseItem.setIntroduction(train.getIntroduction());
//            courseItem.setCredit(train.getCredit());
//            // 培训没有查看数和完成人数字段，设为0
//            courseItem.setViewCount(0);
//            courseItem.setCompleteCount(0);
//            trainings.add(courseItem);
//        }
//        recommendedCoursesDTO.setTrainings(trainings);
//
//        // 获取推荐学习地图
//        List<CourseItemDTO> learningMaps = new ArrayList<>();
//        List<LearningMap> learningMapList = learningMapMapper.selectList(
//                new LambdaQueryWrapper<LearningMap>()
//                        .eq(LearningMap::getIsDel, 0)
//                        .orderByDesc(LearningMap::getGmtCreate)
//                        .last("LIMIT 6") // 取前6个
//        );
//
//        for (LearningMap learningMap : learningMapList) {
//            CourseItemDTO courseItem = new CourseItemDTO();
//            courseItem.setId(learningMap.getId());
//            courseItem.setName(learningMap.getName());
//            courseItem.setType("MAP");
//            courseItem.setCover(learningMap.getCover());
//            courseItem.setIntroduction(learningMap.getIntroduction());
//            // 学习地图的学分是必修学分和选修学分的总和
//            courseItem.setCredit(learningMap.getRequiredCredit() != null && learningMap.getElectiveCredit() != null ?
//                    (learningMap.getRequiredCredit().intValue() + learningMap.getElectiveCredit().intValue()) : 0);
//            // 学习地图没有查看数和完成人数字段，设为0
//            courseItem.setViewCount(0);
//            courseItem.setCompleteCount(0);
//            learningMaps.add(courseItem);
//        }
//        recommendedCoursesDTO.setLearningMaps(learningMaps);

        return recommendedCoursesDTO;
    }

    /**
     * 获取排行榜数据
     *
     * @return 排行榜DTO
     */
    private RankingsDTO getRankings() {
        RankingsDTO rankingsDTO = new RankingsDTO();

        // 学习时长排行榜
        List<RankingItemDTO> studyDurationRanking = getStudyDurationRanking();
        rankingsDTO.setStudyDurationRanking(studyDurationRanking);

        // 课程完成数排行榜
        List<RankingItemDTO> courseCompletionRanking = getCourseCompletionRanking();
        rankingsDTO.setCourseCompletionRanking(courseCompletionRanking);

        // 获得证书数排行榜
        List<RankingItemDTO> certificateRanking = getCertificateRanking();
        rankingsDTO.setCertificateRanking(certificateRanking);

        return rankingsDTO;
    }

    /**
     * 获取学习时长排行榜
     *
     * @return 排行榜项列表
     */
    private List<RankingItemDTO> getStudyDurationRanking() {
        // 获取所有用户的学习时长
        Map<Long, Integer> userStudyDurationMap = new HashMap<>();

        // 使用新表 user_learning_task 查询所有用户的学习时长
        List<UserLearningTask> userLearningTasks = userLearningTaskMapper.selectList(
                new LambdaQueryWrapper<UserLearningTask>()
                        .eq(UserLearningTask::getIsDel, 0)
        );
        
        // 累加每个用户的学习时长
        for (UserLearningTask task : userLearningTasks) {
            Long userId = task.getUserId();
            Integer duration = task.getStudyDuration() != null ? task.getStudyDuration() : 0;
            userStudyDurationMap.put(userId, userStudyDurationMap.getOrDefault(userId, 0) + duration);
        }

        // 按学习时长排序
        List<Map.Entry<Long, Integer>> sortedList = new ArrayList<>(userStudyDurationMap.entrySet());
        sortedList.sort(Map.Entry.<Long, Integer>comparingByValue().reversed());

        // 取前10名
        List<RankingItemDTO> rankingList = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedList) {
            if (rank > 10) {
                break;
            }

            Long userId = entry.getKey();
            Integer studyDuration = entry.getValue();

            // 获取用户信息
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUserId, userId)
                            .eq(User::getIsDel, 0));
            if (user == null) {
                continue;
            }

            RankingItemDTO rankingItem = new RankingItemDTO();
            rankingItem.setUserId(userId);
            rankingItem.setNickname(user.getNickname());
            rankingItem.setAvatar(user.getAvatar());
            rankingItem.setRank(rank++);
            rankingItem.setStudyDuration(studyDuration);

            // 获取部门信息
//            DepartmentUser departmentUser = departmentUserMapper.selectOne(
//                    new LambdaQueryWrapper<DepartmentUser>()
//                            .eq(DepartmentUser::getUserId, userId)
//                            .eq(DepartmentUser::getIsDel, 0)
//            );
//            if (departmentUser != null) {
//                Department department = departmentMapper.selectById(departmentUser.getDepartmentId());
//                if (department != null) {
//                    rankingItem.setDepartmentName(department.getDepartmentName());
//                }
//            }

            rankingList.add(rankingItem);
        }

        return rankingList;
    }

    /**
     * 获取课程完成数排行榜
     *
     * @return 排行榜项列表
     */
    private List<RankingItemDTO> getCourseCompletionRanking() {
        // 获取所有用户的课程完成数
        Map<Long, Integer> userCourseCompletionMap = new HashMap<>();

        // 使用新表 user_learning_task 查询所有已完成的课程学习记录
        List<UserLearningTask> completedCourseTasks = userLearningTaskMapper.selectList(
                new LambdaQueryWrapper<UserLearningTask>()
                        .eq(UserLearningTask::getBizType, BizType.COURSE)
                        .eq(UserLearningTask::getStatus, 2) // 已完成状态
                        .eq(UserLearningTask::getIsDel, 0)
        );
        
        // 统计每个用户完成的课程数量
        for (UserLearningTask task : completedCourseTasks) {
            Long userId = task.getUserId();
            userCourseCompletionMap.put(userId, userCourseCompletionMap.getOrDefault(userId, 0) + 1);
        }

        // 按课程完成数排序
        List<Map.Entry<Long, Integer>> sortedList = new ArrayList<>(userCourseCompletionMap.entrySet());
        sortedList.sort(Map.Entry.<Long, Integer>comparingByValue().reversed());

        // 取前10名
        List<RankingItemDTO> rankingList = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedList) {
            if (rank > 10) {
                break;
            }

            Long userId = entry.getKey();
            Integer completedCourseCount = entry.getValue();

            // 获取用户信息
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUserId, userId)
                            .eq(User::getIsDel, 0)
            );
            if (user == null) {
                continue;
            }

            RankingItemDTO rankingItem = new RankingItemDTO();
            rankingItem.setUserId(userId);
            rankingItem.setNickname(user.getNickname());
            rankingItem.setAvatar(user.getAvatar());
            rankingItem.setRank(rank++);
            rankingItem.setCompletedCourseCount(completedCourseCount);

            // 获取部门信息
            DepartmentUser departmentUser = departmentUserMapper.selectOne(
                    new LambdaQueryWrapper<DepartmentUser>()
                            .eq(DepartmentUser::getUserId, userId)
                            .eq(DepartmentUser::getIsDel, 0)
            );
            if (departmentUser != null) {
                Department department = departmentMapper.selectById(departmentUser.getDepartmentId());
                if (department != null) {
                    rankingItem.setDepartmentName(department.getDepartmentName());
                }
            }

            rankingList.add(rankingItem);
        }

        return rankingList;
    }

    /**
     * 获取证书数排行榜
     *
     * @return 排行榜项列表
     */
    private List<RankingItemDTO> getCertificateRanking() {
        // 获取所有用户的证书数
        Map<Long, Integer> userCertificateCountMap = new HashMap<>();

        // 查询所有有效证书
        List<UserCertificate> userCertificates = userCertificateMapper.selectList(
                new LambdaQueryWrapper<UserCertificate>()
                        .eq(UserCertificate::getStatus, 0) // 有效状态
                        .eq(UserCertificate::getIsDel, 0)
        );
        for (UserCertificate certificate : userCertificates) {
            Long userId = certificate.getUserId();
            userCertificateCountMap.put(userId, userCertificateCountMap.getOrDefault(userId, 0) + 1);
        }

        // 按证书数排序
        List<Map.Entry<Long, Integer>> sortedList = new ArrayList<>(userCertificateCountMap.entrySet());
        sortedList.sort(Map.Entry.<Long, Integer>comparingByValue().reversed());

        // 取前10名
        List<RankingItemDTO> rankingList = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedList) {
            if (rank > 10) {
                break;
            }

            Long userId = entry.getKey();
            Integer certificateCount = entry.getValue();

            // 获取用户信息
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUserId, userId)
                            .eq(User::getIsDel, 0)
            );
            if (user == null) {
                continue;
            }

            RankingItemDTO rankingItem = new RankingItemDTO();
            rankingItem.setUserId(userId);
            rankingItem.setNickname(user.getNickname());
            rankingItem.setAvatar(user.getAvatar());
            rankingItem.setRank(rank++);
            rankingItem.setCertificateCount(certificateCount);

            // 获取部门信息
            DepartmentUser departmentUser = departmentUserMapper.selectOne(
                    new LambdaQueryWrapper<DepartmentUser>()
                            .eq(DepartmentUser::getUserId, userId)
                            .eq(DepartmentUser::getIsDel, 0)
            );
            if (departmentUser != null) {
                Department department = departmentMapper.selectById(departmentUser.getDepartmentId());
                if (department != null) {
                    rankingItem.setDepartmentName(department.getDepartmentName());
                }
            }

            rankingList.add(rankingItem);
        }

        return rankingList;
    }
}
