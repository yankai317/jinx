package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.LearningMap;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author cys
* @description 针对表【learning_map(学习地图表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.LearningMap
*/
public interface LearningMapMapper extends BaseMapper<LearningMap> {

    /**
     * 批量更新创建人
     *
     * @return Map包含learnerCount(学习人数)和completionCount(完成人数)
     */
    void batchUpdateCreator(@Param("ids") List<Long> ids, @Param("newCreatorId") Long newCreatorId, @Param("newCreatorName") String newCreatorName,
                           @Param("operatorId") Long operatorId, @Param("operatorName") String operatorName);

}




