package com.learn.service.dingtalk.impl;

import com.alibaba.fastjson.JSON;
import com.learn.service.dingtalk.DingTalkDepartmentSyncService;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.learn.service.dingtalk.common.DingTalkApiClient;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.user.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理部门同步逻辑,
 * 实现以下功能:
 * 1. 同步钉钉部门到系统部门
 * 2. 同步钉钉部门用户关系
 */
@Service
@Slf4j
public class DingTalkDepartmentSyncServiceImpl implements DingTalkDepartmentSyncService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private DingTalkApiClient dingTalkApiClient;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    // 外部系统来源标识
    private static final String EXTERNAL_SOURCE = "dingtalk";

    /**
     * 部门信息包装类
     */
    @Data
    private static class DepartmentInfo {
        private Long deptId;
        private String name;
        private Long parentId;

        public DepartmentInfo(Long deptId, String name, Long parentId) {
            this.deptId = deptId;
            this.name = name;
            this.parentId = parentId;
        }

    }

    /**
     * 用户信息包装类
     */
    @Data
    private static class UserInfo {
        private String userId;
        private String name;
        private String jobNumber;
        private String avatar;
        private String unionId;

        public UserInfo(String userId, String unionId, String name, String jobNumber, String avatar) {
            this.userId = userId;
            this.unionId = unionId;
            this.name = name;
            this.jobNumber = jobNumber;
            this.avatar = avatar;
        }

    }

    /**
     * 钉钉接口文档: https://open.dingtalk.com/document/isvapp/get-department-list
     * 同步钉钉部门
     * 1. 拉取钉钉所有部门数据
     * 2. 按层级批量处理部门数据，确保父部门先于子部门处理
     * 3. 本地有但钉钉无的部门，做逻辑删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDepartments() {
        try {
            // 1. 拉取钉钉部门列表
            List<DepartmentInfo> dingDeptList = getAllDepartments();
            if (CollectionUtils.isEmpty(dingDeptList)) {
                log.info("钉钉部门列表为空，无需同步");
                return;
            }
            log.info("获取到钉钉部门列表，共 {} 个部门", dingDeptList.size());

            // 2. 构建本地部门Map<external_id, Department>
            List<Department> localDeptList = departmentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                            .eq(Department::getExternalSource, EXTERNAL_SOURCE)
                            .in(Department::getExternalId, dingDeptList.stream().map(DepartmentInfo::getDeptId).collect(Collectors.toList()))
            );
            Map<String, Department> localDeptMap = localDeptList.stream()
                    .collect(Collectors.toMap(Department::getExternalId, dept -> dept));
            log.info("获取到本地部门列表，共 {} 个部门", localDeptList.size());

            // 记录已处理的钉钉部门ID
            Set<String> processedDingDeptIds = new HashSet<>();

            // 3. 按层级处理部门，先处理顶级部门，再处理子部门
            // 按层级对部门进行分组
            Map<Long, List<DepartmentInfo>> levelDeptMap = new HashMap<>();

            // 筛选出顶级部门（parentId=1）
            List<DepartmentInfo> rootDepts = dingDeptList.stream()
                    .filter(dept -> dept.getParentId().equals(1L))
                    .collect(Collectors.toList());
            levelDeptMap.put(1L, rootDepts);

            // 构建层级关系
            buildDepartmentLevelMap(dingDeptList, levelDeptMap);

            // 按层级顺序处理部门
            processDepartmentsByLevel(levelDeptMap, localDeptMap, processedDingDeptIds);

            // 4. 对比本地和钉钉数据，做逻辑删除
            // 计算基准时间：当前时间往前一小时
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date baseTime = calendar.getTime();
            Date now = new Date();

            // 使用单条SQL批量删除过期部门
            Map<String, Object> params = new HashMap<>();
            params.put("baseTime", baseTime);
            params.put("now", now);
            params.put("notInExternalIds", processedDingDeptIds);
            params.put("externalSource", EXTERNAL_SOURCE);
            int deletedCount = departmentMapper.batchDeleteByUpdateTime(params);
            if (deletedCount > 0) {
                log.info("批量删除部门，数量: {}", deletedCount);
            }

            log.info("部门同步完成，总数: {}", processedDingDeptIds.size());
        } catch (Exception e) {
            log.error("同步钉钉部门异常", e);
            throw e;
        }
    }

    /**
     * 构建部门层级关系映射
     *
     * @param allDepts     所有部门
     * @param levelDeptMap 层级部门映射
     */
    private void buildDepartmentLevelMap(List<DepartmentInfo> allDepts, Map<Long, List<DepartmentInfo>> levelDeptMap) {
        // 从顶级部门开始，逐层构建
        Long currentLevel = 1L;

        while (true) {
            List<DepartmentInfo> currentLevelDepts = levelDeptMap.get(currentLevel);
            if (CollectionUtils.isEmpty(currentLevelDepts)) {
                break;
            }

            // 收集当前层级部门的ID
            Set<Long> parentIds = currentLevelDepts.stream()
                    .map(DepartmentInfo::getDeptId)
                    .collect(Collectors.toSet());

            // 查找下一层级的部门
            List<DepartmentInfo> nextLevelDepts = allDepts.stream()
                    .filter(dept -> parentIds.contains(dept.getParentId()) && !dept.getDeptId().equals(dept.getParentId()))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(nextLevelDepts)) {
                break;
            }

            // 添加到层级映射
            levelDeptMap.put(currentLevel + 1, nextLevelDepts);
            currentLevel++;
        }
    }

    /**
     * 按层级处理部门
     *
     * @param levelDeptMap         层级部门映射
     * @param localDeptMap         本地部门映射
     * @param processedDingDeptIds 已处理的钉钉部门ID
     */
    private void processDepartmentsByLevel(Map<Long, List<DepartmentInfo>> levelDeptMap,
                                           Map<String, Department> localDeptMap,
                                           Set<String> processedDingDeptIds) {
        Date now = new Date();

        // 按层级顺序处理
        for (Long level = 1L; level <= levelDeptMap.size(); level++) {
            List<DepartmentInfo> deptList = levelDeptMap.get(level);
            if (CollectionUtils.isEmpty(deptList)) {
                continue;
            }

            log.info("处理第 {} 层级部门，数量: {}", level, deptList.size());

            // 批量处理同一层级的部门
            List<Department> toInsertList = new ArrayList<>();
            List<Department> toUpdateList = new ArrayList<>();

            for (DepartmentInfo dingDept : deptList) {
                String dingDeptId = String.valueOf(dingDept.getDeptId());
                processedDingDeptIds.add(dingDeptId);

                Department localDept = localDeptMap.get(dingDeptId);

                // 计算父部门ID
                Long parentId = 0L;
                if (!dingDept.getParentId().equals(1L)) {
                    // 非顶级部门，需要找到父部门
                    String parentDingId = String.valueOf(dingDept.getParentId());
                    Department parentDept = localDeptMap.get(parentDingId);
                    if (parentDept != null) {
                        parentId = parentDept.getId();
                    }
                }

                int deptLevel = level.intValue();

                if (localDept == null) {
                    // 新增部门
                    localDept = new Department();
                    localDept.setDepartmentName(dingDept.getName());
                    localDept.setParentId(parentId);
                    localDept.setDepartmentLevel(deptLevel);
                    localDept.setSortOrder(0);
                    localDept.setExternalId(dingDeptId);
                    localDept.setExternalSource(EXTERNAL_SOURCE);
                    localDept.setGmtCreate(now);
                    localDept.setGmtModified(now);
                    localDept.setIsDel(0);

                    toInsertList.add(localDept);
                } else {
                    // 更新部门
                    localDept.setDepartmentName(dingDept.getName());
                    localDept.setParentId(parentId);
                    localDept.setDepartmentLevel(deptLevel);
                    localDept.setSortOrder(0);
                    localDept.setGmtModified(now);
                    localDept.setIsDel(0);

                    toUpdateList.add(localDept);
                }
            }

            // 批量插入新部门
            if (!toInsertList.isEmpty()) {
                // 使用批量插入
                departmentMapper.batchInsert(toInsertList);

                // 插入后更新本地映射
                for (Department dept : toInsertList) {
                    localDeptMap.put(dept.getExternalId(), dept);
                    log.debug("部门插入后ID: {}, 名称: {}, 外部ID: {}", dept.getId(), dept.getDepartmentName(), dept.getExternalId());
                }
                log.info("批量插入部门，数量: {}", toInsertList.size());
            }

            // 批量更新部门
            if (!toUpdateList.isEmpty()) {
                // 使用批量更新
                departmentMapper.batchUpdate(toUpdateList);
                log.info("批量更新部门，数量: {}", toUpdateList.size());
            }

            // 更新部门路径
            updateDepartmentPaths(toInsertList, toUpdateList, localDeptMap);
        }
    }

    /**
     * 批量更新部门路径
     *
     * @param insertedDepts 新插入的部门
     * @param updatedDepts  更新的部门
     * @param localDeptMap  本地部门映射
     */
    private void updateDepartmentPaths(List<Department> insertedDepts,
                                       List<Department> updatedDepts,
                                       Map<String, Department> localDeptMap) {
        List<Department> toUpdatePathList = new ArrayList<>();
        toUpdatePathList.addAll(insertedDepts);
        toUpdatePathList.addAll(updatedDepts);

        if (CollectionUtils.isEmpty(toUpdatePathList)) {
            return;
        }

        // 1. 收集所有需要查询的父部门ID
        Set<Long> parentIds = toUpdatePathList.stream()
                .map(Department::getParentId)
                .filter(id -> id > 0) // 只收集非顶级部门的父ID
                .collect(Collectors.toSet());

        // 2. 批量查询所有父部门
        Map<Long, Department> parentDeptMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            // 使用mybatis-plus的批量查询
            List<Department> parentDepts = departmentMapper.selectBatchIds(parentIds);
            if (!CollectionUtils.isEmpty(parentDepts)) {
                parentDeptMap = parentDepts.stream()
                        .collect(Collectors.toMap(Department::getId, dept -> dept));
            }
        }

        // 3. 在内存中计算所有部门的路径
        for (Department dept : toUpdatePathList) {
            String deptPath;
            if (dept.getParentId().equals(0L)) {
                // 顶级部门，路径就是自己的ID
                deptPath = String.valueOf(dept.getId());
            } else {
                // 非顶级部门，从内存中获取父部门路径
                Department parentDept = parentDeptMap.get(dept.getParentId());

                // 如果找不到父部门(异常情况)，直接使用自己的ID作为路径
                if (parentDept == null || StringUtils.isEmpty(parentDept.getDepartmentPath())) {
                    log.warn("未找到父部门或父部门路径为空, 部门ID={}, 父部门ID={}", dept.getId(), dept.getParentId());
                    deptPath = String.valueOf(dept.getId());
                } else {
                    deptPath = parentDept.getDepartmentPath() + "," + dept.getId();
                }
            }

            dept.setDepartmentPath(deptPath);
        }

        // 4. 批量更新路径
        if (!toUpdatePathList.isEmpty()) {
            departmentMapper.batchUpdate(toUpdatePathList);
            log.info("批量更新部门路径，数量: {}", toUpdatePathList.size());
        }
    }

    /**
     * 获取所有钉钉部门信息
     *
     * @return 部门信息列表
     */
    private List<DepartmentInfo> getAllDepartments() {
        List<DepartmentInfo> allDepts = new ArrayList<>();

        // 先获取根部门下的部门
        getDepartmentsRecursively(1L, allDepts);

        return allDepts;
    }

    /**
     * 递归获取部门及其子部门
     *
     * @param parentDeptId 父部门ID
     * @param allDepts     结果列表
     */
    private void getDepartmentsRecursively(Long parentDeptId, List<DepartmentInfo> allDepts) {
        OapiV2DepartmentListsubResponse response = dingTalkApiClient.listDepartments(parentDeptId);
        if (response == null || !response.isSuccess()) {
            log.error("获取钉钉部门列表失败, parentDeptId={}", parentDeptId);
            return;
        }

        List<OapiV2DepartmentListsubResponse.DeptBaseResponse> depts = response.getResult();
        if (CollectionUtils.isEmpty(depts)) {
            return;
        }

        for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : depts) {
            // 添加当前部门
            allDepts.add(new DepartmentInfo(dept.getDeptId(), dept.getName(), dept.getParentId()));

            // 递归获取子部门
            getDepartmentsRecursively(dept.getDeptId(), allDepts);
        }
    }

    /**
     * 钉钉接口文档: https://open.dingtalk.com/document/isvapp/queries-the-complete-information-of-a-department-user
     * 同步部门人员关系
     * 1. 按照100个部门批量处理一次
     * 2. 批量处理用户创建和部门用户关系，已存在的用户也需要更新更新时间
     * 3. 本地有但钉钉无的关系，做逻辑删除，并且删除对应的user和user_third_party
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDepartmentUsers() {
        try {
            // 获取本地部门列表（钉钉来源）
            List<Department> localDeptList = departmentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                            .eq(Department::getExternalSource, EXTERNAL_SOURCE)
                            .eq(Department::getIsDel, 0)
            );
            if (CollectionUtils.isEmpty(localDeptList)) {
                log.error("本地钉钉来源部门列表为空，无法同步部门用户关系");
                return;
            }

            log.info("开始同步钉钉部门用户关系，本地部门总数: {}", localDeptList.size());
            
            // 记录所有处理过的用户ID，用于后续删除不存在的用户
            Set<String> allProcessedDingUserIds = new HashSet<>();
            // 记录所有处理过的部门用户关系
            Set<String> allProcessedRelations = new HashSet<>();
            // 记录需要批量更新的用户ID
            Set<Long> usersToUpdate = new HashSet<>();
            // 记录用户ID和第三方用户ID的映射关系，用于批量更新第三方用户
            Map<Long, String> userThirdPartyToUpdate = new HashMap<>();
            
            // 按照100个部门一批进行处理
            int batchSize = 100;
            for (int i = 0; i < localDeptList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, localDeptList.size());
                List<Department> batchDepts = localDeptList.subList(i, endIndex);
                
                log.info("处理部门批次: {}/{}, 数量: {}", 
                        (i / batchSize) + 1, 
                        (localDeptList.size() + batchSize - 1) / batchSize, 
                        batchDepts.size());
                
                // 1. 收集当前批次部门的钉钉用户信息
                Map<String, UserInfo> batchDingUsers = new HashMap<>(); // 钉钉用户ID -> 用户信息
                Map<Long, List<String>> batchDeptUserIdsMap = new HashMap<>(); // 部门ID -> 用户ID列表
                
                // 获取当前批次部门的用户
                for (Department dept : batchDepts) {
                    String deptId = dept.getExternalId();
                    List<UserInfo> deptUsers = getDepartmentUsers(Long.valueOf(deptId));
                    if (!CollectionUtils.isEmpty(deptUsers)) {
                        // 收集用户信息
                        for (UserInfo userInfo : deptUsers) {
                            batchDingUsers.put(userInfo.getUserId(), userInfo);
                            // 添加到全局处理过的用户ID集合
                            allProcessedDingUserIds.add(userInfo.getUserId());
                        }        // 收集部门用户关系
                        List<String> userIds = deptUsers.stream()
                                .map(UserInfo::getUserId)
                                .collect(Collectors.toList());
                        batchDeptUserIdsMap.put(dept.getId(), userIds);
                    }
                }
                
                if (batchDingUsers.isEmpty()) {
                    log.info("当前批次部门没有用户，跳过处理");
                    continue;
                }
                
                log.info("当前批次部门用户数: {}", batchDingUsers.size());
                
                // 2. 查询当前批次已存在的用户
                List<String> batchDingUserIds = new ArrayList<>(batchDingUsers.keySet());
                Map<String, Long> existingUserIdMap = new HashMap<>(); // 钉钉用户ID -> 系统用户ID
                
                // 分批查询已存在的用户，每批100个
                int userBatchSize = 100;
                for (int j = 0; j < batchDingUserIds.size(); j += userBatchSize) {
                    int userEndIndex = Math.min(j + userBatchSize, batchDingUserIds.size());
                    List<String> userBatch = batchDingUserIds.subList(j, userEndIndex);
                    
                    // 批量查询已存在的用户
                    com.learn.service.dto.UserQueryRequest queryRequest = new com.learn.service.dto.UserQueryRequest();
                    queryRequest.setThirdPartyType(EXTERNAL_SOURCE);
                    queryRequest.setThirdPartyUserIds(userBatch);
                    List<com.learn.service.dto.UserQueryResponse> userResponses = userService.queryUsers(queryRequest);    // 收集已存在的用户ID映射
                    if (!CollectionUtils.isEmpty(userResponses)) {
                        for (com.learn.service.dto.UserQueryResponse user : userResponses) {
                            existingUserIdMap.put(user.getThirdPartyUserId(), user.getUserId());
                        }
                    }
                }
                
                // 3. 处理用户创建和更新
                List<com.learn.service.dto.UserCreateRequest> userCreateRequests = new ArrayList<>();
                Date now = new Date();
                
                for (UserInfo dingUser : batchDingUsers.values()) {
                    if (!existingUserIdMap.containsKey(dingUser.getUserId())) {
                        // 用户不存在，准备创建
                        com.learn.service.dto.UserCreateRequest createRequest = new com.learn.service.dto.UserCreateRequest();
                        createRequest.setThirdPartyType(EXTERNAL_SOURCE);
                        createRequest.setThirdPartyUserId(dingUser.getUserId());
                        createRequest.setThirdPartyUsername(dingUser.getName());
                        createRequest.setEmployeeNo(dingUser.getJobNumber());
                        createRequest.setNickname(dingUser.getName());
                        createRequest.setAvatar(dingUser.getAvatar());
                        createRequest.setAttributes(JSON.toJSONString(dingUser));
                        // 系统操作，使用系统管理员ID
                        createRequest.setCreatorId(0L);
                        createRequest.setCreatorName("系统");
                        
                        userCreateRequests.add(createRequest);
                    } else {
                        // 用户已存在，记录用户ID，稍后批量更新
                        Long userId = existingUserIdMap.get(dingUser.getUserId());
                        // 将用户ID添加到待更新列表中
                        usersToUpdate.add(userId);
                        // 记录用户ID和第三方用户ID的映射关系，用于批量更新第三方用户
                        userThirdPartyToUpdate.put(userId, dingUser.getUserId());
                    }
                }
                
                // 批量创建新用户
                if (!userCreateRequests.isEmpty()) {
                    log.info("开始批量创建新用户，数量: {}", userCreateRequests.size());
                    
                    // 使用批量创建用户接口
                    List<com.learn.service.dto.UserCreateResponse> createResponses = userService.batchCreateUsers(userCreateRequests, EXTERNAL_SOURCE);
                    
                    // 更新用户ID映射
                    for (int j = 0; j < createResponses.size(); j++) {
                        com.learn.service.dto.UserCreateResponse response = createResponses.get(j);
                        com.learn.service.dto.UserCreateRequest request = userCreateRequests.get(j);
                        
                        if (response.getSuccess()) {
                            existingUserIdMap.put(request.getThirdPartyUserId(), response.getUserId());
                        } else {
                            log.error("创建新用户失败: {}, 钉钉用户ID: {}", response.getErrorMessage(), request.getThirdPartyUserId());
                        }
                    }
                    
                    log.info("批量创建新用户完成");
                }
                
                // 批量更新已存在的用户
                if (!usersToUpdate.isEmpty()) {
                    log.info("开始批量更新已存在用户，数量: {}", usersToUpdate.size());
                    
                    // 批量更新用户表
                    List<User> usersToUpdateList = new ArrayList<>();
                    for (Long userId : usersToUpdate) {
                        User user = new User();
                        user.setUserId(userId);
                        usersToUpdateList.add(user);
                    }
                    
                    // 使用批量更新接口
                    if (!usersToUpdateList.isEmpty()) {
                        userMapper.batchUpdateModifyTime(usersToUpdateList);
                    }
                    
                    // 批量更新用户第三方关联表
                    List<UserThirdParty> thirdPartiesToUpdateList = new ArrayList<>();
                    for (Map.Entry<Long, String> entry : userThirdPartyToUpdate.entrySet()) {
                        Long userId = entry.getKey();
                        String thirdPartyUserId = entry.getValue();
                        UserThirdParty userThirdParty = new UserThirdParty();
                        userThirdParty.setUserId(userId);
                        userThirdParty.setThirdPartyUserId(thirdPartyUserId);
                        thirdPartiesToUpdateList.add(userThirdParty);
                    }// 使用批量更新接口
                    if (!thirdPartiesToUpdateList.isEmpty()) {
                        userThirdPartyMapper.batchUpdateModifyTime(thirdPartiesToUpdateList, EXTERNAL_SOURCE);
                    }
                    
                    log.info("批量更新已存在用户完成");// 清空待更新列表，准备下一批次
                    usersToUpdate.clear();
                    userThirdPartyToUpdate.clear();
                }
                
                // 4. 准备部门用户关系数据
                List<DepartmentUser> toInsertList = new ArrayList<>();
                
                // 构建部门用户关系
                for (Map.Entry<Long, List<String>> entry : batchDeptUserIdsMap.entrySet()) {
                    Long departmentId = entry.getKey();
                    List<String> dingUserIds = entry.getValue();
                    for (String dingUserId : dingUserIds) {
                        // 获取系统用户ID
                        Long userId = existingUserIdMap.get(dingUserId);
                        if (userId == null) {
                            // 用户创建失败，跳过
                            continue;
                        }
                        // 记录关系
                        String relationKey = departmentId + "_" + userId;
                        allProcessedRelations.add(relationKey);        // 创建部门用户关系
                        DepartmentUser deptUser = new DepartmentUser();
                        deptUser.setDepartmentId(departmentId);
                        deptUser.setUserId(userId);
                        deptUser.setGmtCreate(now);
                        deptUser.setGmtModified(now);
                        deptUser.setIsDel(0);
                        toInsertList.add(deptUser);
                    }
                }
                
                // 5. 批量插入/更新部门用户关系
                if (!toInsertList.isEmpty()) {
                    // 分批处理，每批500个
                    int insertBatchSize = 500;
                    for (int j = 0; j < toInsertList.size(); j += insertBatchSize) {
                        int insertEndIndex = Math.min(j + insertBatchSize, toInsertList.size());
                        List<DepartmentUser> insertBatch = toInsertList.subList(j, insertEndIndex);
                        
                        departmentUserMapper.insertOrUpdateBatch(insertBatch);
                        log.info("批量插入/更新部门用户关系，批次: {}/{}, 数量: {}",
                                (j / insertBatchSize) + 1,
                                (toInsertList.size() + insertBatchSize - 1) / insertBatchSize,
                                insertBatch.size());
                    }
                }
            }
            
            log.info("所有部门用户关系处理完成，总处理用户数: {}, 总处理关系数: {}", 
                    allProcessedDingUserIds.size(), allProcessedRelations.size());
            
            // 6. 删除过期关系和用户
            // 计算基准时间：当前时间往前一小时
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date baseTime = calendar.getTime();
            
            // 批量删除过期关系
            int deletedRelationCount = departmentUserMapper.batchDeleteByUpdateTime(baseTime);
            if (deletedRelationCount > 0) {
                log.info("批量删除部门用户关系，数量: {}", deletedRelationCount);
            }
            // 批量删除过期关系
            int deletedUserCount = userMapper.batchDeleteByUpdateTime(baseTime);
            if (deletedUserCount > 0) {
                log.info("批量删除用户，数量: {}", deletedUserCount);
            }
            // 批量删除过期关系
            int deletedUserRelationCount = userThirdPartyMapper.batchDeleteByUpdateTime(baseTime);
            if (deletedUserRelationCount > 0) {
                log.info("批量删除三方用户关系，数量: {}", deletedUserRelationCount);
            }
            log.info("部门用户关系同步完成");
        } catch (Exception e) {
            log.error("同步钉钉部门用户关系异常", e);
            throw e;
        }
    }

    /**
     * 获取部门用户列表
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    private List<UserInfo> getDepartmentUsers(Long deptId) {
        List<UserInfo> userList = new ArrayList<>();

        // 分页获取部门用户
        Long cursor = 0L;
        Long size = 100L; // 钉钉接口建议值
        boolean hasMore = true;

        while (hasMore) {
            OapiV2UserListResponse response = dingTalkApiClient.listDepartmentUsers(deptId, cursor, size);

            if (Objects.isNull(response)
                    || Objects.isNull(response.getResult())
                    || CollectionUtils.isEmpty(response.getResult().getList())) {
                hasMore = false;
                break;
            }

            List<OapiV2UserListResponse.ListUserResponse> users = response.getResult().getList();

            // 如果没有数据，说明已经取完
            if (CollectionUtils.isEmpty(users)) {
                hasMore = false;
                continue;
            }

            // 添加用户信息
            for (OapiV2UserListResponse.ListUserResponse user : users) {
                userList.add(new UserInfo(
                        user.getUserid(),
                        user.getUnionid(),
                        user.getName(),
                        user.getJobNumber(),
                        user.getAvatar()
                ));
            }

            // 更新游标
            Long nextCursor = response.getResult().getNextCursor();
            if (nextCursor != null) {
                cursor = nextCursor;
            } else {
                hasMore = false;
            }
        }

        return userList;
    }
}

