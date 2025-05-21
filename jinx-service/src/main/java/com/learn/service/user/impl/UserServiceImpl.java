package com.learn.service.user.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.UserRepository;
import com.learn.infrastructure.repository.entity.FunctionRole;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.infrastructure.repository.mapper.FunctionRoleMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.infrastructure.repository.mapper.UserThirdPartyMapper;
import com.learn.service.dto.*;
import com.learn.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FunctionRoleUserMapper functionRoleUserMapper;

    @Autowired
    private FunctionRoleMapper functionRoleMapper;

    /**
     * 创建用户
     * 1.创建接口需要根据三方关系表，判断用户是否存在，如果存在直接返回，不存在就创建
     * 2.user中的 user_id 需要大于6位以上，小于20位以下，并且需要是随机的。不能是顺序的。且不能重复
     * @param request 创建用户请求对象
     * @return 创建用户响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreateResponse createUser(UserCreateRequest request) {
        UserCreateResponse response = new UserCreateResponse();
        
        try {
            // 参数校验
            if (StringUtils.isEmpty(request.getThirdPartyType()) || StringUtils.isEmpty(request.getThirdPartyUserId())) {
                response.setSuccess(false);
                response.setErrorMessage("第三方平台类型和用户ID不能为空");
                return response;
            }
            
            // 1. 根据第三方平台类型和用户ID查询是否已存在
            LambdaQueryWrapper<UserThirdParty> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserThirdParty::getThirdPartyType, request.getThirdPartyType())
                    .eq(UserThirdParty::getThirdPartyUserId, request.getThirdPartyUserId())
                    .eq(UserThirdParty::getIsDel, 0);
            
            UserThirdParty existUserThirdParty = userThirdPartyMapper.selectOne(queryWrapper);
            
            if (existUserThirdParty != null) {
                // 用户已存在，直接返回
                response.setUserId(existUserThirdParty.getUserId());
                response.setIsNewUser(false);
                response.setSuccess(true);
                return response;
            }
            
            // 2. 用户不存在，创建新用户
            Date now = new Date();
            
            // 2.1 创建用户基本信息
            User user = new User();
            // 生成随机用户ID，确保大于6位小于20位
            Long userId = generateRandomUserId();
            user.setUserId(userId);
            user.setEmployeeNo(request.getEmployeeNo());
            user.setNickname(request.getNickname());
            user.setAvatar(request.getAvatar());
            user.setIsDel(0);
            
            userMapper.insert(user);
            
            // 2.2 创建用户第三方关联信息
            UserThirdParty userThirdParty = new UserThirdParty();
            userThirdParty.setUserId(userId);
            userThirdParty.setThirdPartyType(request.getThirdPartyType());
            userThirdParty.setThirdPartyUserId(request.getThirdPartyUserId());
            userThirdParty.setThirdPartyUsername(request.getThirdPartyUsername());
            userThirdParty.setIsDel(0);
            userThirdParty.setAttributes(JSON.toJSONString(request.getAttributes()));
            userThirdPartyMapper.insert(userThirdParty);
            
            // 3. 返回创建结果
            response.setUserId(userId);
            response.setIsNewUser(true);
            response.setSuccess(true);
            
            return response;
        } catch (Exception e) {
            log.error("创建用户异常", e);
            response.setSuccess(false);
            response.setErrorMessage("创建用户失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量查询用户
     * 1. 用户id查询
     * 2. 支持查询所有用户列表，用于前端页面展示
     * @param request 查询用户请求对象
     * @return 查询用户响应对象列表
     */
    @Override
    public List<UserQueryResponse> queryUsers(UserQueryRequest request) {
        List<UserQueryResponse> responseList = new ArrayList<>();
        
        try {
            // 查询条件为空时，查询所有用户
            boolean queryAll = (CollectionUtils.isEmpty(request.getUserIds()) && 
                               (StringUtils.isEmpty(request.getThirdPartyType()) ||
                                CollectionUtils.isEmpty(request.getThirdPartyUserIds())));
            
            // 根据查询条件获取用户信息
            List<User> users = new ArrayList<>();
            List<UserThirdParty> userThirdParties = new ArrayList<>();
            
            if (queryAll) {
                // 查询所有用户，支持分页
                if (request.getPageNum() != null && request.getPageSize() != null) {
                    // 使用分页查询
                    Page<User> page = userRepository.queryUserByPage(request, request.getPageNum(), request.getPageSize());
                    users = page.getRecords();
                } else {
                    // 不分页，查询所有用户
                    users = userRepository.queryUserByCondition(request);
                }
                
                if (!CollectionUtils.isEmpty(users)) {
                    // 获取用户ID列表
                    List<Long> userIds = users.stream()
                            .map(User::getUserId)
                            .collect(Collectors.toList());
                    
                    // 查询用户第三方关联信息
                    userThirdParties = userRepository.queryUserThirdPartyByUserIds(userIds, request.getThirdPartyType(), request.isContainDeleted());
                }
            } else if (!CollectionUtils.isEmpty(request.getUserIds())) {
                // 1. 根据用户ID查询
                users = userRepository.queryUserByIds(request.getUserIds());
                
                if (!CollectionUtils.isEmpty(users)) {
                    // 获取用户ID列表
                    List<Long> userIds = users.stream()
                            .map(User::getUserId)
                            .collect(Collectors.toList());
                    
                    // 查询用户第三方关联信息
                    userThirdParties = userRepository.queryUserThirdPartyByUserIds(userIds, request.getThirdPartyType(), request.isContainDeleted());
                }
            } else {
                // 2. 根据第三方平台类型和用户ID查询
                userThirdParties = userRepository.queryUserThirdPartyByThirdPartyIds(
                        request.getThirdPartyType(), request.getThirdPartyUserIds());
                
                if (!CollectionUtils.isEmpty(userThirdParties)) {
                    // 获取用户ID列表
                    List<Long> userIds = userThirdParties.stream()
                            .map(UserThirdParty::getUserId)
                            .collect(Collectors.toList());
                    
                    // 查询用户基本信息
                    users = userRepository.queryUserByIds(userIds);
                }
            }

            // 构建用户第三方关联映射 <userId, List<UserThirdParty>>
            Map<Long, List<UserThirdParty>> userThirdPartyMap = userThirdParties.stream()
                    .collect(Collectors.groupingBy(UserThirdParty::getUserId));
            
            // 组装响应对象
            for (User user : users) {
                Long userId = user.getUserId();
                List<UserThirdParty> thirdParties = userThirdPartyMap.getOrDefault(userId, new ArrayList<>());
                
                // 如果指定了第三方平台类型，则只返回该类型的关联信息
                if (!StringUtils.isEmpty(request.getThirdPartyType()) && !thirdParties.isEmpty()) {
                    thirdParties = thirdParties.stream()
                            .filter(tp -> request.getThirdPartyType().equals(tp.getThirdPartyType()))
                            .collect(Collectors.toList());
                }
                
                // 为每个第三方关联创建一个响应对象
                if (thirdParties.isEmpty()) {
                    // 如果没有第三方关联，创建一个只包含基本信息的响应对象
                    UserQueryResponse response = new UserQueryResponse();
                    response.setUserId(userId);
                    response.setEmployeeNo(user.getEmployeeNo());
                    response.setNickname(user.getNickname());
                    response.setAvatar(user.getAvatar());
                    
                    responseList.add(response);
                } else {
                    // 为每个第三方关联创建一个响应对象
                    for (UserThirdParty thirdParty : thirdParties) {
                        UserQueryResponse response = new UserQueryResponse();
                        response.setUserId(userId);
                        response.setEmployeeNo(user.getEmployeeNo());
                        response.setNickname(user.getNickname());
                        response.setAvatar(user.getAvatar());
                        response.setThirdPartyType(thirdParty.getThirdPartyType());
                        response.setThirdPartyUserId(thirdParty.getThirdPartyUserId());
                        response.setThirdPartyUsername(thirdParty.getThirdPartyUsername());
                        
                        responseList.add(response);
                    }
                }
            }
            
            return responseList;
        } catch (Exception e) {
            log.error("查询用户异常", e);
            return responseList;
        }
    }

    @Override
    public List<UserQueryResponse> queryAdministrators(UserQueryRequest request) {
        List<UserQueryResponse> responseList = new ArrayList<>();
        
        try {
            // 1. 查询出function_role中所有未被删除的角色id列表
            LambdaQueryWrapper<FunctionRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(FunctionRole::getIsDel, 0);
            List<FunctionRole> roles = functionRoleMapper.selectList(roleWrapper);
            
            if (CollectionUtils.isEmpty(roles)) {
                return responseList;
            }
            
            // 提取角色ID列表
            List<Long> roleIds = roles.stream()
                    .map(FunctionRole::getId)
                    .collect(Collectors.toList());
            
            // 2. 在function_role_user中查询出所有有效的管理员角色信息，和user表联表查询
            List<User> adminUsers = new ArrayList<>();
            
            // 支持分页查询
            if (request.getPageNum() != null && request.getPageSize() != null) {
                // 使用分页查询
                Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
                page = functionRoleUserMapper.queryAdministratorUsers(roleIds, request.getKeyword(), page);
                adminUsers = page.getRecords();
            } else {
                // 不分页，查询所有管理员
                adminUsers = functionRoleUserMapper.queryAdministratorUsersWithoutPage(roleIds, request.getKeyword());
            }
            
            if (CollectionUtils.isEmpty(adminUsers)) {
                return responseList;
            }
            
            // 3. 获取用户ID列表
            List<Long> userIds = adminUsers.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());
            
            // 4. 查询用户第三方关联信息
            List<UserThirdParty> userThirdParties = userRepository.queryUserThirdPartyByUserIds(
                    userIds, request.getThirdPartyType(), request.isContainDeleted());
            
            // 5. 构建用户第三方关联映射 <userId, List<UserThirdParty>>
            Map<Long, List<UserThirdParty>> userThirdPartyMap = userThirdParties.stream()
                    .collect(Collectors.groupingBy(UserThirdParty::getUserId));
            
            // 6. 组装响应对象
            for (User user : adminUsers) {
                Long userId = user.getUserId();
                List<UserThirdParty> thirdParties = userThirdPartyMap.getOrDefault(userId, new ArrayList<>());
                
                // 如果指定了第三方平台类型，则只返回该类型的关联信息
                if (!StringUtils.isEmpty(request.getThirdPartyType()) && !thirdParties.isEmpty()) {
                    thirdParties = thirdParties.stream()
                            .filter(tp -> request.getThirdPartyType().equals(tp.getThirdPartyType()))
                            .collect(Collectors.toList());
                }
                
                // 为每个第三方关联创建一个响应对象
                if (thirdParties.isEmpty()) {
                    // 如果没有第三方关联，创建一个只包含基本信息的响应对象
                    UserQueryResponse response = new UserQueryResponse();
                    response.setUserId(userId);
                    response.setEmployeeNo(user.getEmployeeNo());
                    response.setNickname(user.getNickname());
                    response.setAvatar(user.getAvatar());
                    
                    responseList.add(response);
                } else {
                    // 为每个第三方关联创建一个响应对象
                    for (UserThirdParty thirdParty : thirdParties) {
                        UserQueryResponse response = new UserQueryResponse();
                        response.setUserId(userId);
                        response.setEmployeeNo(user.getEmployeeNo());
                        response.setNickname(user.getNickname());
                        response.setAvatar(user.getAvatar());
                        response.setThirdPartyType(thirdParty.getThirdPartyType());
                        response.setThirdPartyUserId(thirdParty.getThirdPartyUserId());
                        response.setThirdPartyUsername(thirdParty.getThirdPartyUsername());
                        
                        responseList.add(response);
                    }
                }
            }
            
            return responseList;
        } catch (Exception e) {
            log.error("查询管理员异常", e);
            return responseList;
        }
    }

    /**
     * 删除用户
     * @param request 删除用户请求对象
     * @return 删除用户响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDeleteResponse deleteUser(UserDeleteRequest request) {
        UserDeleteResponse response = new UserDeleteResponse();
        
        try {
            // 参数校验
            if (request.getUserId() == null && 
                (StringUtils.isEmpty(request.getThirdPartyType()) || StringUtils.isEmpty(request.getThirdPartyUserId()))) {
                response.setSuccess(false);
                response.setErrorMessage("用户ID或第三方平台信息不能为空");
                return response;
            }
            
            Long userId = request.getUserId();
            
            // 如果提供了第三方平台信息，先查询对应的用户ID
            if (userId == null && !StringUtils.isEmpty(request.getThirdPartyType()) && !StringUtils.isEmpty(request.getThirdPartyUserId())) {
                LambdaQueryWrapper<UserThirdParty> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserThirdParty::getThirdPartyType, request.getThirdPartyType())
                        .eq(UserThirdParty::getThirdPartyUserId, request.getThirdPartyUserId())
                        .eq(UserThirdParty::getIsDel, 0);
                
                UserThirdParty userThirdParty = userThirdPartyMapper.selectOne(queryWrapper);
                
                if (userThirdParty == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("未找到对应的用户");
                    return response;
                }
                
                userId = userThirdParty.getUserId();
            }
            
            // 逻辑删除用户
            if (userId != null) {
                Date now = new Date();
                
                // 1. 删除用户基本信息
                LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(User::getUserId, userId);
                
                User user = new User();
                user.setIsDel(1);
                user.setGmtModified(now);
                user.setUpdaterId(request.getOperatorId());
                user.setUpdaterName(request.getOperatorName());
                
                userMapper.update(user, userWrapper);
                
                // 2. 删除用户第三方关联信息
                LambdaQueryWrapper<UserThirdParty> thirdPartyWrapper = new LambdaQueryWrapper<>();
                thirdPartyWrapper.eq(UserThirdParty::getUserId, userId);
                
                UserThirdParty userThirdParty = new UserThirdParty();
                userThirdParty.setIsDel(1);
                userThirdParty.setGmtModified(now);
                userThirdParty.setUpdaterId(request.getOperatorId());
                userThirdParty.setUpdaterName(request.getOperatorName());
                
                userThirdPartyMapper.update(userThirdParty, thirdPartyWrapper);
                
                response.setSuccess(true);
            } else {
                response.setSuccess(false);
                response.setErrorMessage("未找到对应的用户");
            }
            
            return response;
        } catch (Exception e) {
            log.error("删除用户异常", e);
            response.setSuccess(false);
            response.setErrorMessage("删除用户失败: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * 生成随机用户ID
     * 确保大于6位小于20位，并且是随机的
     */
    private Long generateRandomUserId() {
        // 从批量生成的ID池中获取一个ID
        List<Long> userIds = generateBatchRandomUserIds(1);
        return userIds.get(0);
    }

    /**
     * 批量生成随机用户ID
     * 确保大于6位小于20位，并且是随机的
     * @param count 需要生成的ID数量
     * @return 生成的用户ID列表
     */
    private List<Long> generateBatchRandomUserIds(int count) {
        Random random = new Random();
        
        // 生成足够多的候选ID (多生成一些以应对可能的冲突)
        Set<Long> candidateIds = new HashSet<>();
        int candidateCount = count * 2; // 生成两倍数量的候选ID
        
        while (candidateIds.size() < candidateCount) {
            // 生成8位随机ID (10000000 - 99999999)
            long userId = 100000L + random.nextInt(90000000);
            candidateIds.add(userId);
        }
        
        // 批量检查这些ID是否已存在
        List<Long> candidateIdList = new ArrayList<>(candidateIds);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getUserId, candidateIdList);
        
        // 查询已存在的用户ID
        List<User> existingUsers = userMapper.selectList(queryWrapper);
        Set<Long> existingUserIds = existingUsers.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());
        
        // 过滤掉已存在的ID
        List<Long> availableIds = candidateIdList.stream()
                .filter(id -> !existingUserIds.contains(id))
                .collect(Collectors.toList());
        
        // 从可用ID中选择需要的数量
        if (availableIds.size() < count) {
            // 如果可用ID不足，递归调用生成更多ID
            return generateBatchRandomUserIds(count);
        } else {
            // 返回需要的数量的ID
            return availableIds.subList(0, count);
        }
    }

    /**
     * 批量创建用户
     * 1.批量创建接口需要根据三方关系表，判断用户是否存在，如果存在直接返回，不存在就创建
     * 2.user中的 user_id 需要大于6位以上，小于20位以下，并且需要是随机的。不能是顺序的。且不能重复
     *
     * @param requests       创建用户请求对象列表
     * @param thirdPartyType 第三方平台类型
     * @return 创建用户响应对象列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UserCreateResponse> batchCreateUsers(List<UserCreateRequest> requests, String thirdPartyType) {
        List<UserCreateResponse> responseList = new ArrayList<>();
        
        if (CollectionUtils.isEmpty(requests)) {
            return responseList;
        }
        
        try {
            // 1. 收集所有第三方用户ID和类型
            List<String> thirdPartyUserIds = requests.stream()
                    .map(UserCreateRequest::getThirdPartyUserId)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());

            // 2. 批量查询已存在的用户
            Map<String, UserThirdParty> existingUserMap = new HashMap<>();
            
            if (CollectionUtils.isEmpty(thirdPartyUserIds)) {
                return responseList;
            }
            LambdaQueryWrapper<UserThirdParty> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(UserThirdParty::getThirdPartyUserId, thirdPartyUserIds)
                    .eq(UserThirdParty::getThirdPartyType, thirdPartyType)
                    .eq(UserThirdParty::getIsDel, 0);

            List<UserThirdParty> existingUsers = userThirdPartyMapper.selectList(queryWrapper);

            // 构建映射：thirdPartyType_thirdPartyUserId -> UserThirdParty
            for (UserThirdParty userThirdParty : existingUsers) {
                String key = userThirdParty.getThirdPartyType() + "_" + userThirdParty.getThirdPartyUserId();
                existingUserMap.put(key, userThirdParty);
            }

            // 3. 处理每个请求
            List<User> toInsertUsers = new ArrayList<>();
            List<UserThirdParty> toInsertThirdParties = new ArrayList<>();
            Date now = new Date();
            
            // 统计需要创建的新用户数量
            int newUserCount = 0;
            for (UserCreateRequest request : requests) {
                if (!StringUtils.isEmpty(request.getThirdPartyType()) && !StringUtils.isEmpty(request.getThirdPartyUserId())) {
                    String key = request.getThirdPartyType() + "_" + request.getThirdPartyUserId();
                    if (!existingUserMap.containsKey(key)) {
                        newUserCount++;
                    }
                }
            }
            
            // 批量生成所需的用户ID
            List<Long> userIds = new ArrayList<>();
            if (newUserCount > 0) {
                userIds = generateBatchRandomUserIds(newUserCount);
            }
            
            int idIndex = 0;
            for (UserCreateRequest request : requests) {
                UserCreateResponse response = new UserCreateResponse();
                
                // 参数校验
                if (StringUtils.isEmpty(request.getThirdPartyType()) || StringUtils.isEmpty(request.getThirdPartyUserId())) {
                    response.setSuccess(false);
                    response.setErrorMessage("第三方平台类型和用户ID不能为空");
                    responseList.add(response);
                    continue;
                }
                
                // 查找是否已存在
                String key = request.getThirdPartyType() + "_" + request.getThirdPartyUserId();
                UserThirdParty existingUser = existingUserMap.get(key);
                
                if (existingUser != null) {
                    // 用户已存在，直接返回
                    response.setUserId(existingUser.getUserId());
                    response.setIsNewUser(false);
                    response.setSuccess(true);
                } else {
                    // 用户不存在，创建新用户
                    // 从批量生成的ID中获取一个
                    Long userId = userIds.get(idIndex++);
                    
                    // 创建用户基本信息
                    User user = new User();
                    user.setUserId(userId);
                    user.setEmployeeNo(request.getEmployeeNo());
                    user.setNickname(request.getNickname());
                    user.setAvatar(request.getAvatar());
                    user.setGmtCreate(now);
                    user.setGmtModified(now);
                    user.setIsDel(0);
                    
                    // 创建用户第三方关联信息
                    UserThirdParty userThirdParty = new UserThirdParty();
                    userThirdParty.setUserId(userId);
                    userThirdParty.setThirdPartyType(request.getThirdPartyType());
                    userThirdParty.setThirdPartyUserId(request.getThirdPartyUserId());
                    userThirdParty.setThirdPartyUsername(request.getThirdPartyUsername());
                    userThirdParty.setAttributes(request.getAttributes());
                    userThirdParty.setGmtCreate(now);
                    userThirdParty.setGmtModified(now);
                    userThirdParty.setIsDel(0);
                    
                    // 添加到批量插入列表
                    toInsertUsers.add(user);
                    toInsertThirdParties.add(userThirdParty);
                    
                    // 设置响应
                    response.setUserId(userId);
                    response.setIsNewUser(true);
                    response.setSuccess(true);
                    
                    // 添加到映射，避免重复创建
                    existingUserMap.put(key, userThirdParty);
                }
                
                responseList.add(response);
            }
            
            // 4. 批量插入新用户
            if (!toInsertUsers.isEmpty()) {
                userMapper.batchInsert(toInsertUsers);
            }
            
            // 5. 批量插入用户第三方关联
            if (!toInsertThirdParties.isEmpty()) {
                userThirdPartyMapper.batchInsert(toInsertThirdParties);
            }
            
            return responseList;
        } catch (Exception e) {
            log.error("批量创建用户异常", e);
            throw e;
        }
    }
}
