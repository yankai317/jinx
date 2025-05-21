package com.learn.service.org;

import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.org.OrgDTO;
import com.learn.service.dto.org.OrgRequest;
import com.learn.service.dto.user.UserDTO;

import java.util.List;
/**
 * 实现部门的相关基础接口，
 * 包括查询部门:需要控制是否返回树状结构、还是列表结构、查询部门下的人:需要支持返回用户信息和仅返回用户id
 * 数据库如下: department、department_user
 */
public interface OrgService {
    /**
     * 查询部门列表
     * @param request 查询请求参数
     * @return 部门列表查询结果
     */
    BaseResponse<OrgDTO> queryOrgs(OrgRequest request);
    
    /**
     * 查询部门树
     * @param request 查询请求参数
     * @return 部门树查询结果
     */
    BaseResponse<OrgDTO> queryOrgTree(OrgRequest request);
    
    /**
     * 查询部门下的用户
     * @param request 查询请求参数
     * @return 部门下的用户查询结果
     */
    BaseResponse<UserDTO> queryOrgUsers(OrgRequest request);
    
    /**
     * 查询部门下的用户ID列表
     * MUST NOT 支持单个id查询
     * 1. 支持多个org_ids 查询其id及其下面子部门的用户id列表, 并且通过 org 的部门path查询。走全文索引的逻辑
     * @param request 查询请求参数
     * @return 部门下的用户ID列表查询结果
     */
    BaseResponse<Long> queryOrgUserIds(OrgRequest request);

    /**
     * 查询部门的子部门
     * @param request 查询子部门请求参数
     * @return 子部门查询结果
     */
    BaseResponse<OrgDTO> queryOrgChildren(OrgRequest request);

    /**
     * 批量获取用户的部门信息
     * @param userIds 用户ID列表
     * @return 用户部门信息列表
     */
    BaseResponse<UserDTO> queryUserOrgs(List<Long> userIds);

}
