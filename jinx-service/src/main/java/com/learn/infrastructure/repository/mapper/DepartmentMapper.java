package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author cys
* @description 针对表【department(部门表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.Department
*/
public interface DepartmentMapper extends BaseMapper<Department> {
    
    /**
     * 根据部门ID列表查询部门及其子部门下的所有用户ID
     * 通过部门path进行模糊匹配，支持全文索引查询
     * 
     * @param orgIds 部门ID列表
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByOrgIdsWithChildren(@Param("orgIds") List<Long> orgIds);

    /**
     * 批量逻辑删除过期部门
     * @param params 包含baseTime(基准时间)、now(当前时间)、notInExternalIds(排除的外部ID集合)、externalSource(外部来源)
     * @return 受影响的行数
     */
    int batchDeleteByUpdateTime(Map<String, Object> params);
    
    /**
     * 批量插入部门
     * @param list 部门列表
     * @return 受影响的行数
     */
    int batchInsert(@Param("list") List<Department> list);
    
    /**
     * 批量更新部门
     * @param list 部门列表
     * @return 受影响的行数
     */
    int batchUpdate(@Param("list") List<Department> list);
}




