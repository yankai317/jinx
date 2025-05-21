package com.learn.service.user;

import com.learn.service.dto.*;

import java.util.List;

/**
 * 实现用户
 * 需要处理事项
 * 1. 用户创建接口
 * 2. 用户批量查询接口
 * 3. 用户删除接口
 * 操作的数据库表: user、user_third_party
 */
public interface UserService {
    /**
     * 创建用户
     * 1.创建接口需要根据三方关系表，判断用户是否存在，如果存在直接返回，不存在就创建
     * 2.user中的 user_id 需要大于6位以上，小于20位以下，并且需要是随机的。不能是顺序的。且不能重复
     * @param request 创建用户请求对象
     * @return 创建用户响应对象
     */
    UserCreateResponse createUser(UserCreateRequest request);
    
    /**
     * 批量创建用户
     * 1.批量创建接口需要根据三方关系表，判断用户是否存在，如果存在直接返回，不存在就创建
     * 2.user中的 user_id 需要大于6位以上，小于20位以下，并且需要是随机的。不能是顺序的。且不能重复
     *
     * @param requests       创建用户请求对象列表
     * @param externalSource
     * @return 创建用户响应对象列表
     */
    List<UserCreateResponse> batchCreateUsers(List<UserCreateRequest> requests, String externalSource);
    
    /**
     * 批量查询用户
     * 1. 用户id查询
     * @param request 查询用户请求对象
     * @return 查询用户响应对象列表
     */
    List<UserQueryResponse> queryUsers(UserQueryRequest request);

    /**
     * 批量查询管理员
     * 1. 用户id查询
     * @param request 查询用户请求对象
     * @return 查询用户响应对象列表
     */
    List<UserQueryResponse> queryAdministrators(UserQueryRequest request);

    /**
     * 删除用户
     * @param request 删除用户请求对象
     * @return 删除用户响应对象
     */
    UserDeleteResponse deleteUser(UserDeleteRequest request);

}
