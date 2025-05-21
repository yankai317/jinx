package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author cys
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.User
*/
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 批量插入用户
     * @param list 用户列表
     * @return 受影响的行数
     */
    int batchInsert(@Param("list") List<User> list);
    
    /**
     * 批量更新用户
     * @param list 用户列表
     * @return 受影响的行数
     */
    int batchUpdateModifyTime(@Param("list") List<User> list);


    /**
     * 批量逻辑删除过期关系
     * @param baseTime 基准时间
     * @return 受影响的行数
     */
    int batchDeleteByUpdateTime(@Param("baseTime") Date baseTime);
}




