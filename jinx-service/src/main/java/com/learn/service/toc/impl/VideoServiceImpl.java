package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.exception.CommonException;
import com.learn.common.util.FileUrlUtil;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.video.VideoPlayRequest;
import com.learn.dto.toc.video.VideoPlayResponse;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.toc.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 视频服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private final CoursesMapper coursesMapper;
    private final UserLearningTaskMapper userLearningTaskMapper;

    /**
     * 获取视频播放信息
     *
     * @param request 视频播放请求
     * @param userId  用户ID
     * @return 视频播放信息
     */
    @Override
    public ApiResponse<VideoPlayResponse> getVideoPlayInfo(VideoPlayRequest request, Long userId) {
        // 参数校验
        if (request.getCourseId() == null) {
            return ApiResponse.error("课程ID不能为空");
        }

        // 查询课程信息
        Courses course = coursesMapper.selectById(request.getCourseId());
        if (course == null || course.getIsDel() == 1) {
            return ApiResponse.error("课程不存在");
        }

        // 检查课程类型是否为视频
        if (!"video".equals(course.getType())) {
            return ApiResponse.error("该课程不是视频类型");
        }

        // 查询用户学习记录
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, course.getId())
                .eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0);
        UserLearningTask userLearningTask = userLearningTaskMapper.selectOne(taskWrapper);

        // 构建响应对象
        VideoPlayResponse response = buildVideoPlayResponse(course, userLearningTask);

        return ApiResponse.success(response);
    }

    /**
     * 构建视频播放响应对象
     *
     * @param course           课程信息
     * @param userLearningTask 用户学习任务
     * @return 视频播放响应对象
     */
    private VideoPlayResponse buildVideoPlayResponse(Courses course, UserLearningTask userLearningTask) {
        VideoPlayResponse response = new VideoPlayResponse();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        
        // 处理视频URL，如果是加密路径则解密
        String videoUrl = course.getAppendixPath();
        response.setVideoUrl(videoUrl);
        
        // 设置封面图
        response.setCoverImage(course.getCoverImage());
        
        // 从attributes中获取视频时长，如果没有则默认为0
        Integer duration = 0;
        if (course.getAttributes() != null && course.getAttributes().contains("duration")) {
            try {
                String durationStr = course.getAttributes().replaceAll(".*\"duration\":(\\d+).*", "$1");
                duration = Integer.parseInt(durationStr);
            } catch (Exception e) {
                log.error("解析视频时长失败", e);
            }
        }
        response.setDuration(duration);
        
        // 设置播放进度和位置
        int currentTime = 0;
        String lastPlayTime = "";
        if (userLearningTask != null) {
            // 从attributes中获取当前播放位置（秒）
            if (userLearningTask.getAttributes() != null && userLearningTask.getAttributes().contains("currentTime")) {
                try {
                    String currentTimeStr = userLearningTask.getAttributes().replaceAll(".*\"currentTime\":(\\d+).*", "$1");
                    currentTime = Integer.parseInt(currentTimeStr);
                } catch (Exception e) {
                    log.error("解析当前播放位置失败", e);
                }
            }
            
            // 格式化上次播放时间
            if (userLearningTask.getLastStudyTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastPlayTime = sdf.format(userLearningTask.getLastStudyTime());
            }
        }
        
        // 设置当前播放位置（秒）
        response.setLastPosition(currentTime);
        
        // 设置播放进度（秒）
        // 注意：VideoPlayResponse中的progress应该是秒数而不是百分比
        if (userLearningTask != null && userLearningTask.getProgress() != null && userLearningTask.getProgress() > 0) {
            // 如果用户学习任务中已有进度记录（秒），则使用该进度
            response.setProgress(userLearningTask.getProgress());
        } else {
            // 如果没有进度记录，则使用当前播放位置作为进度
            response.setProgress(currentTime);
        }
        
        // 查询上一个和下一个课程ID
        // 这里简单实现，实际可能需要根据课程分类或者培训计划等信息来确定
        Long prevCourseId = null;
        Long nextCourseId = null;
        
        // 如果请求中包含培训ID或地图ID，可以查询相关的上下课程
        if (userLearningTask != null && userLearningTask.getParentId() != null) {
            // 查询同一父节点下的其他课程
            LambdaQueryWrapper<UserLearningTask> siblingWrapper = new LambdaQueryWrapper<>();
            siblingWrapper.eq(UserLearningTask::getUserId, userLearningTask.getUserId())
                    .eq(UserLearningTask::getParentId, userLearningTask.getParentId())
                    .eq(UserLearningTask::getBizType, BizType.COURSE)
                    .eq(UserLearningTask::getIsDel, 0)
                    .orderByAsc(UserLearningTask::getId);
            
            List<UserLearningTask> siblings = userLearningTaskMapper.selectList(siblingWrapper);
            
            if (siblings != null && !siblings.isEmpty()) {
                for (int i = 0; i < siblings.size(); i++) {
                    if (siblings.get(i).getBizId().equals(course.getId())) {
                        // 找到当前课程，获取前后课程
                        if (i > 0) {
                            prevCourseId = siblings.get(i - 1).getBizId();
                        }
                        if (i < siblings.size() - 1) {
                            nextCourseId = siblings.get(i + 1).getBizId();
                        }
                        break;
                    }
                }
            }
        }
        
        response.setPrevCourseId(prevCourseId);
        response.setNextCourseId(nextCourseId);
        
        return response;
    }
}
