package com.learn.service.dingtalk.impl;

import com.dingtalk.api.response.OapiRoleListResponse;
import com.dingtalk.api.response.OapiRoleSimplelistResponse;
import com.learn.service.dingtalk.common.DingTalkApiClient;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.dingtalk.DingTalkRoleSyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理角色同步逻辑,
 * 实现以下功能:
 * 1. 同步钉钉角色到系统角色
 * 2. 同步钉钉角色用户关系
 */
@Service
@Slf4j
public class DingTalkRoleSyncServiceImpl implements DingTalkRoleSyncService {

    @Autowired
    private OrgRoleMapper orgRoleMapper;

    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    @Autowired
    private OrgRoleUserMapper orgRoleUserMapper;

    @Autowired
    private DingTalkApiClient dingTalkApiClient;

    // 外部系统来源标识
    private static final String EXTERNAL_SOURCE = "dingtalk";
    private static final String EXTERNAL_SOURCE_GROUP = "dingtalk_group";

    /**
     * 角色信息包装类，包含角色信息和类型信息
     */
    private static class RoleInfo {
        private Long roleId;
        private String name;
        private boolean isGroup;

        public RoleInfo(Long roleId, String name, boolean isGroup) {
            this.roleId = roleId;
            this.name = name;
            this.isGroup = isGroup;
        }

        public Long getRoleId() {
            return roleId;
        }

        public String getName() {
            return name;
        }

        public boolean isGroup() {
            return isGroup;
        }
    }

    /**
     * 钉钉接口文档: https://open.dingtalk.com/document/isvapp/obtains-a-list-of-enterprise-roles
     * 同步钉钉角色
     * <p>
     * 1. 拉取钉钉所有角色数据，其中会返回2类角色: 1. 角色group 2.角色 需要同步到 org_role 中，同时支持前面2种类型。
     * 2. 批量处理钉钉角色，若本地不存在则新增，存在则更新
     * 3. 本地有但钉钉无的角色，做逻辑删除
     * 4. 同步需要支持 角色group 是 角色的父节点。用于后续返回树状接口。parent_id 不是外部id。需要转换为内部的。所以需要优先插入group。再插入role。或者更新role的parent_id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRoles() {
        // 1. 拉取钉钉角色列表
        List<RoleInfo> dingRoleList = getRoleInfos();
        if (CollectionUtils.isEmpty(dingRoleList)) {
            log.info("钉钉角色列表为空，无需同步");
            return;
        }
        log.info("获取到钉钉角色列表，共 {} 个角色", dingRoleList.size());

        // 2. 构建本地角色Map<external_id, OrgRole>
        List<OrgRole> localRoleList = orgRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgRole>()
                        .in(OrgRole::getExternalSource, Arrays.asList(EXTERNAL_SOURCE, EXTERNAL_SOURCE_GROUP))
                        .in(OrgRole::getExternalId, dingRoleList.stream().map(RoleInfo::getRoleId).collect(Collectors.toList()))
        );
        Map<String, OrgRole> localRoleMap = localRoleList.stream()
                .collect(Collectors.toMap(OrgRole::getExternalId, role -> role));
        log.info("获取到本地角色列表，共 {} 个角色", localRoleList.size());

        // 记录已处理的钉钉角色ID
        Set<String> processedDingRoleIds = new HashSet<>();
        
        // 3. 先处理角色组，确保父节点先存在
        Map<Long, Long> groupIdMapping = new HashMap<>(); // 钉钉角色组ID -> 本地角色组ID的映射
        Date now = new Date();

        // 筛选出所有角色组
        List<RoleInfo> roleGroups = dingRoleList.stream()
                .filter(RoleInfo::isGroup)
                .collect(Collectors.toList());
        
        // 准备批量处理的集合
        List<OrgRole> toInsertGroupList = new ArrayList<>();
        List<OrgRole> toUpdateGroupList = new ArrayList<>();

        // 处理所有角色组
        for (RoleInfo roleGroup : roleGroups) {
            String dingRoleId = String.valueOf(roleGroup.getRoleId());
            processedDingRoleIds.add(dingRoleId);

            OrgRole localRole = localRoleMap.get(dingRoleId);

            if (localRole == null) {
                // 新增角色组
                localRole = new OrgRole();
                localRole.setRoleName(roleGroup.getName());
                localRole.setRoleDescription("钉钉同步角色组");
                localRole.setExternalId(dingRoleId);
                localRole.setExternalSource(EXTERNAL_SOURCE_GROUP);
                localRole.setParentId(0L); // 角色组作为顶级节点
                localRole.setGmtCreate(now);
                localRole.setGmtModified(now);
                localRole.setIsDel(0);

                toInsertGroupList.add(localRole);
            } else {
                // 更新角色组
                localRole.setRoleName(roleGroup.getName());
                localRole.setExternalSource(EXTERNAL_SOURCE_GROUP);
                localRole.setParentId(0L); // 确保角色组是顶级节点
                localRole.setGmtModified(now);
                localRole.setIsDel(0); // 确保未被删除

                toUpdateGroupList.add(localRole);
            }
        }
        
        // 批量插入角色组
        if (!toInsertGroupList.isEmpty()) {
            orgRoleMapper.batchInsert(toInsertGroupList);
            log.info("批量插入角色组，数量: {}", toInsertGroupList.size());
            
            // 更新本地角色映射
            for (OrgRole role : toInsertGroupList) {
                localRoleMap.put(role.getExternalId(), role);
                groupIdMapping.put(Long.valueOf(role.getExternalId()), role.getId());
            }
        }
        
        // 批量更新角色组
        if (!toUpdateGroupList.isEmpty()) {
            orgRoleMapper.updateBatchById(toUpdateGroupList);
            log.info("批量更新角色组，数量: {}", toUpdateGroupList.size());
            
            // 更新映射关系
            for (OrgRole role : toUpdateGroupList) {
                groupIdMapping.put(Long.valueOf(role.getExternalId()), role.getId());
            }
        }

        // 4. 处理普通角色，设置正确的parent_id
        // 筛选出所有普通角色
        List<RoleInfo> normalRoles = dingRoleList.stream()
                .filter(role -> !role.isGroup())
                .collect(Collectors.toList());
        
        // 准备批量处理的集合
        List<OrgRole> toInsertRoleList = new ArrayList<>();
        List<OrgRole> toUpdateRoleList = new ArrayList<>();

        // 处理所有普通角色
        for (RoleInfo normalRole : normalRoles) {
            String dingRoleId = String.valueOf(normalRole.getRoleId());
            processedDingRoleIds.add(dingRoleId);

            OrgRole localRole = localRoleMap.get(dingRoleId);

            // 查找该角色所属的角色组
            Long parentId = 0L; // 默认为顶级
            Long groupId = findRoleGroupId(normalRole.getRoleId(), roleGroups);
            if (groupId != null && groupIdMapping.containsKey(groupId)) {
                parentId = groupIdMapping.get(groupId);
            }

            if (localRole == null) {
                // 新增普通角色
                localRole = new OrgRole();
                localRole.setRoleName(normalRole.getName());
                localRole.setRoleDescription("钉钉同步角色");
                localRole.setExternalId(dingRoleId);
                localRole.setExternalSource(EXTERNAL_SOURCE);
                localRole.setParentId(parentId); // 设置父节点ID
                localRole.setGmtCreate(now);
                localRole.setGmtModified(now);
                localRole.setIsDel(0);

                toInsertRoleList.add(localRole);
            } else {
                // 更新普通角色
                localRole.setRoleName(normalRole.getName());
                localRole.setExternalSource(EXTERNAL_SOURCE);
                localRole.setParentId(parentId); // 更新父节点ID
                localRole.setGmtModified(now);
                localRole.setIsDel(0); // 确保未被删除
                
                toUpdateRoleList.add(localRole);
            }
        }
        
        // 批量插入普通角色
        if (!toInsertRoleList.isEmpty()) {
            orgRoleMapper.batchInsert(toInsertRoleList);
            log.info("批量插入普通角色，数量: {}", toInsertRoleList.size());
            
            // 更新本地角色映射
            for (OrgRole role : toInsertRoleList) {
                localRoleMap.put(role.getExternalId(), role);
            }
        }
        
        // 批量更新普通角色
        if (!toUpdateRoleList.isEmpty()) {
            orgRoleMapper.updateBatchById(toUpdateRoleList);
            log.info("批量更新普通角色，数量: {}", toUpdateRoleList.size());
        }

        // 5. 本地有但钉钉无的角色，做逻辑删除
        // 计算基准时间：当前时间往前一小时
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date baseTime = calendar.getTime();

        // 使用单条SQL批量删除过期角色
        int deletedCount = orgRoleMapper.batchDeleteByUpdateTime(baseTime, now, processedDingRoleIds);
        if (deletedCount > 0) {
            log.info("批量删除角色，数量: {}", deletedCount);
        }

        log.info("角色同步完成，总数: {}", processedDingRoleIds.size());
    }

    /**
     * 查找角色所属的角色组ID
     *
     * @param roleId     角色ID
     * @param roleGroups 所有角色组
     * @return 角色组ID，如果找不到则返回null
     */
    private Long findRoleGroupId(Long roleId, List<RoleInfo> roleGroups) {
        // 这里需要根据钉钉API的返回结构来实现
        // 由于钉钉API返回的角色组中包含了该组下的所有角色，我们可以遍历所有角色组，查找包含指定角色的组
        for (RoleInfo roleGroup : roleGroups) {
            // 获取角色组下的所有角色ID
            List<Long> groupRoleIds = getGroupRoleIds(roleGroup.getRoleId());
            if (groupRoleIds.contains(roleId)) {
                return roleGroup.getRoleId();
            }
        }
        return null;
    }

    // 存储角色组与其包含的角色ID的映射关系
    private Map<Long, List<Long>> groupRolesMap = new HashMap<>();

    /**
     * 获取角色组下的所有角色ID
     *
     * @param groupId 角色组ID
     * @return 角色ID列表
     */
    private List<Long> getGroupRoleIds(Long groupId) {
        // 从缓存的映射关系中获取角色组下的所有角色ID
        return groupRolesMap.getOrDefault(groupId, new ArrayList<>());
    }

    /**
     * 获取钉钉角色信息列表，包括角色组和角色
     *
     * @return 角色信息列表
     */
    private List<RoleInfo> getRoleInfos() {
        List<RoleInfo> allRoleInfos = new ArrayList<>();
        // 清空之前的映射关系
        groupRolesMap.clear();

        // 获取角色列表，分页处理
        long offset = 0;
        long size = 200; // 钉钉接口最大支持200
        boolean hasMore = true;

        while (hasMore) {
            OapiRoleListResponse response = dingTalkApiClient.listRoles(offset, size);


            if (response.getResult() != null && response.getResult().getList() != null) {
                List<OapiRoleListResponse.OpenRoleGroup> roleGroups = response.getResult().getList();

                // 如果没有数据，说明已经取完
                if (CollectionUtils.isEmpty(roleGroups)) {
                    hasMore = false;
                    continue;
                }

                // 处理角色组和角色
                for (OapiRoleListResponse.OpenRoleGroup roleGroup : roleGroups) {
                    Long groupId = roleGroup.getGroupId();
                    // 添加角色组本身作为一个角色
                    allRoleInfos.add(new RoleInfo(groupId, roleGroup.getName(), true));

                    // 添加角色组下的所有角色，并建立映射关系
                    List<Long> roleIds = new ArrayList<>();
                    if (roleGroup.getRoles() != null) {
                        for (OapiRoleListResponse.OpenRole role : roleGroup.getRoles()) {
                            Long roleId = role.getId();
                            allRoleInfos.add(new RoleInfo(roleId, role.getName(), false));
                            roleIds.add(roleId);
                        }
                    }
                    // 存储角色组与其包含的角色ID的映射关系
                    groupRolesMap.put(groupId, roleIds);
                }

                // 更新偏移量
                offset += roleGroups.size();

                // 如果本次获取的数量小于size，说明已经取完
                if (roleGroups.size() < size) {
                    hasMore = false;
                }
            } else {
                hasMore = false;
            }
        }

        return allRoleInfos;
    }

    /**
     * 批量更新角色
     *
     * @param list 角色列表
     * @return 更新数量
     */
    private int updateBatchOrgRoles(List<OrgRole> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }

        // 使用自定义SQL实现批量更新
        return orgRoleMapper.updateBatchById(list);
    }

    /**
     * 钉钉接口文档: https://open.dingtalk.com/document/isvapp/obtain-the-list-of-employees-of-a-role
     * 同步角色人员关系
     * 1. 批量获取钉钉所有角色人员关系，并且同步到 org_role_user 表中
     * 2. 用户理论上都已经创建，该任务跑在部门用户任务后面
     * 3. 批量处理角色用户关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRoleUsers() {
        // 获取本地角色列表（钉钉来源，包括角色组和角色）
        List<OrgRole> localRoleList = orgRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgRole>()
                        .in(OrgRole::getExternalSource, Arrays.asList(EXTERNAL_SOURCE, EXTERNAL_SOURCE_GROUP))
                        .eq(OrgRole::getIsDel, 0)
        );
        if (CollectionUtils.isEmpty(localRoleList)) {
            log.error("本地钉钉来源角色列表为空，无法同步角色用户关系");
            return;
        }

        // 1. 批量获取所有钉钉用户ID
        Map<Long, List<String>> allDingRoleUserMap = new HashMap<>(); // 角色ID -> 钉钉用户ID列表
        Set<String> allDingUserIds = new HashSet<>(); // 所有钉钉用户ID
        
        // 收集所有角色的用户ID
        for (OrgRole role : localRoleList) {
            String roleId = role.getExternalId();
            List<String> roleUserIds = getRoleUserIds(Long.valueOf(roleId));
            if (!CollectionUtils.isEmpty(roleUserIds)) {
                allDingRoleUserMap.put(role.getId(), roleUserIds);
                allDingUserIds.addAll(roleUserIds);
            }
        }
        
        // 2. 批量查询所有用户的映射关系
        Map<String, Long> thirdPartyUserIdMap = new HashMap<>(); // 钉钉用户ID -> 系统用户ID
        
        if (!allDingUserIds.isEmpty()) {
            // 分批查询，每批100个
            List<String> dingUserIdList = new ArrayList<>(allDingUserIds);
            int batchSize = 100;
            
            for (int i = 0; i < dingUserIdList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, dingUserIdList.size());
                List<String> batchUserIds = dingUserIdList.subList(i, endIndex);
                
                // 批量查询用户映射关系
                List<UserThirdParty> userThirdPartyList = userThirdPartyMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserThirdParty>()
                                .eq(UserThirdParty::getThirdPartyType, EXTERNAL_SOURCE)
                                .in(UserThirdParty::getThirdPartyUserId, batchUserIds)
                                .eq(UserThirdParty::getIsDel, 0)
                );
                
                // 收集映射关系
                for (UserThirdParty userThirdParty : userThirdPartyList) {
                    thirdPartyUserIdMap.put(userThirdParty.getThirdPartyUserId(), userThirdParty.getUserId());
                }
            }
        }

        // 3. 准备批量处理的数据
        List<OrgRoleUser> toInsertList = new ArrayList<>();
        Set<String> processedRelations = new HashSet<>();
        Date now = new Date();

        // 构建角色用户关系
        for (Map.Entry<Long, List<String>> entry : allDingRoleUserMap.entrySet()) {
            Long orgRoleId = entry.getKey();
            List<String> dingUserIds = entry.getValue();

            for (String dingUserId : dingUserIds) {
                Long userId = thirdPartyUserIdMap.get(dingUserId);
                if (userId == null) {
                    log.warn("未找到钉钉用户{}对应的本地用户，跳过", dingUserId);
                    continue;
                }

                String relationKey = orgRoleId + "_" + userId;
                processedRelations.add(relationKey);

                // 新增关系，添加到待插入列表
                OrgRoleUser roleUser = new OrgRoleUser();
                roleUser.setOrgRoleId(orgRoleId);
                roleUser.setUserId(userId);
                roleUser.setGmtCreate(now);
                roleUser.setGmtModified(now);
                roleUser.setIsDel(0);
                toInsertList.add(roleUser);
            }
        }

        // 4. 批量插入角色用户关系
        if (!toInsertList.isEmpty()) {
            // 分批处理，每批500个
            int insertBatchSize = 500;
            for (int i = 0; i < toInsertList.size(); i += insertBatchSize) {
                int endIndex = Math.min(i + insertBatchSize, toInsertList.size());
                List<OrgRoleUser> batchList = toInsertList.subList(i, endIndex);
                
                orgRoleUserMapper.insertOrUpdateBatch(batchList);
                log.info("批量插入角色用户关系，批次: {}/{}, 数量: {}", 
                         (i / insertBatchSize) + 1, 
                         (toInsertList.size() + insertBatchSize - 1) / insertBatchSize, 
                         batchList.size());
            }
        }

        // 5. 删除过期关系
        // 计算基准时间：当前时间往前一小时
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date baseTime = calendar.getTime();

        // 批量删除过期关系
        int deletedCount = orgRoleUserMapper.batchDeleteByUpdateTime(baseTime, now, processedRelations);
        if (deletedCount > 0) {
            log.info("批量删除角色用户关系，数量: {}", deletedCount);
        }

        log.info("角色用户关系同步完成，总数: {}", processedRelations.size());
    }

    /**
     * 获取角色的用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    private List<String> getRoleUserIds(Long roleId) {
        List<String> userIds = new ArrayList<>();

        // 分页获取角色用户列表
        long offset = 0;
        long size = 200; // 钉钉接口最大支持200
        boolean hasMore = true;

        while (hasMore) {
            OapiRoleSimplelistResponse response = dingTalkApiClient.listRoleUsers(roleId, offset, size);
            if (response == null || !response.isSuccess()) {
                log.error("获取钉钉角色用户列表失败, roleId={}", roleId);
                return userIds;
            }

            if (response.getResult() != null && response.getResult().getList() != null) {
                List<OapiRoleSimplelistResponse.OpenEmpSimple> users = response.getResult().getList();

                // 如果没有数据，说明已经取完
                if (CollectionUtils.isEmpty(users)) {
                    hasMore = false;
                    continue;
                }

                // 添加用户ID
                for (OapiRoleSimplelistResponse.OpenEmpSimple user : users) {
                    userIds.add(user.getUserid());
                }

                // 更新偏移量
                Long nextCursor = response.getResult().getNextCursor();
                if (nextCursor != null && nextCursor > 0) {
                    offset = nextCursor;
                } else {
                    hasMore = false;
                }
            } else {
                hasMore = false;
            }
        }

        return userIds;
    }
}

