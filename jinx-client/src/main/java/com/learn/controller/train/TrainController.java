package com.learn.controller.train;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.query.StatisticsQueryBO;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.CourseCreatorUpdateRequest;
import com.learn.dto.train.TrainBatchDeleteRequest;
import com.learn.dto.train.TrainBatchDeleteResponse;
import com.learn.dto.train.TrainCreateRequest;
import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.train.TrainLearnerDetailDTO;
import com.learn.dto.train.TrainLearnersRequest;
import com.learn.dto.train.StatisticLearnersResponse;
import com.learn.dto.train.TrainListRequest;
import com.learn.dto.train.TrainListResponse;
import com.learn.dto.train.StatisticsDTO;
import com.learn.dto.train.TrainUpdateRequest;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.User;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.StatisticService;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.operation.OperationLogService;
import com.learn.service.train.TrainCreateService;
import com.learn.service.train.TrainDeleteService;
import com.learn.service.train.TrainPublishService;
import com.learn.service.train.TrainQueryService;
import com.learn.service.train.TrainUnpublishService;
import com.learn.service.train.TrainUpdateService;
import com.learn.service.train.TrainUserService;
import com.learn.service.user.UserInfoService;
import dev.ai4j.openai4j.Json;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.learn.constants.BizType.COURSE;
import static com.learn.constants.BizType.TRAIN;

/**
 * 培训控制器
 */
@RestController
@RequestMapping("/api/train")
@CrossOrigin
@Slf4j
public class TrainController {

    @Autowired
    private TrainQueryService trainQueryService;
    @Autowired
    private TrainCreateService trainCreateService;
    @Autowired
    private TrainUpdateService trainUpdateService;
    @Autowired
    private TrainPublishService trainPublishService;
    @Autowired
    private TrainUnpublishService trainUnpublishService;
    @Autowired
    private TrainDeleteService trainDeleteService;
    @Autowired
    private TrainUserService trainUserService;
    @Autowired
    private StatisticService statisticService;

    @Autowired
    private UserTokenUtil userTokenUtil;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 创建培训
     *
     * @param request 创建培训请求
     * @return 创建的培训详情
     */
    @PostMapping("/create")
    public ApiResponse<TrainDetailDTO> createTrain(@RequestBody TrainCreateRequest request, HttpServletRequest httpServletRequest) {
        log.info("创建培训请求入参: {}", request);// 参数校验
        if (request == null) {
            return ApiResponse.error("请求参数不能为空");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return ApiResponse.error("培训名称不能为空");
        }

        try {
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);
            // 调用服务创建培训
            TrainDetailDTO detailDTO = trainCreateService.createTrain(request, userInfo);

            // 记录创建操作日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            try {
                String operationDetail = objectMapper.writeValueAsString(request);
                operationLogService.recordCreateOperation(detailDTO.getId(), TRAIN, operationDetail, tokenInfo);
            } catch (JsonProcessingException e) {
                log.error("记录培训创建操作日志失败", e);
            }

            return ApiResponse.success("创建成功", detailDTO);
        } catch (Exception e) {
            log.error("创建培训失败", e);
            return ApiResponse.error("创建培训失败: " + e.getMessage());
        }
    }

    /**
     * 获取培训列表
     *
     * @param request 培训列表查询请求
     * @return 培训列表查询响应
     */
    @PostMapping("/list")
    public ApiResponse<TrainListResponse> getTrainList(@RequestBody TrainListRequest request) {
        log.info("获取培训列表请求入参: {}", request);

        // 参数校验
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }

        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }

        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            request.setCreatorId(UserContextHolder.getUserId());
        }
        Map<String, List<Long>> range = userTokenUtil.getRange(userId);
        request.setTargetTypeAndIds(range);

        // 调用服务获取培训列表
        TrainListResponse response = trainQueryService.getTrainList(request);

        return ApiResponse.success("获取成功", response);
    }

    /**
     * 获取培训详情
     *
     * @param id 培训ID
     * @return 培训详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<TrainDetailDTO> getTrainDetail(@PathVariable("id") Long id) {
        log.info("获取培训详情请求入参, 培训ID: {}", id);

        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }

        // 调用服务获取培训详情
        TrainDetailDTO detailDTO = trainQueryService.getTrainDetail(id);
        if (detailDTO == null) {
            return ApiResponse.error("培训不存在或已删除");
        }

        return ApiResponse.success("获取成功", detailDTO);
    }

    /**
     * 更新培训信息
     *
     * @param request 更新培训请求
     * @return 更新结果
     */
    @PutMapping("/update")
    public ApiResponse<Boolean> updateTrain(@RequestBody TrainUpdateRequest request) {
        log.info("更新培训请求入参: {}", request);// 参数校验
        if (request == null) {
            return ApiResponse.error("请求参数不能为空");
        }

        if (request.getId() == null || request.getId() <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }

        try {

            // 获取当前用户信息
            Long userId = UserContextHolder.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeId(request.getId())
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务更新培训
            boolean success = trainUpdateService.updateTrain(request, userId);

            // 记录更新操作日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            if (tokenInfo != null) {
                String operationDetail = objectMapper.writeValueAsString(request);
                operationLogService.recordUpdateOperation(request.getId(), TRAIN, operationDetail, tokenInfo);
            }

            return ApiResponse.success("更新成功", success);
        } catch (Exception e) {
            log.error("更新培训失败", e);
            return ApiResponse.error("更新培训失败: " + e.getMessage());
        }
    }

    /**
     * 发布培训
     *
     * @param id 培训ID
     * @return 发布结果
     */
    @PutMapping("/publish/{id}")
    public ApiResponse<Boolean> publishTrain(@PathVariable("id") Long id) {
        log.info("发布培训请求入参,培训ID: {}", id);
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }
        try {
            // 获取当前用户信息
            UserTokenInfo userInfo = UserContextHolder.getUserInfo();
            Long userId = userInfo.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeId(id)
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务发布培训
            boolean success = trainPublishService.publishTrain(id, userInfo);

            // 记录发布操作日志
            if (userInfo != null) {
                try {
                    String operationDetail = "{\"trainId\":" + id + "}";
                    operationLogService.recordPublishOperation(id, TRAIN, operationDetail, userInfo);
                } catch (Exception e) {
                    log.error("记录培训发布操作日志失败", e);
                }
            }

            return ApiResponse.success("发布成功", success);
        } catch (Exception e) {
            log.error("发布培训失败", e);
            return ApiResponse.error("发布培训失败: " + e.getMessage());
        }
    }

    /**
     * 取消发布培训
     *
     * @param id 培训ID
     * @return 取消发布结果
     */
    @PutMapping("/unpublish/{id}")
    public ApiResponse<Boolean> unpublishTrain(@PathVariable("id") Long id) {
        log.info("取消发布培训请求入参,培训ID: {}", id);
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }
        try {

            UserTokenInfo userInfo = UserContextHolder.getUserInfo();
            Long userId = userInfo.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeId(id)
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务取消发布培训
            boolean success = trainUnpublishService.unpublishTrain(id, userInfo);

            // 记录取消发布操作日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            if (tokenInfo != null) {
                try {
                    String operationDetail = "{\"trainId\":" + id + "}";
                    operationLogService.recordUnpublishOperation(id, TRAIN, operationDetail, tokenInfo);
                } catch (Exception e) {
                    log.error("记录培训取消发布操作日志失败", e);
                }
            }

            return ApiResponse.success("取消发布成功", success);
        } catch (Exception e) {
            log.error("取消发布培训失败", e);
            return ApiResponse.error("取消发布培训失败: " + e.getMessage());
        }
    }

    /**
     * 删除培训
     *
     * @param id 培训ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteTrain(@PathVariable("id") Long id) {
        log.info("删除培训请求入参,培训ID: {}", id);// 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }
        try {

            // 记录删除操作日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            Long userId = tokenInfo.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeId(id)
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务删除培训
            boolean success = trainDeleteService.deleteTrain(id, tokenInfo);

            if (tokenInfo != null) {
                String operationDetail = "{\"trainId\":" + id + "}";
                operationLogService.recordDeleteOperation(id, TRAIN, operationDetail, tokenInfo);
            }

            return ApiResponse.success("删除成功", success);
        } catch (Exception e) {
            log.error("删除培训失败", e);
            return ApiResponse.error("删除培训失败: " + e.getMessage());
        }
    }

    /**
     * 获取培训统计数据
     *
     * @param id 培训ID
     * @return 培训统计数据
     */
    @GetMapping("/statistics/{id}")
    public ApiResponse<StatisticsDTO> getTrainStatistics(@PathVariable("id") Long id) {
        log.info("获取培训统计数据请求入参,培训ID: {}", id);

        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }

        try {
            // 调用服务获取培训统计数据
            StatisticsQueryBO queryBO = new StatisticsQueryBO();
            queryBO.setBizType(TRAIN);
            queryBO.setBizId(id);
            StatisticsDTO statisticsDTO = statisticService.getStatistics(queryBO, StatisticsDTO.class);
            return ApiResponse.success("获取成功", statisticsDTO);
        } catch (IllegalArgumentException e) {
            log.error("获取培训统计数据失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取培训统计数据失败", e);
            return ApiResponse.error("获取培训统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取培训学习人员列表
     *
     * @param id      培训ID
     * @param request 查询条件
     * @return 学习人员列表
     */
    @PostMapping("/learners/{id}")
    public ApiResponse<StatisticLearnersResponse> getTrainLearners(@PathVariable("id") Long id, @RequestBody TrainLearnersRequest request) {
        log.info("获取培训学习人员列表请求入参,培训ID: {}, 请求参数: {}", id, request);

        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }
        if (request == null) {
            request = new TrainLearnersRequest();
        }
        // 设置默认分页参数
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }

        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }

        try {
            // 调用服务获取培训学习人员列表
            StatisticsQueryBO queryBO = new StatisticsQueryBO();
            queryBO.setStatus(request.getStatus());
            queryBO.setPageSize(request.getPageSize());
            queryBO.setPageNum(request.getPageNum());
            queryBO.setBizId(id);
            queryBO.setBizType(TRAIN);
            queryBO.setUserId(request.getUserId());
            StatisticLearnersResponse response = statisticService.getLearners(queryBO);
            return ApiResponse.success("获取成功", response);
        } catch (IllegalArgumentException e) {
            log.error("获取培训学习人员列表失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取培训学习人员列表失败", e);
            return ApiResponse.error("获取培训学习人员列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取培训学员学习详情
     *
     * @param trainId 培训ID
     * @param userId  用户ID
     * @return 学习详情
     */
    @GetMapping("/learner/detail")
    public ApiResponse<TrainLearnerDetailDTO> getTrainLearnerDetail(@RequestParam("trainId") Long trainId, @RequestParam("userId") Long userId) {
        log.info("获取培训学员学习详情请求入参,培训ID: {}, 用户ID: {}", trainId, userId);// 参数校验
        if (trainId == null || trainId <= 0) {
            return ApiResponse.error("培训ID不能为空或小于等于0");
        }

        if (userId == null || userId <= 0) {
            return ApiResponse.error("用户ID不能为空或小于等于0");
        }
        try {
            // 调用服务获取培训学员学习详情
            TrainLearnerDetailDTO detailDTO = trainUserService.getTrainLearnerDetail(trainId, userId);

            if (detailDTO == null) {
                return ApiResponse.error("未找到学员学习记录");
            }

            return ApiResponse.success("获取成功", detailDTO);
        } catch (IllegalArgumentException e) {
            log.error("获取培训学员学习详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取培训学员学习详情失败", e);
            return ApiResponse.error("获取培训学员学习详情失败: " + e.getMessage());
        }
    }


    /**
     * 批量删除培训
     *
     * @param request 批量删除培训请求
     * @return 批量删除结果
     */
    @PostMapping("/batchDelete")
    public ApiResponse<TrainBatchDeleteResponse> batchDeleteTrain(@RequestBody TrainBatchDeleteRequest request, HttpServletRequest httpServletRequest) {
        log.info("批量删除培训请求入参: {}", request);

        // 参数校验
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return ApiResponse.error("培训ID列表不能为空");
        }

        try {
            // 获取用户信息用于记录日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            if (Objects.isNull(tokenInfo)) {
                throw new CommonException("未登录或登录已过期");
            }

            Long userId = tokenInfo.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeIds(request.getIds())
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));

            // 调用服务批量删除培训
            TrainBatchDeleteResponse response = trainDeleteService.batchDeleteTrain(request.getIds(), tokenInfo);

            // 记录批量删除操作日志
            try {
                String operationDetail = "{\"trainIds\":" + request.getIds() + "}";
                // 使用第一个ID作为主ID记录日志
                Long mainId = request.getIds().get(0);
                operationLogService.recordDeleteOperation(mainId, TRAIN, operationDetail, tokenInfo);
            } catch (Exception e) {
                log.error("记录培训批量删除操作日志失败", e);
            }

            return ApiResponse.success("批量删除成功", response);
        } catch (Exception e) {
            log.error("批量删除培训失败", e);
            return ApiResponse.error("批量删除培训失败: " + e.getMessage());
        }
    }


    /**
     * 批量更新课程创建人
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @PostMapping("/batchUpdateCreator")
    public ApiResponse<Boolean> batchUpdateCreator(@RequestBody CourseCreatorUpdateRequest request) {
        log.info("批量更新培训创建人请求入参:{}", Json.toJson(request));

        // 参数校验
        if (request.getCourseIds() == null || request.getCourseIds().isEmpty()) {
            return ApiResponse.error(400, "课程ID列表不能为空");
        }
        if (request.getNewCreatorId() == null) {
            return ApiResponse.error(400, "新的创建人ID不能为空");
        }

        try {
            // 获取当前用户信息
            UserTokenInfo userInfo = UserContextHolder.getUserInfo();
            Long userId = userInfo.getUserId();
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(TRAIN)
                    .setTypeIds(request.getCourseIds())
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 获取新创建人信息
            UserInfoResponse newCreator = userInfoService.getUserInfo(request.getNewCreatorId());
            if (newCreator == null) {
                return ApiResponse.error(400, "新的创建人不存在");
            }


            // 调用服务批量更新课程创建人
            boolean result = trainUpdateService.batchUpdateCreator(
                    request.getCourseIds(),
                    request.getNewCreatorId(),
                    newCreator.getNickname(),
                    userInfo.getUserId(),
                    userInfo.getName()
            );

            return ApiResponse.success("批量更新创建人成功", result);
        } catch (Exception e) {
            log.error("批量更新课程创建人失败", e);
            return ApiResponse.error(500, "批量更新课程创建人失败：" + e.getMessage());
        }
    }

}
