package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.ContentRelation;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author cys
* @description 针对表【train_content_relation(培训内容关联表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.TrainContentRelation
*/
public interface ContentRelationMapper extends BaseMapper<ContentRelation> {

    /**
     * 批量逻辑删除内容关联
     *
     * @param ids 内容关联ID列表
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @param gmtModified 更新时间
     * @return 影响行数
     */
    int batchLogicalDelete(@Param("ids") List<Long> ids,
                           @Param("type")String type,
                           @Param("updaterId") Long updaterId, 
                           @Param("updaterName") String updaterName, 
                           @Param("gmtModified") Date gmtModified);

    /**
     * 批量插入内容关联
     *
     * @param relations 内容关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("relations") List<ContentRelation> relations);
}




