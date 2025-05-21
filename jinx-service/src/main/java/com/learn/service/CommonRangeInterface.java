package com.learn.service;

import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.service.dto.*;

import java.util.List;

/**
 * 通用范围处理表，支持功能，
 * 其他功能模块创建部门、角色、人员范围的时候。提供统一的能力。
 * 目前功能模块包括: 可见范围、协同管理、任务指派
 * 业务模块包括: 课程、培训、学习地图
 * 提供的功能:
 * 1. 创建范围，输入功能模块类型【比如可见范围】，
 * 输入业务模块类型[比如培训、课程、学习地图]和id(单个)，
 * 输入目标范围的类型[部门、角色、人员]和ids(批量)
 * 2. 查询范围中命中了多少个业务id。根据部门、角色、人员id。查询是否在范围内，需要利用到mysql的全文索引。
 * 3. 查询业务id配置了多少信息
 */
public interface CommonRangeInterface {

    /**
     * 批量创建范围
     * @param request 创建范围请求对象
     * @return 创建范围响应对象
     */
    CommonRangeCreateResponse batchCreateRange(CommonRangeCreateRequest request);

    /**
     * 根据目标ID查询在范围内的业务ID列表
     * @param request 查询范围请求对象
     * @return 查询范围响应对象列表
     */
    List<CommonRangeQueryResponse> queryBusinessIdsByTargets(CommonRangeQueryRequest request);

    /**
     * 查询业务ID配置的范围信息
     * @param request 查询范围请求对象
     * @return 查询范围响应对象
     */
    CommonRangeQueryResponse queryRangeConfigByBusinessId(CommonRangeQueryRequest request);

    /**
     * 删除业务ID的范围配置
     * @param request 删除范围请求对象
     * @return 删除范围响应对象
     */
    Boolean deleteRangeByBusinessId(CommonRangeDeleteRequest request);

    /**
     * 更新业务ID的范围配置
     * @param request 更新范围请求对象
     * @return 更新范围响应对象
     */
    CommonRangeUpdateResponse updateRangeByBusinessId(CommonRangeUpdateRequest request);

    /**
     * 判断这个用户是否有这个 [功能枚举+业务模块类型+业务模块id] 的权限
     * @param request
     */
    void checkUserHasRightsIfNotThrowException(CommonRangeQueryRequest request);

    /**
     * 判断这个用户是否有这个 [功能枚举+业务模块类型+业务模块id] 的权限
     * 1. 需要先查询用户的角色、部门、人员id
     * 2. 根据角色、部门、人员id、功能枚举、业务模块类型、业务模块id，查询角色、部门、人员id是否在范围内
     * @param request
     * @return
     */
    Boolean checkUserHasRight(CommonRangeQueryRequest request);


    /**
     * 根据 用户id + modelType 查询当前用户在哪些业务模块下有权限
     * 1. MUST 先根据用户 查询用户的角色、部门、人员id
     * 2. 根据角色、部门、人员id、功能枚举、业务模块类型、业务模块id，查询角色、部门、人员id是否在范围内
     * @param request 查询范围请求对象
     * @return 查询范围响应对象列表
     */
    List<CommonRangeQueryResponse> listCommonRangeByCondition(CommonRangeQueryRequest request);

    /**
     * 根据范围ids查询范围
     *
     * @param rangeIds 范围ID列表
     * @return 范围对象列表
     */
    List<CommonRange> listCommonRangeByRangeIds(List<Long> rangeIds);
}
