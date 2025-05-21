package com.learn.service.common.strategy.impl;

import com.learn.common.dto.CardSendInfo;
import com.learn.common.enums.AssignFinishedTimeTypeEnums;
import com.learn.common.enums.AssignStatusEnums;
import com.learn.common.enums.AssignTypeEnums;
import com.learn.common.enums.RangeTargetTypeEnums;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.AssignRecordsMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.learn.constants.BizConstants.DEFAULT_USER_ID;

/**
 * 自动指派类型策略实现
 */
@Component
@Slf4j
public class AutoAssignTypeStrategy extends AbstractAssignTypeStrategy {
    
    @Autowired
    private AssignRecordsMapper assignRecordsMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public int processAssignRecord(AssignRecords assignRecord, List<CommonRange> rangeList,
                                  AssignFinishedTimeTypeEnums assignFinishedTimeType,
                                  Integer customFinishedDay,
                                  Boolean ifIsNotifyExistUser,
                                  Date notifyUserAfterJoinDate) {
        log.info("处理自动指派记录：{}", assignRecord.getId());
        
        if (CollectionUtils.isEmpty(rangeList)) {
            log.warn("指派范围为空，无法处理");
            return 0;
        }
        
        String bizType = assignRecord.getType();
        Long bizId = assignRecord.getTypeId();
        
        // 根据业务类型和ID查询标题和图片
        CardSendInfo bizInfo = getTypeElementInfo(bizType, bizId);
        bizInfo.setCreateInfo(String.format("%s %s", assignRecord.getCreatorName(),
                DateFormatUtils.format(assignRecord.getGmtCreate(), "yyyy-MM-dd HH:mm:ss")));

        // 收集所有目标ID（部门、角色、用户）
        Map<String, Set<Long>> targetMap = new HashMap<>();
        
        for (CommonRange range : rangeList) {
            String targetType = range.getTargetType();
            List<Long> targetIds = parseTargetIds(range.getTargetIds());
            
            if (!targetMap.containsKey(targetType)) {
                targetMap.put(targetType, new HashSet<>());
            }
            targetMap.get(targetType).addAll(targetIds);
        }
        
        // 收集所有用户ID
        Set<Long> allUserIds = new HashSet<>();
        
        // 处理部门类型的目标
        Set<Long> departmentIds = targetMap.getOrDefault(RangeTargetTypeEnums.DEPARTMENT.getCode(), Collections.emptySet());
        if (!departmentIds.isEmpty()) {
            List<Long> departmentUserIds = getDepartmentUsers(new ArrayList<>(departmentIds));
            allUserIds.addAll(departmentUserIds);
        }
        
        // 处理角色类型的目标
        Set<Long> roleIds = targetMap.getOrDefault(RangeTargetTypeEnums.ROLE.getCode(), Collections.emptySet());
        if (!roleIds.isEmpty()) {
            List<Long> roleUserIds = getRoleUsers(new ArrayList<>(roleIds));
            allUserIds.addAll(roleUserIds);
        }
        
        // 处理用户类型的目标
        Set<Long> directUserIds = targetMap.getOrDefault(RangeTargetTypeEnums.USER.getCode(), Collections.emptySet());
        allUserIds.addAll(directUserIds);

        
        // 获取可见范围内的用户
        Set<Long> visibleUserIds = getVisibleUserIds(bizId, bizType);
        
        // 获取已存在的用户
        List<Long> existingUserIds = getExistingUserIds(bizId, bizType);
        
        // 获取指派记录的修改时间
        Date assignRecordModifiedTime = assignRecord.getGmtModified();
        if (assignRecordModifiedTime == null) {
            assignRecordModifiedTime = assignRecord.getGmtCreate();
        }
        
        // 根据入职时间过滤用户
        List<Long> filteredUserIds = filterUsersByJoinDate(new ArrayList<>(allUserIds), 
                ifIsNotifyExistUser, notifyUserAfterJoinDate, assignRecordModifiedTime);
        
        // 自动指派：排除已经通知过的用户，只追加新用户
        // 分类处理用户
        List<Long> newUserIds = new ArrayList<>();
        List<Long> invisibleUserIds = new ArrayList<>();
        
        for (Long userId : filteredUserIds) {
            if (existingUserIds.contains(userId)) {
                // 已经通知过的用户，跳过
                continue;
            }

            if (!visibleUserIds.contains(DEFAULT_USER_ID)
                    && !visibleUserIds.isEmpty()
                    && !visibleUserIds.contains(userId)) {
                // 不满足可见范围
                invisibleUserIds.add(userId);
                continue;
            }
            // 新用户
            newUserIds.add(userId);
        }
        
        log.info("自动指派记录[{}]：新用户{}个，不可见用户{}个", 
                assignRecord.getId(), newUserIds.size(), invisibleUserIds.size());
        
        // 批量处理不同类型的用户
        int totalInserted = 0;
        
        // 收集所有范围ID
        List<Long> rangeIds = rangeList.stream().map(CommonRange::getId).collect(Collectors.toList());
        
        // 计算完成时间
        Date finishTime = calculateFinishTime(assignFinishedTimeType, customFinishedDay);
        
        // 处理不可见用户
        if (!invisibleUserIds.isEmpty()) {
            totalInserted += batchInsertAssignmentDetail(rangeIds, invisibleUserIds, 
                    assignRecord.getCreatorId(), assignRecord.getCreatorName(), 
                    AssignStatusEnums.NOTIFY_FAILED.getCode(), "用户不在可见范围内",
                    bizId, bizType, assignRecord.getId(), finishTime);
        }
        
        // 处理新用户
        if (!newUserIds.isEmpty()) {
            totalInserted += batchInsertAssignmentDetail(rangeIds, newUserIds, 
                    assignRecord.getCreatorId(), assignRecord.getCreatorName(), 
                    AssignStatusEnums.WAIT_NOTIFY.getCode(), null,
                    bizId, bizType, assignRecord.getId(), finishTime);
            
            // 发送钉钉消息
            sendDingTalkNotification(newUserIds, bizId, bizType, bizInfo);
        }
        
        // 自动通知成功后，更新状态为进行中
        assignRecord.setStatus(AssignStatusEnums.PROCESS.getCode());
        assignRecord.setGmtModified(new Date());
        assignRecordsMapper.updateById(assignRecord);

        log.info("自动指派记录[{}]处理完成，共处理{}个用户", assignRecord.getId(), totalInserted);
        return totalInserted;
    }

    @Override
    public String getAssignTypeCode() {
        return AssignTypeEnums.AUTO.getCode();
    }

    /**
     * 根据入职时间过滤用户
     *
     * @param userIds 用户ID列表
     * @param ifIsNotifyExistUser 是否通知已存在的用户
     * @param notifyUserAfterJoinDate 通知入职日期之后的用户
     * @param assignRecordModifiedTime 指派记录修改时间
     * @return 过滤后的用户ID列表
     */
    private List<Long> filterUsersByJoinDate(List<Long> userIds, Boolean ifIsNotifyExistUser, 
                                        Date notifyUserAfterJoinDate, Date assignRecordModifiedTime) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        
        // 查询用户的入职时间（gmt_create）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getUserId, userIds)
                .eq(User::getIsDel, 0)
                .select(User::getUserId, User::getGmtCreate);
        
        List<User> users = userMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        
        List<Long> filteredUserIds = new ArrayList<>();
        
        for (User user : users) {
            Date userJoinDate = user.getGmtCreate();
            
            // 如果用户入职时间为空，默认添加到结果中
            if (userJoinDate == null) {
                filteredUserIds.add(user.getUserId());
                continue;
            }
            
            // 根据ifIsNotifyExistUser和notifyUserAfterJoinDate过滤用户
            if (ifIsNotifyExistUser != null && ifIsNotifyExistUser) {
                // 通知已存在的用户（入职时间早于或等于指派记录修改时间的用户）
                if (userJoinDate.compareTo(assignRecordModifiedTime) <= 0) {
                    filteredUserIds.add(user.getUserId());
                }
            } else {
                // 仅通知新用户（入职时间晚于指派记录修改时间的用户）
                if (userJoinDate.compareTo(notifyUserAfterJoinDate) > 0) {
                    filteredUserIds.add(user.getUserId());
                }
            }
        }
        
        return filteredUserIds;
    }
}
