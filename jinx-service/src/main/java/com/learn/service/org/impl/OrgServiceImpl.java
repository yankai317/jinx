package com.learn.service.org.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.org.OrgDTO;
import com.learn.service.dto.org.OrgRequest;
import com.learn.service.dto.user.UserDTO;
import com.learn.service.org.OrgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 */
@Service
public class OrgServiceImpl implements OrgService {

    private static final Logger log = LoggerFactory.getLogger(OrgServiceImpl.class);
    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询部门列表
     *
     * @param request 查询请求参数
     * @return 部门列表查询结果
     */
    @Override
    public BaseResponse<OrgDTO> queryOrgs(OrgRequest request) {
        BaseResponse<OrgDTO> response = new BaseResponse<>();

        // 构建查询条件
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getIsDel, 0); // 未删除的部门

        // 关键字搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.like(Department::getDepartmentName, request.getKeyword());
        }


        // 指定ID列表查询
        if (!CollectionUtils.isEmpty(request.getOrgIds())) {
            queryWrapper.in(Department::getId, request.getOrgIds());
        }

        // 分页查询
        Page<Department> page = null;
        if (request.getPageNum() != null && request.getPageSize() != null) {
            page = new Page<>(request.getPageNum(), request.getPageSize());
            page = departmentMapper.selectPage(page, queryWrapper);
        }

        // 不分页查询
        List<Department> orgList;
        if (page != null) {
            orgList = page.getRecords();
            response.setTotal((int) page.getTotal());
        } else {
            orgList = departmentMapper.selectList(queryWrapper);
            response.setTotal(orgList.size());
        }

        // 转换为DTO
        List<OrgDTO> orgDTOList = convertToOrgDTOList(orgList);

        response.setSuccess(true);
        response.setData(orgDTOList);


        return response;
    }

    /**
     * 查询部门树
     * 优化后的逻辑：只获取当前点击组织下的子组织和人员，而不是一次性获取所有
     *
     * @param request 查询请求参数
     * @return 部门树查询结果
     */
    @Override
    public BaseResponse<OrgDTO> queryOrgTree(OrgRequest request) {
        BaseResponse<OrgDTO> response = new BaseResponse<>();
        OrgDTO returnOrgDTO = new OrgDTO();
        try {
            // 构建查询条件
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getIsDel, 0); // 未删除的部门

            // 关键字搜索
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.like(Department::getDepartmentName, request.getKeyword());
                List<Department> departments = departmentMapper.selectList(queryWrapper);
                if (!CollectionUtils.isEmpty(departments)) {
                    returnOrgDTO.setChildren(convertToOrgDTOList(departments));
                }


                LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
                userQueryWrapper.like(User::getNickname, request.getKeyword())
                                .eq(User::getIsDel, 0);
                List<User> users = userMapper.selectList(userQueryWrapper);
                if (!CollectionUtils.isEmpty(users)) {
                    returnOrgDTO.setUsers(convertToUserDTOList(users));
                }

                response.setItem(returnOrgDTO);
                response.setSuccess(true);
                return response;
            }
            
            // 如果指定了部门ID，则只查询该部门的子部门
            // 否则查询根部门（parentId为0或null的部门）
            if (request.getId() != null) {
                queryWrapper.eq(Department::getParentId, request.getId());
            } else {
                queryWrapper.and(wrapper -> wrapper.isNull(Department::getParentId)
                        .or().eq(Department::getParentId, 0L));
            }

            List<Department> departments = departmentMapper.selectList(queryWrapper);
            List<Long> departmentIds = departments.stream().map(Department::getId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(departments)) {
                // 3. 获取在department表中作为parent_id出现的ID列表
                long startTime = System.currentTimeMillis();
                List<Long> departmentHasChildrenIds = departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                                .select(Department::getParentId)
                                .eq(Department::getIsDel, 0)
                                .in(Department::getParentId, departmentIds)
                                .isNotNull(Department::getParentId))
                        .stream()
                        .map(Department::getParentId)
                        .distinct()
                        .collect(Collectors.toList());
                long endTime = System.currentTimeMillis();
                log.info("获取在department表中作为parent_id出现的ID列表耗时: {}ms", endTime - startTime);

                // 2. 获取在department_user表中出现的departmentId列表
                startTime = System.currentTimeMillis();
                List<Long> departmentHasUserIds = departmentUserMapper.selectList(new LambdaQueryWrapper<DepartmentUser>()
                                .select(DepartmentUser::getDepartmentId)
                                .eq(DepartmentUser::getIsDel, 0)
                                .in(DepartmentUser::getDepartmentId, departmentIds)
                                .isNotNull(DepartmentUser::getDepartmentId))
                        .stream()
                        .map(DepartmentUser::getDepartmentId)
                        .distinct()
                        .collect(Collectors.toList());
                endTime = System.currentTimeMillis();
                log.info("获取在department_user表中出现的departmentId列表耗时: {}ms", endTime - startTime); 

                // 3. 构建OrgDTO列表
                startTime = System.currentTimeMillis();
                List<OrgDTO> orgDTOs = new ArrayList<>();
                departments.forEach(department -> {
                    OrgDTO orgDTO = new OrgDTO();
                    orgDTO.setId(department.getId());
                    orgDTO.setName(department.getDepartmentName());
                    orgDTO.setParentId(department.getParentId());
                    orgDTO.setHasChildren(departmentHasChildrenIds.contains(department.getId()) || departmentHasUserIds.contains(department.getId()));
                    orgDTOs.add(orgDTO);
                });
                endTime = System.currentTimeMillis();
                log.info("构建OrgDTO列表耗时: {}ms", endTime - startTime);
                returnOrgDTO.setChildren(orgDTOs);
            }


            // 转换为DTO


            // 如果需要包含用户信息，则查询指定部门下的用户

            if (Boolean.TRUE.equals(request.getIncludeUsers())) {
                // 获取部门ID
                Long departmentId = request.getId();
                if (departmentId != null) {
                    // 查询部门下的用户关系
                    LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
                    duQueryWrapper.eq(DepartmentUser::getDepartmentId, departmentId)
                            .eq(DepartmentUser::getIsDel, 0);
                    List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duQueryWrapper);
                    
                    if (!CollectionUtils.isEmpty(departmentUsers)) {
                        // 获取用户ID列表
                        List<Long> userIds = departmentUsers.stream()
                                .map(DepartmentUser::getUserId)
                                .distinct()
                                .collect(Collectors.toList());
                        
                        // 查询用户信息
                        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
                        userQueryWrapper.in(User::getUserId, userIds)
                                .eq(User::getIsDel, 0);
                        List<User> users = userMapper.selectList(userQueryWrapper);

                        returnOrgDTO.setUsers(convertToUserDTOList(users));
                        returnOrgDTO.setId(departmentId);
                    }
                } else {
                    // 如果是根级查询，可以查询没有部门的用户（部门ID为0或null的用户）
                    returnOrgDTO.setId(0L);
                    LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
                    duQueryWrapper.and(wrapper -> wrapper.isNull(DepartmentUser::getDepartmentId)
                            .or().eq(DepartmentUser::getDepartmentId, 0L))
                            .eq(DepartmentUser::getIsDel, 0);
                    List<DepartmentUser> rootUsers = departmentUserMapper.selectList(duQueryWrapper);

                    if (!CollectionUtils.isEmpty(rootUsers)) {
                        // 获取用户ID列表
                        List<Long> userIds = rootUsers.stream()
                                .map(DepartmentUser::getUserId)
                                .distinct()
                                .collect(Collectors.toList());
                        
                        // 查询用户信息
                        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
                        userQueryWrapper.in(User::getUserId, userIds)
                                .eq(User::getIsDel, 0);
                        List<User> users = userMapper.selectList(userQueryWrapper);
                        
                        // 转换为UserDTO列表
                        List<UserDTO> userDTOs = users.stream()
                                .map(user -> {
                                    UserDTO userDTO = new UserDTO();
                                    userDTO.setId(user.getId());
                                    userDTO.setUserId(user.getUserId());
                                    userDTO.setNickname(user.getNickname());
                                    userDTO.setAvatar(user.getAvatar());
                                    return userDTO;
                                })
                                .collect(Collectors.toList());

                        // 创建一个OrgDTO对象，表示根部门，并设置其用户列表
                        returnOrgDTO.setUsers(userDTOs);

                    }
                }
            }
            // 将根部门的用户信息添加到响应中
            response.setItem(returnOrgDTO);
            response.setSuccess(true);
//            response.setData(orgDTOs);
//            Integer orgSize = returnOrgDTO.getChildren() == null ? 0 : returnOrgDTO.getChildren().size();
//            Integer userSize = returnOrgDTO.getUsers() == null ? 0 : returnOrgDTO.getUsers().size();
//            response.setTotal(orgSize + userSize);

        } catch (Exception e) {
            log.error("查询部门树失败", e);
            response.setSuccess(false);
            response.setMessage("查询部门树失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 查询部门下的用户
     *
     * @param request 查询请求参数
     * @return 部门下的用户查询结果
     */
    @Override
    public BaseResponse<UserDTO> queryOrgUsers(OrgRequest request) {
        BaseResponse<UserDTO> response = new BaseResponse<>();

        try {
            if (request.getId() == null) {
                response.setSuccess(false);
                response.setMessage("部门ID不能为空");
                return response;
            }

            // 查询部门下的用户ID
            LambdaQueryWrapper<DepartmentUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DepartmentUser::getDepartmentId, request.getId())
                    .eq(DepartmentUser::getIsDel, 0);

            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(departmentUsers)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }

            // 获取用户ID列表
            List<Long> userIds = departmentUsers.stream()
                    .map(DepartmentUser::getUserId)
                    .collect(Collectors.toList());

            // 查询用户信息
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getUserId, userIds)
                    .eq(User::getIsDel, 0);

            // 分页查询
            Page<User> page = null;
            if (request.getPageNum() != null && request.getPageSize() != null) {
                page = new Page<>(request.getPageNum(), request.getPageSize());
                page = userMapper.selectPage(page, userQueryWrapper);
            }

            // 不分页查询
            List<User> userList;
            if (page != null) {
                userList = page.getRecords();
                response.setTotal((int) page.getTotal());
            } else {
                userList = userMapper.selectList(userQueryWrapper);
                response.setTotal(userList.size());
            }

            // 转换为DTO
            List<UserDTO> userDTOList = convertToUserDTOList(userList);

            response.setSuccess(true);
            response.setData(userDTOList);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询部门下的用户失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 查询部门下的用户ID列表
     * MUST NOT 支持单个id查询
     * 1. 支持多个org_ids 查询其id及其下面子部门的用户id列表, 并且通过 org 的部门path查询。走全文索引的逻辑
     *
     * @param request 查询请求参数
     * @return 部门下的用户ID列表查询结果
     */
    @Override
    public BaseResponse<Long> queryOrgUserIds(OrgRequest request) {
        BaseResponse<Long> response = new BaseResponse<>();

        try {
            // 验证参数
            if (CollectionUtils.isEmpty(request.getOrgIds())) {
                response.setSuccess(false);
                response.setMessage("部门ID列表不能为空");
                return response;
            }

            // 查询部门下的用户ID
            List<Long> userIds = departmentMapper.selectUserIdsByOrgIdsWithChildren(request.getOrgIds());

            if (CollectionUtils.isEmpty(userIds)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }

            response.setSuccess(true);
            response.setData(userIds);
            response.setTotal(userIds.size());

        } catch (Exception e) {
            log.error("查询部门下的用户ID列表失败", e);
            response.setSuccess(false);
            response.setMessage("查询部门下的用户ID列表失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 查询部门的子部门
     *
     * @param request 查询子部门请求参数
     * @return 子部门查询结果
     */
    @Override
    public BaseResponse<OrgDTO> queryOrgChildren(OrgRequest request) {
        BaseResponse<OrgDTO> response = new BaseResponse<>();

        try {
            if (request.getId() == null) {
                response.setSuccess(false);
                response.setMessage("部门ID不能为空");
                return response;
            }

            // 查询子部门
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getParentId, request.getId())
                    .eq(Department::getIsDel, 0);

            // 分页查询
            Page<Department> page = null;
            if (request.getPageNum() != null && request.getPageSize() != null) {
                page = new Page<>(request.getPageNum(), request.getPageSize());
                page = departmentMapper.selectPage(page, queryWrapper);
            }

            // 不分页查询
            List<Department> childrenList;
            if (page != null) {
                childrenList = page.getRecords();
                response.setTotal((int) page.getTotal());
            } else {
                childrenList = departmentMapper.selectList(queryWrapper);
                response.setTotal(childrenList.size());
            }

            // 转换为DTO
            List<OrgDTO> childrenDTOList = convertToOrgDTOList(childrenList);

            response.setSuccess(true);
            response.setData(childrenDTOList);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询部门的子部门失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 将Department实体列表转换为OrgDTO列表
     *
     * @param departmentList Department实体列表
     * @return OrgDTO列表
     */
    private List<OrgDTO> convertToOrgDTOList(List<Department> departmentList) {
        if (CollectionUtils.isEmpty(departmentList)) {
            return Collections.emptyList();
        }

        return departmentList.stream().map(department -> {
            OrgDTO orgDTO = new OrgDTO();
            orgDTO.setId(department.getId());
            orgDTO.setName(department.getDepartmentName());
            // 这里可以设置code，如果Department中有对应的字段
            // orgDTO.setCode(department.getDepartmentCode());
            orgDTO.setParentId(department.getParentId());
            return orgDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 将User实体列表转换为UserDTO列表
     *
     * @param userList User实体列表
     * @return UserDTO列表
     */
    private List<UserDTO> convertToUserDTOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }

        return userList.stream().map(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getUserId());
            userDTO.setNickname(user.getNickname());
            userDTO.setAvatar(user.getAvatar());
            // 这里可以设置其他字段，如果User中有对应的字段
            // userDTO.setUsername(user.getUsername());
            // userDTO.setEmail(user.getEmail());
            // userDTO.setPhone(user.getPhone());
            return userDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 构建部门树
     *
     * @param allOrgs 所有部门DTO列表
     * @return 根部门列表
     */
    private List<OrgDTO> buildOrgTree(List<OrgDTO> allOrgs) {
        if (CollectionUtils.isEmpty(allOrgs)) {
            return Collections.emptyList();
        }

        // 按ID分组
        Map<Long, OrgDTO> orgMap = allOrgs.stream()
                .collect(Collectors.toMap(OrgDTO::getId, org -> org));

        List<OrgDTO> rootOrgs = new ArrayList<>();
        for (OrgDTO org : allOrgs) {
            Long parentId = org.getParentId();

            // 如果是根部门（parentId为0或null）
            if (parentId == null || parentId == 0L) {
                rootOrgs.add(org);
            } else {
                // 非根部门，添加到父部门的children列表中
                OrgDTO parentOrg = orgMap.get(parentId);
                if (parentOrg != null) {
                    if (parentOrg.getChildren() == null) {
                        parentOrg.setChildren(new ArrayList<>());
                    }
                    parentOrg.getChildren().add(org);
                } else {
                    // 如果找不到父部门，则作为根部门处理
                    rootOrgs.add(org);
                }
            }
        }

        return rootOrgs;
    }

    /**
     * 批量获取用户的部门信息
     *
     * @param userIds 用户ID列表
     * @return 用户部门信息列表
     */
    @Override
    public BaseResponse<UserDTO> queryUserOrgs(List<Long> userIds) {
        BaseResponse<UserDTO> response = new BaseResponse<>();

        try {
            if (CollectionUtils.isEmpty(userIds)) {
                response.setSuccess(false);
                response.setMessage("用户ID列表不能为空");
                return response;
            }

            log.info("开始获取用户部门信息，用户ID列表: {}", userIds);

            // 查询用户基本信息
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getUserId, userIds)
                    .eq(User::getIsDel, 0);
            List<User> userList = userMapper.selectList(userQueryWrapper);

            log.info("查询到的用户列表 size: {}, 用户: {}", userList.size(), userList);

            if (CollectionUtils.isEmpty(userList)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }

            // 查询用户部门关系
            LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
            duQueryWrapper.in(DepartmentUser::getUserId, userIds)
                    .eq(DepartmentUser::getIsDel, 0);
            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duQueryWrapper);

            log.info("查询到的用户部门关系 size: {}, 关系: {}", departmentUsers.size(), departmentUsers);

            // 获取部门ID列表
            List<Long> departmentIds = departmentUsers.stream()
                    .map(DepartmentUser::getDepartmentId)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("解析出的部门ID列表: {}", departmentIds);

            // 查询部门信息
            Map<Long, Department> departmentMap;
            if (!CollectionUtils.isEmpty(departmentIds)) {
                LambdaQueryWrapper<Department> deptQueryWrapper = new LambdaQueryWrapper<>();
                deptQueryWrapper.in(Department::getId, departmentIds)
                        .eq(Department::getIsDel, 0);
                List<Department> departments = departmentMapper.selectList(deptQueryWrapper);

                log.info("查询到的部门列表: {}", departments);

                departmentMap = departments.stream()
                        .collect(Collectors.toMap(Department::getId, dept -> dept, (k1, k2) -> k1));
            } else {
                departmentMap = Collections.emptyMap();
            }

            // 用户ID到部门的映射
            Map<Long, List<DepartmentUser>> userDeptMap = departmentUsers.stream()
                    .collect(Collectors.groupingBy(DepartmentUser::getUserId));

            // 转换为UserDTO，并设置部门信息
            List<UserDTO> userDTOList = userList.stream().map(user -> {
                UserDTO userDTO = new UserDTO();
                userDTO.setId(user.getId());
                userDTO.setUserId(user.getUserId());
                userDTO.setNickname(user.getNickname());
                userDTO.setAvatar(user.getAvatar());

                // 获取用户的部门信息
                List<DepartmentUser> userDepts = userDeptMap.getOrDefault(user.getUserId(), Collections.emptyList());
                log.info("用户ID: {}, 昵称: {}, 关联的部门关系: {}", user.getUserId(), user.getNickname(), userDepts);

                if (!CollectionUtils.isEmpty(userDepts)) {
                    // 主部门（通常是第一个部门）
                    DepartmentUser mainDept = userDepts.get(0);
                    Department department = departmentMap.get(mainDept.getDepartmentId());

                    if (department != null) {
                        userDTO.setDepartmentId(department.getId());
                        userDTO.setDepartmentName(department.getDepartmentName());
                        log.info("用户ID: {}, 设置主部门: id={}, name={}", user.getUserId(), department.getId(), department.getDepartmentName());
                    } else {
                        log.warn("用户ID: {} 的主部门ID: {} 在部门映射中未找到", user.getUserId(), mainDept.getDepartmentId());
                    }

                    // 如果有多个部门，设置部门列表
                    if (userDepts.size() > 1) {
                        List<OrgDTO> departments = userDepts.stream()
                                .map(DepartmentUser::getDepartmentId)
                                .map(departmentMap::get)
                                .filter(dept -> dept != null)
                                .map(dept -> {
                                    OrgDTO orgDTO = new OrgDTO();
                                    orgDTO.setId(dept.getId());
                                    orgDTO.setName(dept.getDepartmentName());
                                    return orgDTO;
                                })
                                .collect(Collectors.toList());

                        userDTO.setDepartments(departments);
                        log.info("用户ID: {}, 设置多部门列表: {}", user.getUserId(),
                                departments.stream().map(OrgDTO::getName).collect(Collectors.joining(", ")));
                    } else if (userDepts.size() == 1) {
                        // 即使只有一个部门，也设置departments字段，保证前端一致的数据结构
                        Department dept = departmentMap.get(userDepts.get(0).getDepartmentId());
                        if (dept != null) {
                            List<OrgDTO> departments = new ArrayList<>();
                            OrgDTO orgDTO = new OrgDTO();
                            orgDTO.setId(dept.getId());
                            orgDTO.setName(dept.getDepartmentName());
                            departments.add(orgDTO);
                            userDTO.setDepartments(departments);
                            log.info("用户ID: {}, 设置单部门列表: {}", user.getUserId(), dept.getDepartmentName());
                        }
                    }
                } else {
                    log.warn("用户ID: {} 没有关联的部门", user.getUserId());
                }

                return userDTO;
            }).collect(Collectors.toList());

            response.setSuccess(true);
            response.setData(userDTOList);
            response.setTotal(userDTOList.size());

            log.info("返回用户部门信息 size: {}, 第一个用户样例: {}", userDTOList.size(),
                    userDTOList.isEmpty() ? "无" : userDTOList.get(0));

        } catch (Exception e) {
            log.error("批量获取用户的部门信息失败", e);
            response.setSuccess(false);
            response.setMessage("批量获取用户的部门信息失败: " + e.getMessage());
        }

        return response;
    }
}
