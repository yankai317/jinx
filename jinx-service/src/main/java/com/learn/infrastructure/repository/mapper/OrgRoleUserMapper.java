package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.OrgRoleUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
* @author cys
* @description 针对表【org_role_user(组织架构角色用户关联表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.OrgRoleUser
*/
public interface OrgRoleUserMapper extends BaseMapper<OrgRoleUser> {
    
    /**
     * 批量插入或更新角色用户关系
     * @param list 角色用户关系列表
     * @return 受影响的行数
     */
    int insertOrUpdateBatch(@Param("list") List<OrgRoleUser> list);
    
    /**
     * 批量更新角色用户关系
     * @param list 角色用户关系列表
     * @return 受影响的行数
     */
    int updateBatchById(@Param("list") List<OrgRoleUser> list);
    
    /**
     * 批量逻辑删除过期关系
     * @param baseTime 基准时间
     * @param now 当前时间
     * @param notInIds 需要排除的关系ID集合（orgRoleId_userId格式）
     * @return 受影响的行数
     */
    int batchDeleteByUpdateTime(@Param("baseTime") Date baseTime, @Param("now") Date now, @Param("notInIds") Set<String> notInIds);
}




