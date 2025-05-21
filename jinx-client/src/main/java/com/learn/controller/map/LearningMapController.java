package com.learn.controller.map;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.query.StatisticsQueryBO;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.MapCreatorUpdateRequest;
import com.learn.dto.map.*;
import com.learn.dto.train.StatisticLearnersResponse;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.StatisticService;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.user.UserInfoService;
import dev.ai4j.openai4j.Json;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.map.LearnMapCreateService;
import com.learn.service.map.LearnMapDeleteService;
import com.learn.service.map.LearnMapQueryService;
import com.learn.service.map.LearnMapStatisticsService;
import com.learn.service.map.LearnMapUpdateService;
import com.learn.service.map.LearnMapUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.learn.constants.BizType.LEARNING_MAP;
import static com.learn.constants.BizType.TRAIN;

/**
 * 学习地图控制器
 */
@RestController
@RequestMapping("/api/learningMap")
@CrossOrigin
@Slf4j
public class LearningMapController {

    @Autowired
    private LearnMapQueryService learnMapQueryService;

    @Autowired
    private LearnMapCreateService learnMapCreateService;

    @Autowired
    private LearnMapUpdateService learnMapUpdateService;

    @Autowired
    private LearnMapDeleteService learnMapDeleteService;

    @Autowired
    private LearnMapStatisticsService learnMapStatisticsService;

    @Autowired
    private StatisticService statisticService;

    @Autowired
    private LearnMapUserService learnMapUserService;

    @Autowired
    private UserTokenUtil userTokenUtil;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 获取学习地图列表
     *
     * @param request            查询请求
     * @param httpServletRequest HTTP请求
     * @return 学习地图列表响应
     */
    @PostMapping("/list")
    public ApiResponse<LearningMapListResponse> getLearningMapList(
            @RequestBody LearningMapListRequest request,
            HttpServletRequest httpServletRequest) {
        log.info("获取学习地图列表请求入参: {}", request);

        // 参数校验
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }

        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(httpServletRequest);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            request.setCreatorId(UserContextHolder.getUserId());
        }

        // 调用服务获取学习地图列表
        LearningMapListResponse response = learnMapQueryService.getLearningMapList(request, userId);

        return ApiResponse.success("获取成功", response);
    }

    /**
     * 获取学习地图详情
     *
     * @param id                 学习地图ID
     * @param httpServletRequest HTTP请求
     * @return 学习地图详情响应
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<LearningMapDetailDTO> getLearningMapDetail(
            @PathVariable("id") Long id,
            HttpServletRequest httpServletRequest) {
        log.info("获取学习地图详情请求入参, id: {}", id);

        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error("学习地图ID无效");
        }

        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(httpServletRequest);

        // 调用服务获取学习地图详情
        LearningMapDetailDTO detailDTO = learnMapQueryService.getLearningMapDetail(id, userId);

        if (detailDTO == null) {
            return ApiResponse.error("学习地图不存在或无权访问");
        }

        return ApiResponse.success("获取成功", detailDTO);
    }

    /**
     * 创建学习地图
     *
     * @param request            创建学习地图请求
     * @param httpServletRequest HTTP请求
     * @return 创建学习地图响应
     */
    @PostMapping("/create")
    public ApiResponse<LearningMapCreateResponse> createLearningMap(
            @RequestBody LearningMapCreateRequest request,
            HttpServletRequest httpServletRequest) {
        log.info("创建学习地图请求入参: {}", request);

        try {
            // 获取当前用户ID和用户名
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            // 调用服务创建学习地图
            LearningMapCreateResponse response = learnMapCreateService.createLearningMap(request, userInfo.getUserId(), userInfo.getNickname());

            return ApiResponse.success("创建成功", response);
        } catch (Exception e) {
            log.error("创建学习地图失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 更新学习地图
     *
     * @param request            更新学习地图请求
     * @param httpServletRequest HTTP请求
     * @return 更新学习地图响应
     */
    @PutMapping("/update")
    public ApiResponse<Boolean> updateLearningMap(
            @RequestBody LearningMapUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        log.info("更新学习地图请求入参: {}", request);

        try {
            // 参数校验
            if (request.getId() == null || request.getId() <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }

            // 获取当前用户ID和用户名
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(LEARNING_MAP)
                    .setTypeId(request.getId())
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务更新学习地图
            Boolean result = learnMapUpdateService.updateLearningMap(request, userInfo.getUserId(), userInfo.getNickname());
            return ApiResponse.success("更新成功", result);
        } catch (Exception e) {
            log.error("更新学习地图失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除学习地图
     *
     * @param id                 学习地图ID
     * @param httpServletRequest HTTP请求
     * @return 删除学习地图响应
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteLearningMap(
            @PathVariable("id") Long id,
            HttpServletRequest httpServletRequest) {
        log.info("删除学习地图请求入参, id: {}", id);

        try {
            // 参数校验
            if (id == null || id <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }

            // 获取当前用户ID和用户名
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(LEARNING_MAP)
                    .setTypeId(id)
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务删除学习地图
            Boolean result = learnMapDeleteService.deleteLearningMap(id, userInfo.getUserId(), userInfo.getNickname());

            return ApiResponse.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除学习地图失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取学习地图统计数据
     *
     * @param id 学习地图ID
     * @return 学习地图统计数据响应
     */
    @GetMapping("/statistics/{id}")
    public ApiResponse<LearningMapStatisticsDTO> getLearningMapStatistics(
            @PathVariable("id") Long id) {
        log.info("获取学习地图统计数据请求入参, id: {}", id);

        try {
            // 参数校验
            if (id == null || id <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }

            // 获取当前用户ID
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                return ApiResponse.error("用户未登录");
            }

            // 调用服务获取学习地图统计数据
            StatisticsQueryBO queryBO = new StatisticsQueryBO();
            queryBO.setBizId(id);
            queryBO.setBizType(LEARNING_MAP);
            LearningMapStatisticsDTO statisticsDTO = statisticService.getStatistics(queryBO, LearningMapStatisticsDTO.class);
            if (statisticsDTO == null) {
                return ApiResponse.error("学习地图不存在或无权访问");
            }

            learnMapStatisticsService.setLearningMapStatistics(statisticsDTO);

            return ApiResponse.success("获取成功", statisticsDTO);
        } catch (Exception e) {
            log.error("获取学习地图统计数据失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取学习地图学习人员列表
     *
     * @param id      学习地图ID
     * @param request 查询请求参数
     * @return 学习人员列表响应
     */
    @PostMapping("/learners/{id}")
    public ApiResponse<StatisticLearnersResponse> getLearningMapLearners(
            @PathVariable("id") Long id,
            @RequestBody LearningMapLearnersRequest request) {
        log.info("获取学习地图学习人员列表请求入参, id: {}, request: {}", id, request);

        try {
            // 参数校验
            if (id == null || id <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }

            // 设置默认分页参数
            if (request.getPageNum() == null || request.getPageNum() < 1) {
                request.setPageNum(1);
            }
            if (request.getPageSize() == null || request.getPageSize() < 1) {
                request.setPageSize(10);
            }

            // 获取当前用户ID
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                return ApiResponse.error("用户未登录");
            }

            // 调用服务获取学习地图学习人员列表
            StatisticsQueryBO queryBO = new StatisticsQueryBO();
            queryBO.setStatus(request.getStatus());
            queryBO.setPageSize(request.getPageSize());
            queryBO.setPageNum(request.getPageNum());
            queryBO.setBizId(id);
            queryBO.setBizType(LEARNING_MAP);
            queryBO.setUserId(userId);
            StatisticLearnersResponse response = statisticService.getLearners(queryBO);

            return ApiResponse.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取学习地图学习人员列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取学习地图学员学习详情
     *
     * @param mapId              学习地图ID
     * @param userId             用户ID
     * @param httpServletRequest HTTP请求
     * @return 学习详情响应
     */
    @GetMapping("/learner/detail")
    public ApiResponse<LearningMapLearnerDetailDTO> getLearningMapLearnerDetail(
            @RequestParam("mapId") Long mapId,
            @RequestParam("userId") Long userId,
            HttpServletRequest httpServletRequest) {
        log.info("获取学习地图学员学习详情请求入参, mapId: {}, userId: {}", mapId, userId);

        try {
            // 参数校验
            if (mapId == null || mapId <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }
            if (userId == null || userId <= 0) {
                return ApiResponse.error("用户ID无效");
            }

            // 获取当前用户ID（操作者）
            Long currentUserId = userTokenUtil.getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return ApiResponse.error("用户未登录");
            }

            // 调用服务获取学习地图学员学习详情
            LearningMapLearnerDetailDTO detailDTO = learnMapUserService.getLearningMapLearnerDetail(mapId, userId);

            if (detailDTO == null) {
                return ApiResponse.error("未找到学员学习记录");
            }

            return ApiResponse.success("获取成功", detailDTO);
        } catch (Exception e) {
            log.error("获取学习地图学员学习详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 发送学习提醒
     *
     * @param mapId              学习地图ID
     * @param userId             用户ID
     * @param content            提醒内容
     * @param httpServletRequest HTTP请求
     * @return 发送结果
     */
    @PostMapping("/learner/remind")
    public ApiResponse<Boolean> sendLearningReminder(
            @RequestParam("mapId") Long mapId,
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content,
            HttpServletRequest httpServletRequest) {
        log.info("发送学习提醒请求入参, mapId: {}, userId: {}, content: {}", mapId, userId, content);

        try {
            // 参数校验
            if (mapId == null || mapId <= 0) {
                return ApiResponse.error("学习地图ID无效");
            }
            if (userId == null || userId <= 0) {
                return ApiResponse.error("用户ID无效");
            }
            if (content == null || content.trim().isEmpty()) {
                return ApiResponse.error("提醒内容不能为空");
            }

            // 获取当前用户ID（操作者）
            Long currentUserId = userTokenUtil.getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return ApiResponse.error("用户未登录");
            }

            // 调用服务发送学习提醒
            // 这里简化处理，实际项目中可能需要更复杂的逻辑
            // todo 例如通过消息系统发送提醒，或者发送邮件、短信等
            // 这里假设发送成功
            Boolean result = true;

            return ApiResponse.success("发送成功", result);
        } catch (Exception e) {
            log.error("发送学习提醒失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 批量删除学习地图
     *
     * @param request            批量删除请求
     * @param httpServletRequest HTTP请求
     * @return 批量删除响应
     */
    @PostMapping("/batchDelete")
    public ApiResponse<LearningMapBatchDeleteResponse> batchDeleteLearningMap(
            @RequestBody LearningMapBatchDeleteRequest request,
            HttpServletRequest httpServletRequest) {
        log.info("批量删除学习地图请求入参: {}", request);

        // 参数校验
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return ApiResponse.error("学习地图ID列表不能为空");
        }

        try {
            // 获取当前用户ID和用户名
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(LEARNING_MAP)
                    .setTypeIds(request.getIds())
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务批量删除学习地图
            LearningMapBatchDeleteResponse response = learnMapDeleteService.batchDeleteLearningMap(
                    request.getIds(), userInfo.getUserId(), userInfo.getNickname());

            return ApiResponse.success("批量删除成功", response);
        } catch (Exception e) {
            log.error("批量删除学习地图失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 批量更新学习地图创建人
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @PostMapping("/batchUpdateCreator")
    public ApiResponse<Boolean> batchUpdateCreator(@RequestBody MapCreatorUpdateRequest request) {
        log.info("批量更新地图创建人请求入参:{}", Json.toJson(request));

        // 参数校验
        if (request.getMapIds() == null || request.getMapIds().isEmpty()) {
            return ApiResponse.error(400, "学习地图ID列表不能为空");
        }
        if (request.getNewCreatorId() == null) {
            return ApiResponse.error(400, "新的创建人ID不能为空");
        }

        try {
            // 获取当前用户信息
            UserTokenInfo userInfo = UserContextHolder.getUserInfo();

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(LEARNING_MAP)
                    .setTypeIds(request.getMapIds())
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 获取新创建人信息
            UserInfoResponse newCreator = userInfoService.getUserInfo(request.getNewCreatorId());
            if (newCreator == null) {
                return ApiResponse.error(400, "新的创建人不存在");
            }

            // 调用服务批量更新学习地图创建人
            boolean result = learnMapUpdateService.batchUpdateCreator(
                    request.getMapIds(),
                    request.getNewCreatorId(),
                    newCreator.getNickname(),
                    userInfo.getUserId(),
                    userInfo.getName()
            );

            return ApiResponse.success("批量更新创建人成功", result);
        } catch (Exception e) {
            log.error("批量更新学习地图创建人失败", e);
            return ApiResponse.error(500, "批量更新学习地图创建人失败：" + e.getMessage());
        }
    }
}
