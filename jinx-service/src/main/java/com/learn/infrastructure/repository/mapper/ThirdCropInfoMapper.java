package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.ThirdCropInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author cys
* @description 针对表【third_crop_info(三方企业信息)】的数据库操作Mapper
* @createDate 2025-04-27 10:52:11
* @Entity com.learn.infrastructure.repository.entity.ThirdCropInfo
*/
public interface ThirdCropInfoMapper extends BaseMapper<ThirdCropInfo> {

    /**
     * 使用 JSON_SET 更新 attributes 字段中的特定属性
     *
     * @param id 记录ID
     * @param propertyPath JSON属性路径，格式为 '$.propertyName'
     * @param propertyValue JSON属性值
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 影响的行数
     */
    @Update("UPDATE third_crop_info SET attributes = JSON_SET(attributes, #{propertyPath}, #{propertyValue} ), " +
            "updater_id = #{updaterId}, updater_name = #{updaterName}, gmt_modified = NOW() " +
            "WHERE id = #{id}")
    int updateJsonProperty(@Param("id") Long id, 
                          @Param("propertyPath") String propertyPath, 
                          @Param("propertyValue") String propertyValue,
                          @Param("updaterId") Long updaterId,
                          @Param("updaterName") String updaterName);
}




