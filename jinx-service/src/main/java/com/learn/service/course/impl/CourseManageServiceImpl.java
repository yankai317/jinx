package com.learn.service.course.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.dto.course.CourseBatchDeleteResponse;
import com.learn.dto.course.CourseCreateRequest;
import com.learn.dto.course.CourseUpdateRequest;
import com.learn.dto.course.sub.TargetDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import com.learn.dto.file.FileDecryptUrlResult;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.FileService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.course.CourseManageService;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeUpdateRequest;
import com.learn.service.dto.CommonRangeUpdateResponse;
import com.learn.service.impl.CommonRangeInterfaceImpl;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.learn.common.enums.RangeModelTypeEnums.EDITORS;

/**
 * 课程管理服务实现类
 */
@Service
@Slf4j
public class CourseManageServiceImpl implements CourseManageService {
    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterfaceImpl commonRangeInterface;

    @Autowired
    private FileService fileService;

    @Autowired
    private RangeSetService rangeSetService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Courses updateCourse(CourseUpdateRequest request) {
        // 参数校验
        validateUpdateCourseRequest(request);

        // 查询课程是否存在
        Courses existingCourse = coursesMapper.selectById(request.getId());
        if (existingCourse == null) {
            throw new CommonException("课程不存在");
        }

        // 更新课程基本信息
        updateCourseBasicInfo(existingCourse, request);

        // 更新数据库
        coursesMapper.updateById(existingCourse);

        // 更新课程可见范围（如果有变更）
        request.setOperatorId(request.getUpdaterId());
        request.setOperatorName(request.getUpdaterName());
        rangeSetService.updateVisibilityRange(BizType.COURSE, existingCourse.getId(), request);

        // 更新课程协同管理（如果有变更）
        rangeSetService.updateCollaborators(BizType.COURSE, existingCourse.getId(), request);

        // 处理系列课程的附件关联
        if ("series".equals(existingCourse.getType()) && request.getAppendixFiles() != null) {
            updateSeriesCourseAppendixRelations(existingCourse.getId(), request.getAppendixFiles(), request.getUpdaterId(), request.getUpdaterName());
        }

        return existingCourse;
    }

    /**
     * 更新系列课程的附件关联
     *
     * @param courseId      课程ID
     * @param appendixFiles 附件文件列表
     * @param operatorId    操作人ID
     * @param operatorName  操作人名称
     */
    private void updateSeriesCourseAppendixRelations(Long courseId, List<com.learn.dto.course.sub.SeriesCourseFile> appendixFiles, Long operatorId, String operatorName) {
        // 先删除原有的关联
        LambdaQueryWrapper<ContentRelation> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ContentRelation::getBizId, courseId)
                .eq(ContentRelation::getBizType, BizType.COURSE)
                .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                .eq(ContentRelation::getIsDel, 0);

        List<ContentRelation> existingRelations = contentRelationMapper.selectList(queryWrapper);

        // 逻辑删除原有关联
        if (!existingRelations.isEmpty()) {
            for (ContentRelation relation : existingRelations) {
                relation.setIsDel(1);
                relation.setUpdaterId(operatorId);
                relation.setUpdaterName(operatorName);
                relation.setGmtModified(new Date());
                contentRelationMapper.updateById(relation);
            }
        }

        // 添加新的关联
        if (appendixFiles != null && !appendixFiles.isEmpty()) {
            saveSeriesCourseAppendixRelations(courseId, appendixFiles, operatorId, operatorName);
        }
    }

    /**
     * 校验更新课程请求参数
     *
     * @param request 更新课程请求
     */
    private void validateUpdateCourseRequest(CourseUpdateRequest request) {
        // 校验必填参数
        if (request.getId() == null) {
            throw new CommonException("课程ID不能为空");
        }

        // 校验状态
        if (StringUtils.hasText(request.getStatus())) {
            List<String> validStatus = Arrays.asList("draft", "published");
            if (!validStatus.contains(request.getStatus())) {
                throw new CommonException("课程状态无效，有效值为：draft, published");
            }
        }

        // 校验可见范围
        if (request.getVisibility() != null) {
            VisibilityDTO visibility = request.getVisibility();
            if (!StringUtils.hasText(visibility.getType())) {
                throw new CommonException("可见范围类型不能为空");
            }

            List<String> validVisibilityTypes = Arrays.asList("ALL", "PART");
            if (!validVisibilityTypes.contains(visibility.getType())) {
                throw new CommonException("可见范围类型无效，有效值为：ALL, PART");
            }

            if ("PART".equals(visibility.getType()) &&
                    (visibility.getTargets() == null || visibility.getTargets().isEmpty())) {
                throw new CommonException("部分可见时必须指定可见目标");
            }

            if ("PART".equals(visibility.getType())) {
                checkPartVisibility(visibility);
            }
        }
    }

    /**
     * 更新课程基本信息
     *
     * @param existingCourse 现有课程实体
     * @param request        更新课程请求
     */
    private void updateCourseBasicInfo(Courses existingCourse, CourseUpdateRequest request) {
        // 更新课程名称
        if (StringUtils.hasText(request.getTitle())) {
            existingCourse.setTitle(request.getTitle());
        }

        // 更新封面图
        if (StringUtils.hasText(request.getCoverImage())) {
            // 检查是否是urlKey，如果是则解析为URL
            if (request.getCoverImage().startsWith("KEY_")) {
                FileDecryptUrlResult urlResult = fileService.getUrl(request.getCoverImage());
                if (urlResult != null && StringUtils.hasText(urlResult.getUrl())) {
                    existingCourse.setCoverImage(urlResult.getUrl());
                } else {
                    existingCourse.setCoverImage(request.getCoverImage());
                }
            } else {
                existingCourse.setCoverImage(request.getCoverImage());
            }
        }

        // 更新讲师ID
        if (request.getInstructorId() != null) {
            existingCourse.setInstructorId(request.getInstructorId());
        }
        if (request.getGuestName() != null) {
            existingCourse.setGuestName(request.getGuestName());
        }

        // 更新课程简介
        if (StringUtils.hasText(request.getDescription())) {
            existingCourse.setDescription(request.getDescription());
        }

        // 更新学分
        if (request.getCredit() != null) {
            existingCourse.setCredit(request.getCredit());
        }

        // 更新分类IDs
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            existingCourse.setCategoryIds(request.getCategoryIds());
        }

        // 更新状态
        if (StringUtils.hasText(request.getStatus())) {
            // 如果从草稿变为已发布，设置发布时间
            if ("published".equals(request.getStatus()) && "draft".equals(existingCourse.getStatus())) {
                existingCourse.setPublishTime(new Date());
            }
            existingCourse.setStatus(request.getStatus());
        }

        // 更新是否允许评论
        if (request.getAllowComments() != null) {
            existingCourse.setAllowComments(request.getAllowComments() ? 1 : 0);
        }

        // 更新是否置顶
        if (request.getIsTop() != null) {
            existingCourse.setIsTop(request.getIsTop() ? 1 : 0);
        }
        
        // 更新是否可引用
        if (request.getIfIsCitable() != null) {
            existingCourse.setIfIsCitable(request.getIfIsCitable());
        }

        // 更新文章内容（仅文章类型）
        if ("article".equals(existingCourse.getType()) && StringUtils.hasText(request.getArticle())) {
            existingCourse.setArticle(request.getArticle());
        }

        // 更新附件类型和路径
        if (StringUtils.hasText(request.getAppendixType())) {
            existingCourse.setAppendixType(request.getAppendixType());
        }

        if (StringUtils.hasText(request.getAppendixPath())) {
            // 检查是否是urlKey，如果是则解析为URL
            if (request.getAppendixPath().startsWith("KEY_")) {
                FileDecryptUrlResult urlResult = fileService.getUrl(request.getAppendixPath());
                if (urlResult != null && StringUtils.hasText(urlResult.getUrl())) {
                    existingCourse.setAppendixPath(urlResult.getUrl());
                } else {
                    existingCourse.setAppendixPath(request.getAppendixPath());
                }
            } else {
                existingCourse.setAppendixPath(request.getAppendixPath());
            }
        }

        // 更新更新人信息
        existingCourse.setUpdaterId(request.getUpdaterId());
        existingCourse.setUpdaterName(request.getUpdaterName());
        existingCourse.setGmtModified(new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Courses createCourse(CourseCreateRequest request) {
        // 参数校验
        validateCreateCourseRequest(request);

        // 构建课程实体
        Courses course = buildCourseEntity(request);

        // 插入数据库
        coursesMapper.insert(course);

        // 设置可见范围
        request.setOperatorId(request.getCreatorId());
        request.setOperatorName(request.getCreatorName());
        rangeSetService.setVisibilityRange(BizType.COURSE, course.getId(), request);

        // 设置协同管理
        rangeSetService.setCollaborators(BizType.COURSE, course.getId(), request);

        // 处理系列课程的附件关联
        if ("series".equals(course.getType()) && request.getAppendixFiles() != null && !request.getAppendixFiles().isEmpty()) {
            saveSeriesCourseAppendixRelations(course.getId(), request.getAppendixFiles(), request.getCreatorId(), request.getCreatorName());
        }

        return course;
    }

    /**
     * 保存系列课程的附件关联
     *
     * @param courseId 课程ID
     * @param appendixFiles 附件文件列表
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     */
    /**
     * 保存系列课程的附件关联
     *
     * @param courseId      课程ID
     * @param appendixFiles 附件文件列表
     * @param operatorId    操作人ID
     * @param operatorName  操作人名称
     */
    private void saveSeriesCourseAppendixRelations(Long courseId, List<com.learn.dto.course.sub.SeriesCourseFile> appendixFiles, Long operatorId, String operatorName) {
        if (appendixFiles == null || appendixFiles.isEmpty()) {
            return;
        }

        Date now = new Date();
        for (com.learn.dto.course.sub.SeriesCourseFile file : appendixFiles) {
            // 为每个附件生成一个随机ID
            // 生成较小但不容易重复的ID：使用时间戳后6位 + 5位随机数
            Long appendixId = file.getId() != null ? file.getId() :
                    Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(4) + (10000 + new Random().nextInt(90000)));

            // 创建内容关联实体
            ContentRelation relation = new ContentRelation();
            relation.setBizId(courseId);
            relation.setBizType(BizType.COURSE);
            relation.setContentType(BizType.APPENDIX_FILE);
            relation.setContentId(appendixId);
            relation.setContentUrl(file.getUrl());

            // 将文件名存储为map格式的json
            Map<String, Object> attributesMap = new HashMap<>();
            String originalFilename = file.getName();
            String title = originalFilename;
            if (originalFilename != null && originalFilename.contains(".")) {
                String[] names = originalFilename.split("\\.");
                title = names[0];
            }

            attributesMap.put(AttributeKey.TITLE, title);
            attributesMap.put(AttributeKey.TYPE, file.getType());
            attributesMap.put(AttributeKey.BIZ_TYPE, file.getBizType());
            attributesMap.put(AttributeKey.APPENDIX_PATH, file.getUrl());
            attributesMap.put(AttributeKey.DURATION, file.getDuration());
            relation.setAttributes(Json.toJson(attributesMap));

            relation.setGmtCreate(now);
            relation.setGmtModified(now);
            relation.setCreatorId(operatorId);
            relation.setCreatorName(operatorName);
            relation.setUpdaterId(operatorId);
            relation.setUpdaterName(operatorName);
            relation.setIsDel(0);

            // 插入数据库
            contentRelationMapper.insert(relation);
        }
    }

    /**
     * 校验创建课程请求参数
     *
     * @param request 创建课程请求
     */
    private void validateCreateCourseRequest(CourseCreateRequest request) {
        // 校验必填参数
        if (!StringUtils.hasText(request.getTitle())) {
            throw new CommonException("课程名称不能为空");
        }

        if (!StringUtils.hasText(request.getType())) {
            throw new CommonException("课程类型不能为空");
        }

        // 校验课程类型
        List<String> validTypes = Arrays.asList("video", "document", "series", "article");
        if (!validTypes.contains(request.getType())) {
            throw new CommonException("课程类型无效，有效值为：video, document, series, article");
        }

        // 校验讲师ID（仅视频/系列课）
//        if (("video".equals(request.getType()) || "series".equals(request.getType()))
//                && request.getInstructorId() == null) {
//            throw new CommonException("视频或系列课程必须指定讲师ID");
//        }

        // 校验附件类型和路径（非文章类型必填）
        if (!"series".equals(request.getType())) {
            if (!StringUtils.hasText(request.getAppendixType())) {
                throw new CommonException("必须指定附件类型");
            }
            if (!StringUtils.hasText(request.getAppendixPath())) {
                throw new CommonException("必须指定附件路径");
            }
        }

        // 校验文章内容（仅文章类型）
        if ("article".equals(request.getType()) && !StringUtils.hasText(request.getAppendixPath())) {
            throw new CommonException("文章类型课程必须提供文章内容");
        }

        // 校验状态
        if (StringUtils.hasText(request.getStatus())) {
            List<String> validStatus = Arrays.asList("draft", "published");
            if (!validStatus.contains(request.getStatus())) {
                throw new CommonException("课程状态无效，有效值为：draft, published");
            }
        }

        // 校验可见范围
        if (request.getVisibility() != null) {
            VisibilityDTO visibility = request.getVisibility();
            if (!StringUtils.hasText(visibility.getType())) {
                throw new CommonException("可见范围类型不能为空");
            }

            List<String> validVisibilityTypes = Arrays.asList("ALL", "PART");
            if (!validVisibilityTypes.contains(visibility.getType())) {
                throw new CommonException("可见范围类型无效，有效值为：ALL, PART");
            }

            if ("PART".equals(visibility.getType()) &&
                    (visibility.getTargets() == null || visibility.getTargets().isEmpty())) {
                throw new CommonException("部分可见时必须指定可见目标");
            }

            if ("PART".equals(visibility.getType())) {
                checkPartVisibility(visibility);
            }
        }
    }

    private void checkPartVisibility(VisibilityDTO visibility) {
        //过滤为空的
        visibility.setTargets(visibility.getTargets().stream()
                .filter(it -> CollectionUtils.isNotEmpty(it.getIds()))
                .collect(Collectors.toList()));
        for (TargetDTO target : visibility.getTargets()) {
            if (!StringUtils.hasText(target.getType())) {
                throw new CommonException("可见目标类型不能为空");
            }

            List<String> validTargetTypes = Arrays.asList("department", "role", "user");
            if (!validTargetTypes.contains(target.getType())) {
                throw new CommonException("可见目标类型无效，有效值为：department, role, user");
            }

            if (target.getIds() == null || target.getIds().isEmpty()) {
                throw new CommonException("可见目标ID列表不能为空");
            }
        }
    }

    /**
     * 构建课程实体
     *
     * @param request 创建课程请求
     * @return 课程实体
     */
    private Courses buildCourseEntity(CourseCreateRequest request) {
        Courses course = new Courses();
        course.setTitle(request.getTitle());
        course.setType(request.getType());

        // 设置封面图
        if (StringUtils.hasText(request.getCoverImage())) {
            course.setCoverImage(request.getCoverImage());
        }

        // 设置业务字段
        if ("series".equals(request.getType())) {
            course.setAppendixPath(request.getAppendixPath());
            course.setInstructorId(request.getInstructorId());
            course.setGuestName(request.getGuestName());
        }
        if ("video".equals(request.getType())) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("duration", request.getDuration());
            course.setAttributes(Json.toJson(attributes));
            course.setInstructorId(request.getInstructorId());
            course.setGuestName(request.getGuestName());
        }

        // 设置课程简介
        if (StringUtils.hasText(request.getDescription())) {
            course.setDescription(request.getDescription());
        }

        // 设置学分，默认为1
        course.setCredit(request.getCredit() != null ? request.getCredit() : 1);

        // 设置分类IDs
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            course.setCategoryIds(request.getCategoryIds());
        }

        // 设置状态，默认为draft
        course.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");

        // 设置是否允许评论，默认为true
        course.setAllowComments(request.getAllowComments() != null ? (request.getAllowComments() ? 1 : 0) : 1);

        // 设置是否置顶，默认为false
        course.setIsTop(request.getIsTop() != null ? (request.getIsTop() ? 1 : 0) : 0);
        
        // 设置是否可引用，默认为false
        course.setIfIsCitable(request.getIfIsCitable() );

        course.setAppendixType(request.getAppendixType());
        course.setAppendixPath(request.getAppendixPath());

        // 设置初始值
        course.setViewCount(0);
        course.setCompleteCount(0);
        course.setIsDel(0);

        // 设置创建人信息
        Date now = new Date();
        course.setGmtCreate(now);
        course.setGmtModified(now);
        course.setCreatorId(request.getCreatorId());
        course.setCreatorName(request.getCreatorName());
        course.setUpdaterId(request.getCreatorId());
        course.setUpdaterName(request.getCreatorName());

        // 如果状态为published，设置发布时间
        if ("published".equals(course.getStatus())) {
            course.setPublishTime(now);
        }

        return course;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(Long id, Long updaterId, String updaterName) {
        log.info("删除课程，课程ID：{}，操作人ID：{}", id, updaterId);

        // 参数校验
        if (id == null) {
            throw new CommonException("课程ID不能为空");
        }

        // 查询课程是否存在
        Courses existingCourse = coursesMapper.selectById(id);
        if (existingCourse == null) {
            throw new CommonException("课程不存在");
        }

        // 检查课程是否已被删除
        if (existingCourse.getIsDel() != null && existingCourse.getIsDel() == 1) {
            return true; // 课程已被删除，直接返回成功
        }

        // 检查课程是否被培训引用
        LambdaQueryWrapper<ContentRelation> trainQueryWrapper = Wrappers.lambdaQuery();
        trainQueryWrapper.eq(ContentRelation::getContentId, id)
                .eq(ContentRelation::getContentType, "COURSE")
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getIsDel, 0);
        Long trainCount = contentRelationMapper.selectCount(trainQueryWrapper);
        if (trainCount > 0) {
            throw new CommonException("该课程已被培训引用，无法删除");
        }

        // 检查课程是否被学习地图引用
        LambdaQueryWrapper<ContentRelation> mapQueryWrapper = Wrappers.lambdaQuery();
        mapQueryWrapper.eq(ContentRelation::getContentId, id)
                .eq(ContentRelation::getContentType, "COURSE")
                .eq(ContentRelation::getBizType, BizType.MAP_STAGE)
                .eq(ContentRelation::getIsDel, 0);

        Long mapCount = contentRelationMapper.selectCount(mapQueryWrapper);
        if (mapCount > 0) {
            throw new CommonException("该课程已被学习地图引用，无法删除");
        }

        // 逻辑删除课程
        existingCourse.setIsDel(1);
        existingCourse.setUpdaterId(updaterId);
        existingCourse.setUpdaterName(updaterName);
        existingCourse.setGmtModified(new Date());

        int result = coursesMapper.updateById(existingCourse);

        if (result <= 0) {
            throw new CommonException("删除课程失败");
        }

        // 删除课程相关的范围配置（可见范围）
        CommonRangeDeleteRequest visibilityDeleteRequest = new CommonRangeDeleteRequest();
        visibilityDeleteRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        visibilityDeleteRequest.setType(BizType.COURSE);
        visibilityDeleteRequest.setTypeId(id);
        commonRangeInterface.deleteRangeByBusinessId(visibilityDeleteRequest);

        // 删除课程相关的范围配置（编辑者）
        CommonRangeDeleteRequest editorDeleteRequest = new CommonRangeDeleteRequest();
        editorDeleteRequest.setModelType(RangeModelTypeEnums.EDITORS.getCode());
        editorDeleteRequest.setType(BizType.COURSE);
        editorDeleteRequest.setTypeId(id);

        commonRangeInterface.deleteRangeByBusinessId(editorDeleteRequest);

        // 删除课程相关的范围配置（协作者）
        CommonRangeDeleteRequest collaboratorDeleteRequest = new CommonRangeDeleteRequest();
        collaboratorDeleteRequest.setModelType(RangeModelTypeEnums.COLLABORATORS.getCode());
        collaboratorDeleteRequest.setType(BizType.COURSE);
        collaboratorDeleteRequest.setTypeId(id);

        commonRangeInterface.deleteRangeByBusinessId(collaboratorDeleteRequest);

        // 如果是系列课程，删除附件关联
        if ("series".equals(existingCourse.getType())) {
            deleteSeriesCourseAppendixRelations(id, updaterId, updaterName);
        }

        log.info("删除课程成功，课程ID：{}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseBatchDeleteResponse batchDeleteCourse(List<Long> ids, Long updaterId, String updaterName) {
        log.info("批量删除课程，课程ID列表：{}，操作人ID：{}", ids, updaterId);

        // 参数校验
        if (CollectionUtils.isEmpty(ids)) {
            throw new CommonException("课程ID列表不能为空");
        }

        // 初始化返回结果
        CourseBatchDeleteResponse response = new CourseBatchDeleteResponse();
        response.setSuccess(0);
        response.setFailed(0);
        response.setFailedIds(new ArrayList<>());

        // 1. 批量查询课程是否存在
        LambdaQueryWrapper<Courses> courseQueryWrapper = Wrappers.lambdaQuery();
        courseQueryWrapper.in(Courses::getId, ids);
        List<Courses> existingCourses = coursesMapper.selectList(courseQueryWrapper);

        // 检查是否所有课程都存在
        if (existingCourses.size() < ids.size()) {
            // 找出不存在的课程ID
            List<Long> existingIds = existingCourses.stream()
                    .map(Courses::getId)
                    .collect(Collectors.toList());

            List<Long> nonExistingIds = ids.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toList());

            // 记录不存在的课程为失败
            response.setFailed(nonExistingIds.size());
            response.setFailedIds(nonExistingIds);
            log.warn("批量删除课程失败，以下课程ID不存在：{}", nonExistingIds);

            // 如果所有课程都不存在，直接返回
            if (existingCourses.isEmpty()) {
                return response;
            }

            // 更新ids列表，只包含存在的课程
            ids = existingIds;
        }

        // 过滤掉已经被删除的课程
        List<Long> notDeletedIds = existingCourses.stream()
                .filter(course -> course.getIsDel() == null || course.getIsDel() == 0)
                .map(Courses::getId)
                .collect(Collectors.toList());

        // 记录已经被删除的课程为成功
        int alreadyDeletedCount = ids.size() - notDeletedIds.size();
        response.setSuccess(alreadyDeletedCount);

        // 如果所有课程都已经被删除，直接返回
        if (notDeletedIds.isEmpty()) {
            return response;
        }

        // 2. 批量查询课程是否被培训引用
        LambdaQueryWrapper<ContentRelation> trainQueryWrapper = Wrappers.lambdaQuery();
        trainQueryWrapper.in(ContentRelation::getContentId, notDeletedIds)
                .eq(ContentRelation::getContentType, "COURSE")
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> trainRelations = contentRelationMapper.selectList(trainQueryWrapper);
        
        // 3. 批量查询课程是否被学习地图引用
        LambdaQueryWrapper<ContentRelation> mapQueryWrapper = Wrappers.lambdaQuery();
        mapQueryWrapper.in(ContentRelation::getContentId, notDeletedIds)
                .eq(ContentRelation::getContentType, "COURSE")
                .eq(ContentRelation::getBizType, BizType.MAP_STAGE)
                .eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> mapRelations = contentRelationMapper.selectList(mapQueryWrapper);
        
        // 收集被引用的课程ID
        Set<Long> referencedByTrainIds = trainRelations.stream()
                .map(ContentRelation::getContentId)
                .collect(Collectors.toSet());
        
        Set<Long> referencedByMapIds = mapRelations.stream()
                .map(ContentRelation::getContentId)
                .collect(Collectors.toSet());
        
        // 合并所有被引用的ID
        Set<Long> allReferencedIds = new HashSet<>();
        allReferencedIds.addAll(referencedByTrainIds);
        allReferencedIds.addAll(referencedByMapIds);
        
        // 如果有课程被引用，直接报错
        if (!allReferencedIds.isEmpty()) {
            List<Long> referencedIds = new ArrayList<>(allReferencedIds);
            
            // 构建错误信息
            StringBuilder errorMsg = new StringBuilder("以下课程ID被引用，无法删除：");
            errorMsg.append(String.join(", ", referencedIds.stream().map(String::valueOf).collect(Collectors.toList())));
            
            // 被培训引用的课程
            if (!referencedByTrainIds.isEmpty()) {
                errorMsg.append("。被培训引用的课程ID：");
                errorMsg.append(String.join(", ", referencedByTrainIds.stream().map(String::valueOf).collect(Collectors.toList())));
            }
            
            // 被学习地图引用的课程
            if (!referencedByMapIds.isEmpty()) {
                errorMsg.append("。被学习地图引用的课程ID：");
                errorMsg.append(String.join(", ", referencedByMapIds.stream().map(String::valueOf).collect(Collectors.toList())));
            }
            
            // 记录被引用的课程为失败
            response.setFailed(response.getFailed() + referencedIds.size());
            response.getFailedIds().addAll(referencedIds);
            
            throw new CommonException(errorMsg.toString());
        }
        
        // 4. 批量更新课程的逻辑删除状态
        List<Courses> coursesToUpdate = new ArrayList<>();
        Date now = new Date();
        
        for (Courses course : existingCourses) {
            // 跳过已删除的课程
            if (course.getIsDel() != null && course.getIsDel() == 1) {
                continue;
            }
            
            course.setIsDel(1);
            course.setUpdaterId(updaterId);
            course.setUpdaterName(updaterName);
            course.setGmtModified(now);
            coursesToUpdate.add(course);
        }
        
        // 批量更新
        if (!coursesToUpdate.isEmpty()) {
            // 使用批量逻辑删除方法
            List<Long> idsToDelete = coursesToUpdate.stream()
                    .map(Courses::getId)
                    .collect(Collectors.toList());
            
            int affectedRows = coursesMapper.batchLogicalDelete(idsToDelete, updaterId, updaterName, new Date());
        }
        
        log.info("批量删除课程完成，成功：{}，失败：{}", response.getSuccess(), response.getFailed());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Courses publishCourse(Long id, Long userId, String userName) {
        log.info("发布课程，课程ID：{}，操作人ID：{}", id, userId);

        // 参数校验
        if (id == null) {
            throw new CommonException("课程ID不能为空");
        }

        if (userId == null) {
            throw new CommonException("操作用户ID不能为空");
        }

        // 查询课程是否存在
        Courses existingCourse = coursesMapper.selectById(id);
        if (existingCourse == null) {
            throw new CommonException("课程不存在");
        }

        // 检查课程是否已被删除
        if (existingCourse.getIsDel() != null && existingCourse.getIsDel() == 1) {
            throw new CommonException("课程已被删除，无法发布");
        }

        // 检查课程状态是否为草稿
        if (!"draft".equals(existingCourse.getStatus())) {
            throw new CommonException("只有草稿状态的课程可以发布");
        }

        // 校验操作权限
        checkPublishPermission(id, userId);

        // 更新课程状态为已发布
        existingCourse.setStatus("published");
        existingCourse.setPublishTime(new Date());
        existingCourse.setUpdaterId(userId);
        existingCourse.setUpdaterName(userName);
        existingCourse.setGmtModified(new Date());

        // 更新数据库
        int result = coursesMapper.updateById(existingCourse);

        if (result <= 0) {
            throw new CommonException("发布课程失败");
        }

        log.info("发布课程成功，课程ID：{}", id);
        return existingCourse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Courses unpublishCourse(Long id, Long userId, String userName) {
        log.info("取消发布课程，课程ID：{}，操作人ID：{}", id, userId);

        // 参数校验
        if (id == null) {
            throw new CommonException("课程ID不能为空");
        }

        if (userId == null) {
            throw new CommonException("操作用户ID不能为空");
        }

        // 查询课程是否存在
        Courses existingCourse = coursesMapper.selectById(id);
        if (existingCourse == null) {
            throw new CommonException("课程不存在");
        }

        // 检查课程是否已被删除
        if (existingCourse.getIsDel() != null && existingCourse.getIsDel() == 1) {
            throw new CommonException("课程已被删除，无法取消发布");
        }

        // 检查课程状态是否为已发布
        if (!"published".equals(existingCourse.getStatus())) {
            throw new CommonException("只有已发布状态的课程可以取消发布");
        }

        // 校验操作权限
        checkPublishPermission(id, userId);

        // 更新课程状态为草稿
        existingCourse.setStatus("draft");
        existingCourse.setUpdaterId(userId);
        existingCourse.setUpdaterName(userName);
        existingCourse.setGmtModified(new Date());

        // 更新数据库
        int result = coursesMapper.updateById(existingCourse);

        if (result <= 0) {
            throw new CommonException("取消发布课程失败");
        }

        log.info("取消发布课程成功，课程ID：{}", id);
        return existingCourse;
    }

    /**
     * 删除系列课程的附件关联
     *
     * @param courseId     课程ID
     * @param operatorId   操作人ID
     * @param operatorName 操作人名称
     */
    private void deleteSeriesCourseAppendixRelations(Long courseId, Long operatorId, String operatorName) {
        // 查询系列课程的附件关联
        LambdaQueryWrapper<ContentRelation> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ContentRelation::getBizId, courseId)
                .eq(ContentRelation::getBizType, BizType.COURSE)
                .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                .eq(ContentRelation::getIsDel, 0);

        List<ContentRelation> relations = contentRelationMapper.selectList(queryWrapper);

        // 逻辑删除附件关联
        if (!relations.isEmpty()) {
            Date now = new Date();
            for (ContentRelation relation : relations) {
                relation.setIsDel(1);
                relation.setUpdaterId(operatorId);
                relation.setUpdaterName(operatorName);
                relation.setGmtModified(now);
                contentRelationMapper.updateById(relation);
            }
            log.info("删除系列课程附件关联成功，课程ID：{}，附件数量：{}", courseId, relations.size());
        }
    }

    /**
     * 校验发布权限
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     */
    private void checkPublishPermission(Long courseId, Long userId) {
        // 构建权限校验请求
        CommonRangeQueryRequest request = new CommonRangeQueryRequest();
        request.setModelType(RangeModelTypeEnums.EDITORS.getCode()); // 功能模块类型：编辑者
        request.setType(BizType.COURSE); // 业务模块类型：课程
        request.setTypeId(courseId); // 业务模块ID：课程ID
        request.setUserId(userId.toString()); // 用户ID

        // 调用通用范围接口校验权限
        commonRangeInterface.checkUserHasRightsIfNotThrowException(request);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCreator(List<Long> courseIds, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName) {
        log.info("批量更新课程创建人，课程ID列表：{}，新创建人ID：{}，操作人：{}", courseIds, newCreatorId, operatorName);
        if (CollectionUtils.isEmpty(courseIds)) {
            return false;
        }

        // 执行批量更新
        coursesMapper.batchUpdateCreator(courseIds, newCreatorId, newCreatorName, operatorId, operatorName);


        log.info("批量更新课程创建人完成");
        return true;
    }

}
