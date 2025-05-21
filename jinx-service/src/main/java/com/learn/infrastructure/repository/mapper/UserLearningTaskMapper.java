package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户学习记录表 Mapper 接口
 *
 * @author yujintao
 * @date 2025/5/6
 */
@Mapper
public interface UserLearningTaskMapper extends BaseMapper<UserLearningTask> {
    
    /**
     * 统计学习人数和完成人数
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return Map包含learnerCount(学习人数)和completionCount(完成人数)
     */
    Map<String, Object> countLearnerAndCompletion(@Param("bizType") String bizType, @Param("bizId") Long bizId);
    
    /**
     * 统计学习时长
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return Map包含totalDuration(总学习时长)和avgDuration(平均学习时长)
     */
    Map<String, Object> countDuration(@Param("bizType") String bizType, @Param("bizId") Long bizId);
    
    /**
     * 按内容类型统计完成率
     *
     * @param parentType 父业务类型
     * @param parentId 父业务ID
     * @return 按内容类型分组的统计结果列表
     */
    List<Map<String, Object>> countCompletionByContentType(@Param("parentType") String parentType, @Param("parentId") Long parentId);
    
    /**
     * 按日期统计学习分布
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 按日期分组的统计结果列表
     */
    List<Map<String, Object>> countTimeDistribution(@Param("bizType") String bizType, @Param("bizId") Long bizId);
    
    /**
     * 按业务类型统计用户学习记录数量
     *
     * @param userId 用户ID
     * @return 包含各类型学习记录数量的Map
     */
    Map<String, Object> countUserLearningByTypes(@Param("userId") Long userId, @Param("countSelective") boolean countSelective);

    List<UserLearningTask> getLearningTask(@Param("userId") Long userId, @Param("bizType") String bizType);
}
