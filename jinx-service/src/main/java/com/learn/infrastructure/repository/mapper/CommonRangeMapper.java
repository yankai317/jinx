package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.CommonRange;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author cys
* @description 针对表【common_range(考试可见范围表)】的数据库操作Mapper
* @createDate 2025-04-17 15:07:26
* @Entity com.learn.infrastructure.repository.entity.CommonRange
*/
public interface CommonRangeMapper extends BaseMapper<CommonRange> {

    /**
     * 批量插入范围记录
     * @param rangeList 范围记录列表
     * @return 插入的记录数
     */
    int batchInsert(@Param("list") List<CommonRange> rangeList);

    /**
     * 根据目标ID查询业务ID列表
     * @param modelType 功能模块类型
     * @param type 业务模块类型
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 范围记录列表
     */
    List<CommonRange> queryRangesByTargetId(@Param("modelType") String modelType,@Param("type") String type,
                                         @Param("targetType") String targetType,@Param("targetId") Long targetId);

    /**
     * 根据多个目标类型和ID查询业务ID列表
     * @param modelType 功能模块类型
     * @param type 业务模块类型
     * @param targetTypeAndIds 目标类型和ID的映射
     * @return 范围记录列表
     */
    List<CommonRange> queryRangesByTargets(@Param("modelType") String modelType,@Param("type") String type,
                                        @Param("targetTypeAndIds") Map<String, List<Long>> targetTypeAndIds);

    /**
     * 根据业务ID查询范围配置
     * @param modelType 功能模块类型
     * @param type 业务模块类型
     * @param typeId 业务ID
     * @return 范围配置列表
     */
    List<CommonRange> queryRangeByBusinessId(@Param("modelType") String modelType,
                                @Param("type") String type,
                                           @Param("typeId") Long typeId);
                                           
    /**
     * 根据用户ID和业务模块类型查询有权限的业务ID列表
     * 通过用户ID查询关联的部门ID和角色ID，然后统一查询
     * @param modelType 功能模块类型
     * @param type 业务模块类型，如果为空则查询所有类型
     * @param userId 用户ID
     * @param departmentIds 用户所属部门ID列表
     * @param roleIds 用户所属角色ID列表
     * @return 范围记录列表
     */
    List<CommonRange> queryRangesByUser(@Param("modelType") String modelType,
                                      @Param("type") String type,
                                      @Param("userIds") List<String> userIds,
                                      @Param("departmentIds") List<Long> departmentIds,
                                      @Param("roleIds") List<Long> roleIds);
}
