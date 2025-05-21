package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.OrgRole;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
* @author cys
* @description 针对表【org_role(组织架构角色表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.OrgRole
*/
public interface OrgRoleMapper extends BaseMapper<OrgRole> {
    
    /**
     * 批量插入角色
     * @param list 角色列表
     * @return 受影响的行数
     */
    int batchInsert(@Param("list") List<OrgRole> list);
    
    /**
     * 批量更新角色
     * @param list 角色列表
     * @return 受影响的行数
     */
    int updateBatchById(@Param("list") List<OrgRole> list);
    
    /**
     * 批量逻辑删除过期角色
     * @param baseTime 基准时间
     * @param now 当前时间
     * @param notInExternalIds 需要排除的外部ID集合
     * @return 受影响的行数
     */
    int batchDeleteByUpdateTime(@Param("baseTime") Date baseTime, @Param("now") Date now, @Param("notInExternalIds") Set<String> notInExternalIds);
}




