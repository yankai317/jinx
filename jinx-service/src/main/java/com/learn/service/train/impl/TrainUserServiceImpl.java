package com.learn.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.BizType;
import com.learn.dto.train.TrainLearnerDetailDTO;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.train.TrainUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训用户服务实现类
 */
@Service
@Slf4j
public class TrainUserServiceImpl implements TrainUserService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private UserMapper userMapper;

    
    /**
     * 获取培训学员学习详情
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 学习详情
     */
    @Override
    public TrainLearnerDetailDTO getTrainLearnerDetail(Long trainId, Long userId) {
        log.info("获取培训学员学习详情，培训ID：{}，用户ID：{}", trainId, userId);
        
        // 查询学习完成情况
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getBizId, trainId)
                .eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0);
        UserLearningTask task = userLearningTaskMapper.selectOne(taskWrapper);
        
        if (task == null) {
            log.error("未找到学员学习记录，培训ID：{}，用户ID：{}", trainId, userId);
            return null;
        }
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("未找到用户信息，用户ID：{}", userId);
            return null;
        }
        
        // 获取部门信息
        String department = "";
//        LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
//        duQueryWrapper.eq(DepartmentUser::getUserId, userId)
//                .eq(DepartmentUser::getIsDel, 0);
//        DepartmentUser departmentUser = departmentUserMapper.selectOne(duQueryWrapper);
//        if (departmentUser != null) {
//            department = departmentUser.getDepartmentName();
//        }
        
        // 构建学习详情DTO
        TrainLearnerDetailDTO detailDTO = new TrainLearnerDetailDTO();
        detailDTO.setUserId(userId);
        detailDTO.setUserName(user.getNickname());
        detailDTO.setDepartment(department);
        detailDTO.setSource(task.getSource());
        detailDTO.setStatus(task.getStatus());
        detailDTO.setStudyDuration(task.getStudyDuration());
        
        // 从扩展属性中获取必修任务完成情况
        Map<String, Object> attributes = parseAttributes(task.getAttributes());
        if (attributes != null) {
            Integer completedRequiredCourseCount = (Integer) attributes.get("completedRequiredCourseCount");
            Integer requiredTaskTotal = getRequiredTaskTotal(trainId);
            
            detailDTO.setRequiredTaskFinished(completedRequiredCourseCount != null ? completedRequiredCourseCount : 0);
            detailDTO.setRequiredTaskTotal(requiredTaskTotal);
        }
        
        // 格式化时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (task.getAssignTime() != null) {
            detailDTO.setAssignTime(dateFormat.format(task.getAssignTime()));
        }
        
        if (task.getDeadline() != null) {
            detailDTO.setDeadline(dateFormat.format(task.getDeadline()));
        }
        
        if (task.getCompletionTime() != null) {
            detailDTO.setCompletionTime(dateFormat.format(task.getCompletionTime()));
        }
        
        // 查询培训关联的课程学习记录
        List<TrainLearnerDetailDTO.RecordDTO> recordDTOList = getTrainCourseRecords(trainId, userId);
        detailDTO.setRecords(recordDTOList);
        
        return detailDTO;
    }
    
    /**
     * 发送学习提醒
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 是否发送成功
     */
    @Override
    public boolean sendLearningReminder(Long trainId, Long userId) {
        log.info("发送学习提醒，培训ID：{}，用户ID：{}", trainId, userId);
        
        // 查询学习完成情况
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getBizId, trainId)
                .eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0);
        UserLearningTask task = userLearningTaskMapper.selectOne(taskWrapper);
        
        if (task == null) {
            log.error("未找到学员学习记录，无法发送提醒，培训ID：{}，用户ID：{}", trainId, userId);
            return false;
        }
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("未找到用户信息，无法发送提醒，用户ID：{}", userId);
            return false;
        }
        
        // 实际项目中，这里应该调用消息发送服务，发送提醒消息
        // 例如：发送邮件、短信、站内信等
        // 这里仅记录日志，模拟发送成功
        log.info("已向用户 {} 发送学习提醒，培训ID：{}", user.getNickname(), trainId);
        
        return true;
    }
    
    /**
     * 获取培训关联的课程学习记录
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 课程学习记录列表
     */
    private List<TrainLearnerDetailDTO.RecordDTO> getTrainCourseRecords(Long trainId, Long userId) {
        List<TrainLearnerDetailDTO.RecordDTO> recordDTOList = new ArrayList<>();
        
        // 查询培训关联的课程
        LambdaQueryWrapper<ContentRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getBizId, trainId)
                .eq(ContentRelation::getIsDel, 0);
        List<ContentRelation> relations = contentRelationMapper.selectList(relationWrapper);
        
        if (relations == null || relations.isEmpty()) {
            return recordDTOList;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 遍历关联的课程
        for (ContentRelation relation : relations) {
            String contentType = relation.getContentType();
            Long contentId = relation.getContentId();
            
            // 查询用户对该课程的学习记录
            LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(UserLearningTask::getBizType, contentType)
                    .eq(UserLearningTask::getBizId, contentId)
                    .eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getParentType, BizType.TRAIN)
                    .eq(UserLearningTask::getParentId, trainId)
                    .eq(UserLearningTask::getIsDel, 0);
            UserLearningTask task = userLearningTaskMapper.selectOne(taskWrapper);
            
            // 如果没有学习记录，跳过
            if (task == null) {
                continue;
            }
            
            // 构建记录DTO
            TrainLearnerDetailDTO.RecordDTO recordDTO = new TrainLearnerDetailDTO.RecordDTO();
            recordDTO.setContentId(contentId);
            recordDTO.setContentType(contentType);
            recordDTO.setStatus(task.getStatus());
            recordDTO.setStudyDuration(task.getStudyDuration());
            recordDTO.setScore(task.getScore() != null ? task.getScore() : null);
            
            // todo 设置标题
//            recordDTO.setTitle(relation.getContentName() != null ? relation.getContentName() : "未知内容");
            
            // 格式化时间
            if (task.getLastStudyTime() != null) {
                recordDTO.setLastStudyTime(dateFormat.format(task.getLastStudyTime()));
            }
            
            if (task.getCompletionTime() != null) {
                recordDTO.setCompletionTime(dateFormat.format(task.getCompletionTime()));
            }
            
            recordDTOList.add(recordDTO);
        }
        
        return recordDTOList;
    }
    
    /**
     * 解析扩展属性
     *
     * @param attributesJson 扩展属性JSON字符串
     * @return 扩展属性Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAttributes(String attributesJson) {
        if (!StringUtils.hasText(attributesJson)) {
            return new HashMap<>();
        }
        
        try {
            // 使用Jackson或Gson等JSON库解析
            // 这里简化处理，实际项目中应该使用JSON库
            return new HashMap<>();
        } catch (Exception e) {
            log.error("解析扩展属性失败", e);
            return new HashMap<>();
        }
    }
    
    /**
     * 获取培训必修任务总数
     *
     * @param trainId 培训ID
     * @return 必修任务总数
     */
    private Integer getRequiredTaskTotal(Long trainId) {
        // 查询培训关联的必修课程数量
        LambdaQueryWrapper<ContentRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getBizId, trainId)
                .eq(ContentRelation::getIsRequired, 1) // 必修
                .eq(ContentRelation::getIsDel, 0);
        return Integer.valueOf(contentRelationMapper.selectCount(relationWrapper).toString());
    }
}
