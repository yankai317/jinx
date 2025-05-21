package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author cys
* @description 针对表【user_third_party(用户第三方授权关联表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.UserThirdParty
*/
public interface UserThirdPartyMapper extends BaseMapper<UserThirdParty> {
    
    /**
     * 批量插入用户第三方关联
     * @param list 用户第三方关联列表
     * @return 受影响的行数
     */
    int batchInsert(@Param("list") List<UserThirdParty> list);
    
    /**
     * 批量更新用户第三方关联
     *
     * @param list           用户第三方关联列表
     * @param externalSource
     * @return 受影响的行数
     */
    int batchUpdateModifyTime(@Param("list") List<UserThirdParty> list, @Param("thirdPartyType") String externalSource);


    /**
     * 批量逻辑删除过期关系
     * @param baseTime 基准时间
     * @return 受影响的行数
     */
    int batchDeleteByUpdateTime(@Param("baseTime") Date baseTime);
}




