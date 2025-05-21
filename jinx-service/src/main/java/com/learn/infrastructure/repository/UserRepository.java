package com.learn.infrastructure.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.service.dto.UserQueryRequest;

import java.util.List;

/**
 * 用户数据访问接口
 * 提供用户相关的数据库操作方法
 */
public interface UserRepository {

    /**
     * 根据条件查询用户
     *
     * @param request 查询条件
     * @return 用户列表
     */
    List<User> queryUserByCondition(UserQueryRequest request);

    /**
     * 分页查询用户
     *
     * @param request 查询条件
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 分页用户列表
     */
    Page<User> queryUserByPage(UserQueryRequest request, Integer pageNum, Integer pageSize);

    /**
     * 根据用户ID列表查询用户
     *
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<User> queryUserByIds(List<Long> userIds);

    /**
     * 根据用户ID列表查询用户第三方关联信息
     *
     * @param userIds 用户ID列表
     * @param thirdPartyType 第三方平台类型（可选）
     * @return 用户第三方关联信息列表
     */
    List<UserThirdParty> queryUserThirdPartyByUserIds(List<Long> userIds, String thirdPartyType, boolean containDeleted);

    /**
     * 根据第三方平台类型和用户ID查询用户第三方关联信息
     *
     * @param thirdPartyType 第三方平台类型
     * @param thirdPartyUserIds 第三方平台用户ID列表
     * @return 用户第三方关联信息列表
     */
    List<UserThirdParty> queryUserThirdPartyByThirdPartyIds(String thirdPartyType, List<String> thirdPartyUserIds);
}
