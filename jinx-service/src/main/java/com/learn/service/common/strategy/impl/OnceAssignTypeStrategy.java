package com.learn.service.common.strategy.impl;

import com.learn.common.dto.CardSendInfo;
import com.learn.common.enums.*;
import com.learn.common.util.ConverterUtils;
import com.learn.constants.BizConstants;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.infrastructure.repository.mapper.AssignRecordsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 单次指派类型策略实现
 */
@Component
@Slf4j
public class OnceAssignTypeStrategy extends AbstractAssignTypeStrategy {

    @Autowired
    private AssignRecordsMapper assignRecordsMapper;

    @Override
    public int processAssignRecord(AssignRecords assignRecord, List<CommonRange> rangeList,
                                  AssignFinishedTimeTypeEnums assignFinishedTimeType,
                                  Integer customFinishedDay,
                                  Boolean ifIsNotifyExistUser,
                                  java.util.Date notifyUserAfterJoinDate) {
        log.info("处理单次指派记录：{}", assignRecord.getId());

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
            List<Long> targetIds = ConverterUtils.parseTargetIds(range.getTargetIds());

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

        if (allUserIds.isEmpty()) {
            updateDataToSuccess(assignRecord);
            log.warn("没有找到需要处理的用户");
            return 0;
        }

        // 获取可见范围内的用户
        Set<Long> visibleUserIds = getVisibleUserIds(bizId, bizType);

        // 获取已存在的用户
        List<Long> existingUserIds = getExistingUserIds(bizId, bizType);

        // 分类处理用户
        List<Long> validUserIds = new ArrayList<>();
        List<Long> invisibleUserIds = new ArrayList<>();
        List<Long> duplicateUserIds = new ArrayList<>();

        for (Long userId : allUserIds) {
            if (existingUserIds.contains(userId)) {
                // 重复指派
                duplicateUserIds.add(userId);
            } else if (!visibleUserIds.contains(BizConstants.DEFAULT_USER_ID) && !visibleUserIds.isEmpty() && !visibleUserIds.contains(userId)) {
                // 不满足可见范围
                invisibleUserIds.add(userId);
            } else {
                // 有效用户
                validUserIds.add(userId);
            }
        }

        log.info("单次指派记录[{}]：有效用户{}个，不可见用户{}个，重复用户{}个",
                assignRecord.getId(), validUserIds.size(), invisibleUserIds.size(), duplicateUserIds.size());

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

        // 处理重复用户
        if (!duplicateUserIds.isEmpty()) {
            totalInserted += batchInsertAssignmentDetail(rangeIds, duplicateUserIds,
                    assignRecord.getCreatorId(), assignRecord.getCreatorName(),
                    AssignStatusEnums.NOTIFY_FAILED.getCode(), "用户已被指派",
                    bizId, bizType, assignRecord.getId(), finishTime);
        }

        // 处理有效用户
        if (!validUserIds.isEmpty()) {
            totalInserted += batchInsertAssignmentDetail(rangeIds, validUserIds,
                    assignRecord.getCreatorId(), assignRecord.getCreatorName(),
                    AssignStatusEnums.WAIT_NOTIFY.getCode(), null,
                    bizId, bizType, assignRecord.getId(), finishTime);

            // 发送钉钉消息
            sendDingTalkNotification(validUserIds, bizId, bizType, bizInfo);

        }

        // 单次通知成功后，更新状态为已完成
        if (totalInserted > 0) {
            updateDataToSuccess(assignRecord);
        }

        log.info("单次指派记录[{}]处理完成，共处理{}个用户", assignRecord.getId(), totalInserted);
        return totalInserted;
    }

    private void updateDataToSuccess(AssignRecords assignRecord) {
        assignRecord.setStatus(AssignStatusEnums.SUCCESS.getCode());
        assignRecord.setGmtModified(new Date());
        assignRecordsMapper.updateById(assignRecord);
        log.info("单次指派记录[{}]状态更新为已完成", assignRecord.getId());
    }

    @Override
    public String getAssignTypeCode() {
        return AssignTypeEnums.ONCE.getCode();
    }
}
