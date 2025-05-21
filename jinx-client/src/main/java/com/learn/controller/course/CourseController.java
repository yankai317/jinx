package com.learn.controller.course;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.util.JwtUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.*;
import com.learn.dto.course.CourseCreatorUpdateRequest;
import com.learn.dto.course.sub.VisibilityDTO;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.service.CommonRangeInterface;
import com.learn.service.course.CourseManageService;
import com.learn.service.course.CourseQueryService;
import com.learn.service.course.CourseUserService;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.user.UserInfoService;
import dev.ai4j.openai4j.Json;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.learn.constants.BizType.COURSE;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/api/course")
@CrossOrigin
@Slf4j
public class CourseController {

    @Autowired
    private CourseQueryService courseQueryService;

    @Autowired
    private CourseManageService courseManageService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CourseUserService courseUserService;

    @Autowired
    private UserTokenUtil userTokenUtil;
    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 获取课程详情
     *
     * @param id 课程ID
     * @return 课程详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<CourseDetailDTO> getCourseDetail(@PathVariable Long id) {
        log.info("获取课程详情请求，课程ID：{}", id);

        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        try {
            // 调用服务获取课程详情
            CourseDetailDTO courseDetail = courseQueryService.getCourseDetail(id);
            return ApiResponse.success("获取成功", courseDetail);
        } catch (Exception e) {
            log.error("获取课程详情失败", e);
            return ApiResponse.error(500, "获取课程详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取课程列表
     *
     * @param request     查询条件
     * @return 课程列表
     */
    @PostMapping("/list")
    public ApiResponse<CourseListResponse> getCourseList(@RequestBody CourseListRequest request) {
        log.info("获取课程列表请求入参:{}", Json.toJson(request));

        // 参数校验
        if (request.getPageNum() != null && request.getPageNum() < 1) {
            return ApiResponse.error(400, "页码必须大于等于1");
        }

        if (request.getPageSize() != null && request.getPageSize() < 1) {
            return ApiResponse.error(400, "每页条数必须大于等于1");
        }

        // 获取当前用户信息
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            // 如果是"只看我创建的"，设置当前用户ID
            if (request.getOnlyMine() != null && request.getOnlyMine()) {
                request.setCreatorId(userId);
            }
        }
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            request.setCreatorId(UserContextHolder.getUserId());
        }
        // 调用服务获取课程列表
        CourseListResponse response = courseQueryService.getCourseList(request);
        return ApiResponse.success("获取成功", response);
    }

    /**
     * 从请求中获取当前用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        // 提取token
        String token = authHeader.substring(7);

        // 从token中获取用户ID
        try {
            UserTokenInfo userInfo = jwtUtil.extractUserId(token);
            return userInfo.getUserId();
        } catch (Exception e) {
            log.error("从token中提取用户ID失败", e);
            return null;
        }
    }

    /**
     * 创建课程
     *
     * @param request 创建课程请求
     * @return 创建的课程信息
     */
    @PostMapping("/create")
    public ApiResponse<CourseDetailDTO> createCourse(@RequestBody CourseCreateRequest request, HttpServletRequest servletRequest) {
        log.info("创建课程请求入参:{}", Json.toJson(request));

        // 参数校验
        if (!StringUtils.hasText(request.getTitle())) {
            return ApiResponse.error(400, "课程名称不能为空");
        }

        if (!StringUtils.hasText(request.getType())) {
            return ApiResponse.error(400, "课程类型不能为空");
        }

        // 校验课程类型
        List<String> validTypes = Arrays.asList("video", "document", "series", "article");
        if (!validTypes.contains(request.getType())) {
            return ApiResponse.error(400, "课程类型无效，有效值为：video, document, series, article");
        }

        // 校验附件类型和路径（非文章类型必填）
        if (!"article".equals(request.getType()) && !"series".equals(request.getType())) {
            if (!StringUtils.hasText(request.getAppendixType())) {
                return ApiResponse.error(400, "非文章类型课程必须指定附件类型");
            }
            if (!StringUtils.hasText(request.getAppendixPath())) {
                return ApiResponse.error(400, "非文章类型课程必须指定附件路径");
            }
        }

        // 校验文章内容（仅文章类型）
        if ("article".equals(request.getType()) && !StringUtils.hasText(request.getAppendixPath())) {
            return ApiResponse.error(400, "文章类型课程必须提供文章内容");
        }

        // 校验系列课内容
        if ("series".equals(request.getType()) && (request.getAppendixFiles() == null || request.getAppendixFiles().isEmpty())) {
            return ApiResponse.error(400, "系列课类型课程必须提供文章");
        }

        try {

            Long userId = userTokenUtil.getCurrentUserId(servletRequest);
            request.setCreatorId(userId);
            // 获取用户详细信息
            UserInfoResponse userInfo = userInfoService.getUserInfo(userId);
            request.setCreatorName(userInfo.getNickname());

            // 调用服务创建课程
            Courses course = courseManageService.createCourse(request);

            // 转换为DTO
            CourseDetailDTO courseDetailDTO = convertToCourseDetailDTO(course);

            return ApiResponse.success("创建成功", courseDetailDTO);
        } catch (Exception e) {
            log.error("创建课程失败", e);
            return ApiResponse.error(500, "创建课程失败：" + e.getMessage());
        }
    }

    /**
     * 更新课程
     *
     * @param request 更新课程请求
     * @return 更新结果
     */
    @PutMapping("/update")
    public ApiResponse<CourseDetailDTO> updateCourse(@RequestBody CourseUpdateRequest request) {
        log.info("更新课程请求入参:{}", Json.toJson(request));

        // 参数校验
        if (request.getId() == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        try {
            // 获取当前用户信息
            Long userId = UserContextHolder.getUserId();
            // 设置更新人信息
            request.setUpdaterId(userId);
            // 获取用户详细信息
            UserInfoResponse userInfo = userInfoService.getUserInfo(userId);
            request.setUpdaterName(userInfo.getNickname());
            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeId(request.getId())
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 校验状态
            if (StringUtils.hasText(request.getStatus())) {
                List<String> validStatus = Arrays.asList("draft", "published");
                if (!validStatus.contains(request.getStatus())) {
                    return ApiResponse.error(400, "课程状态无效，有效值为：draft, published");
                }
            }

            // 校验可见范围
            if (request.getVisibility() != null) {
                VisibilityDTO visibility = request.getVisibility();
                if (!StringUtils.hasText(visibility.getType())) {
                    return ApiResponse.error(400, "可见范围类型不能为空");
                }
                List<String> validVisibilityTypes = Arrays.asList("ALL", "PART");
                if (!validVisibilityTypes.contains(visibility.getType())) {
                    return ApiResponse.error(400, "可见范围类型无效，有效值为：ALL, PART");
                }

                if ("PART".equals(visibility.getType()) &&
                        (visibility.getTargets() == null || visibility.getTargets().isEmpty())) {
                    return ApiResponse.error(400, "部分可见时必须指定可见目标");
                }
            }
            // 调用服务更新课程
            Courses course = courseManageService.updateCourse(request);
            // 转换为DTO
            CourseDetailDTO courseDetailDTO = convertToCourseDetailDTO(course);

            return ApiResponse.success("更新成功", courseDetailDTO);
        } catch (Exception e) {
            log.error("更新课程失败", e);
            return ApiResponse.error(500, "更新课程失败：" + e.getMessage());
        }
    }

    /**
     * 将Courses实体转换为CourseDetailDTO
     *
     * @param course Courses实体
     * @return CourseDetailDTO
     */
    private CourseDetailDTO convertToCourseDetailDTO(Courses course) {
        if (course == null) {
            return null;
        }

        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setType(course.getType());
        dto.setCoverImage(course.getCoverImage());
        dto.setInstructorId(course.getInstructorId());
        dto.setDescription(course.getDescription());
        dto.setCredit(course.getCredit());
        // 转换分类IDs
        if (StringUtils.hasText(course.getCategoryIds())) {
//            List<Long> categoryIdList = Arrays.stream(course.getCategoryIds().split(","))
//                    .map(Long::parseLong)
//                    .collect(Collectors.toList());
            dto.setCategoryIds(course.getCategoryIds());
        }

        dto.setStatus(course.getStatus());
        dto.setAllowComments(course.getAllowComments() != null && course.getAllowComments() == 1);
        dto.setIsTop(course.getIsTop() != null && course.getIsTop() == 1);
        dto.setViewCount(course.getViewCount());
        dto.setCompleteCount(course.getCompleteCount());
        dto.setArticle(course.getArticle());
        dto.setAppendixType(course.getAppendixType());
        dto.setAppendixPath(course.getAppendixPath());
        if (Objects.nonNull(course.getPublishTime())) {
            dto.setPublishTime(course.getPublishTime().toString());
        }
//        dto.setGmtCreate(course.getGmtCreate());
        dto.setCreatorId(course.getCreatorId());
        dto.setCreatorName(course.getCreatorName());
        dto.setIfIsCitable(course.getIfIsCitable());

        return dto;
    }

    /**
     * 删除课程
     *
     * @param id          课程ID
     * @param httpRequest HTTP请求
     * @return 删除结果
     */
    @PostMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteCourse(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.info("删除课程请求，课程ID：{}", id);

        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        try {
            // 获取当前用户信息
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ApiResponse.error(401, "未登录或登录已过期");
            }

            // 获取用户详细信息
            UserInfoResponse userInfo = userInfoService.getUserInfo(userId);

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeId(id)
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务删除课程
            boolean result = courseManageService.deleteCourse(id, userId, userInfo.getNickname());

            return ApiResponse.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除课程失败", e);
            return ApiResponse.error(500, "删除课程失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除课程
     *
     * @param request     批量删除课程请求
     * @param httpRequest HTTP请求
     * @return 批量删除结果
     */
    @PostMapping("/batchDelete")
    public ApiResponse<CourseBatchDeleteResponse> batchDeleteCourse(@RequestBody CourseBatchDeleteRequest request, HttpServletRequest httpRequest) {
        log.info("批量删除课程请求入参:{}", Json.toJson(request));// 参数校验
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ApiResponse.error(400, "课程ID列表不能为空");
        }

        try {
            // 获取用户详细信息
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeIds(request.getIds())
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务批量删除课程
            CourseBatchDeleteResponse response = courseManageService.batchDeleteCourse(request.getIds(), userInfo.getUserId(), userInfo.getNickname());

            return ApiResponse.success("批量删除成功", response);
        } catch (Exception e) {
            log.error("批量删除课程失败", e);
            return ApiResponse.error(500, "批量删除课程失败：" + e.getMessage());
        }
    }

    /**
     * 发布课程
     *
     * @param id          课程ID
     * @param httpRequest HTTP请求
     * @return 发布结果
     */
    @PostMapping("/publish/{id}")
    public ApiResponse<Boolean> publishCourse(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.info("发布课程请求，课程ID：{}", id);

        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        try {
            // 获取用户详细信息
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);
            String userName = userInfo.getNickname();
            Long userId = userInfo.getUserId();

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeId(id)
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务发布课程
            courseManageService.publishCourse(id, userId, userName);

            return ApiResponse.success("发布成功", true);
        } catch (Exception e) {
            log.error("发布课程失败", e);
            return ApiResponse.error(500, "发布课程失败：" + e.getMessage());
        }
    }

    /**
     * 取消发布课程
     *
     * @param id          课程ID
     * @param httpRequest HTTP请求
     * @return 取消发布结果
     */
    @PutMapping("/unpublish/{id}")
    public ApiResponse<Boolean> unpublishCourse(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.info("取消发布课程请求，课程ID：{}", id);

        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        try {
            // 获取当前用户信息
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ApiResponse.error(401, "未登录或登录已过期");
            }

            // 获取用户详细信息
            UserInfoResponse userInfo = userInfoService.getUserInfo(userId);
            String userName = userInfo != null ? userInfo.getNickname() : "未知用户";

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeId(id)
                    .setUserId(userInfo.getUserId().toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 调用服务取消发布课程
            Courses course = courseManageService.unpublishCourse(id, userId, userName);

            return ApiResponse.success("取消发布成功", true);
        } catch (Exception e) {
            log.error("取消发布课程失败", e);
            return ApiResponse.error(500, "取消发布课程失败：" + e.getMessage());
        }
    }

    /**
     * 获取课程学习人员列表
     *
     * @param id      课程ID
     * @param request 查询条件
     * @return 学习人员列表
     */
    @PostMapping("/learners/{id}")
    public ApiResponse<CourseLearnerResponse> getCourseLearners(@PathVariable Long id, @RequestBody CourseLearnerRequest request) {
        log.info("获取课程学习人员列表请求，课程ID：{}，请求参数：{}", id, request);

        // 参数校验
        if (id == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        // 校验状态参数
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            List<String> validStatus = Arrays.asList("all", "completed", "learning");
            if (!validStatus.contains(request.getStatus())) {
                return ApiResponse.error(400, "完成状态无效，有效值为：all, completed, learning");
            }
        }

        // 校验分页参数
        if (request.getPageNum() != null && request.getPageNum() < 1) {
            return ApiResponse.error(400, "页码必须大于等于1");
        }

        if (request.getPageSize() != null && request.getPageSize() < 1) {
            return ApiResponse.error(400, "每页条数必须大于等于1");
        }

        try {
            // 调用服务获取课程学习人员列表
            CourseLearnerResponse response = courseUserService.getCourseLearners(id, request);
            return ApiResponse.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取课程学习人员列表失败", e);
            return ApiResponse.error(500, "获取课程学习人员列表失败：" + e.getMessage());
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
        log.info("批量更新课程创建人请求入参:{}", Json.toJson(request));

        // 参数校验
        if (request.getCourseIds() == null || request.getCourseIds().isEmpty()) {
            return ApiResponse.error(400, "课程ID列表不能为空");
        }
        if (request.getNewCreatorId() == null) {
            return ApiResponse.error(400, "新的创建人ID不能为空");
        }

        try {
            // 获取当前用户信息
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                return ApiResponse.error(401, "未登录或登录已过期");
            }
            UserInfoResponse currentUser = userInfoService.getUserInfo(userId);
            if (currentUser == null) {
                return ApiResponse.error(400, "当前用户信息异常");
            }

            commonRangeInterface.checkUserHasRightsIfNotThrowException(new CommonRangeQueryRequest()
                    .setType(COURSE)
                    .setTypeIds(request.getCourseIds())
                    .setUserId(userId.toString())
                    .setModelType(RangeModelTypeEnums.EDITORS.getCode()));
            // 获取新创建人信息
            UserInfoResponse newCreator = userInfoService.getUserInfo(request.getNewCreatorId());
            if (newCreator == null) {
                return ApiResponse.error(400, "新的创建人不存在");
            }


            // 调用服务批量更新课程创建人
            boolean result = courseManageService.batchUpdateCreator(
                    request.getCourseIds(),
                    request.getNewCreatorId(),
                    newCreator.getNickname(),
                    userId,
                    currentUser.getNickname()
            );

            return ApiResponse.success("批量更新创建人成功", result);
        } catch (Exception e) {
            log.error("批量更新课程创建人失败", e);
            return ApiResponse.error(500, "批量更新课程创建人失败：" + e.getMessage());
        }
    }
}
