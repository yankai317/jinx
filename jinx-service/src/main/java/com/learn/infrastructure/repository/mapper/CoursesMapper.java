package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.Courses;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
* @author cys
* @description 针对表【Courses】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.Courses
*/
public interface CoursesMapper extends BaseMapper<Courses> {
    /**
     * 批量更新创建人
     *
     * @return Map包含learnerCount(学习人数)和completionCount(完成人数)
     */
    void batchUpdateCreator(@Param("courseIds") List<Long> courseIds, @Param("newCreatorId") Long newCreatorId, @Param("newCreatorName") String newCreatorName,
                      @Param("operatorId") Long operatorId, @Param("operatorName") String operatorName);

    /**
     * 批量逻辑删除课程
     *
     * @param ids 课程ID列表
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @param gmtModified 更新时间
     * @return 影响行数
     */
    int batchLogicalDelete(@Param("ids") List<Long> ids, 
                           @Param("updaterId") Long updaterId, 
                           @Param("updaterName") String updaterName, 
                           @Param("gmtModified") Date gmtModified);


    List<Map<String, Object>> batchCountViewAndComplete(List<Long> courseIds);
}




