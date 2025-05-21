package com.learn.service.common.strategy.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.mos.boot.config.ability.annotation.ConfigValue;
import com.google.common.collect.Lists;
import com.learn.common.constants.NacosConstants;
import com.learn.common.dto.CardSendInfo;
import com.learn.common.enums.AssignFinishedTimeTypeEnums;
import com.learn.common.enums.AssignStatusEnums;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.AssignmentDetail;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.infrastructure.repository.mapper.AssignmentDetailMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserThirdPartyMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.strategy.AssignTypeStrategy;
import com.learn.service.dingtalk.common.DingTalkApiClient;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.org.OrgRequest;
import com.learn.service.dto.role.RoleRequest;
import com.learn.service.org.OrgService;
import com.learn.service.role.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import shade.com.alibaba.fastjson2.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.learn.common.constants.Constants.*;

/**
 * 指派类型策略抽象基类
 * 封装了AutoAssignTypeStrategy和OnceAssignTypeStrategy的共同代码
 */
@Slf4j
public abstract class AbstractAssignTypeStrategy implements AssignTypeStrategy {

    @Autowired
    protected CommonRangeInterface commonRangeInterface;

    @Autowired
    protected AssignmentDetailMapper assignmentDetailMapper;
    
    @Autowired
    protected OrgService orgService;
    
    @Autowired
    protected RoleService roleService;
    
    @Autowired
    protected DingTalkApiClient dingTalkApiClient;
    
    @Autowired
    protected UserThirdPartyMapper userThirdPartyMapper;
    
    @Autowired
    protected TrainMapper trainMapper;
    
    @Autowired
    protected LearningMapMapper learningMapMapper;
    @Autowired
    private NacosConstants nacosConstants;

    private List<String> whiteList = Lists.newArrayList("11092475", "11011605", "11201949", "11034253","11223774","11231575");

    @ConfigValue(value = "jinx.dingtalk.white.closed")
    private Boolean dingTalkWhiteClosed;

    /**
     * 获取可见范围内的用户ID列表
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 可见范围内的用户ID集合
     */
    @Override
    public Set<Long> getVisibleUserIds(Long bizId, String bizType) {
        // 查询可见范围
        CommonRangeQueryRequest request = new CommonRangeQueryRequest();
        request.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        request.setTypeId(bizId);
        request.setType(bizType);
        
        try {
            CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(request);
            if (response != null && response.getSuccess()) {
                Set<Long> userIds = new HashSet<>();
                
                // 添加直接可见的用户
                if (!CollectionUtils.isEmpty(response.getUserIds())) {
                    userIds.addAll(response.getUserIds());
                }
                
                // 添加部门下的用户
                if (!CollectionUtils.isEmpty(response.getDepartmentIds())) {
                    List<Long> departmentUserIds = getDepartmentUsers(response.getDepartmentIds());
                    userIds.addAll(departmentUserIds);
                }
                
                // 添加角色下的用户
                if (!CollectionUtils.isEmpty(response.getRoleIds())) {
                    List<Long> roleUserIds = getRoleUsers(response.getRoleIds());
                    userIds.addAll(roleUserIds);
                }
                
                return userIds;
            }
        } catch (Exception e) {
            log.warn("获取业务[{}:{}]的可见范围失败: {}", bizType, bizId, e.getMessage());
        }
        
        // 如果没有配置可见范围或查询失败，返回空集合
        return new HashSet<>();
    }

    /**
     * 获取已存在的用户ID列表
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 已存在的用户ID列表
     */
    @Override
    public List<Long> getExistingUserIds(Long bizId, String bizType) {
        List<Long> existingUserIds = new ArrayList<>();
        
        try {
            // 构造bizId格式：{type}_{id}
            String bizIdStr = bizType + "_" + bizId;
            
            // 查询已存在的用户
            LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AssignmentDetail::getBizId, bizIdStr)
                    .eq(AssignmentDetail::getIsDel, 0)
                    .eq(AssignmentDetail::getStatus, AssignStatusEnums.NOTIFY_SUCCESS.getCode())
                    .select(AssignmentDetail::getUserid);
            
            List<AssignmentDetail> details = assignmentDetailMapper.selectList(queryWrapper);
            
            existingUserIds = details.stream()
                    .map(AssignmentDetail::getUserid)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取已存在用户失败", e);
        }
        
        return existingUserIds;
    }
    
    /**
     * 根据完成时间类型计算任务完成时间
     *
     * @param assignFinishedTimeType 完成时间类型
     * @param customFinishedDay 自定义完成时间
     * @return 计算后的完成时间
     */
    protected Date calculateFinishTime(AssignFinishedTimeTypeEnums assignFinishedTimeType, Integer customFinishedDay) {
        if (assignFinishedTimeType == null) {
            return null;
        }
        
        Calendar calendar = Calendar.getInstance();
        
        switch (assignFinishedTimeType) {
            case NONE:
                return null;
            case ONE_WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                return calendar.getTime();
            case TWO_WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 2);
                return calendar.getTime();
            case FOUR_WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 4);
                return calendar.getTime();
            case CUSTOM:
                calendar.add(Calendar.DAY_OF_YEAR, customFinishedDay);
                return calendar.getTime();
            default:
                return null;
        }
    }

    /**
     * 批量插入指派明细
     *
     * @param rangeIds 指派ID列表
     * @param userIds 用户ID列表
     * @param creatorId 创建人ID
     * @param creatorName 创建人名称
     * @param status 状态
     * @param reason 失败原因
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param assignRecordId 指派记录ID
     * @param finishTime 任务完成时间
     * @return 插入的记录数
     */
    protected int batchInsertAssignmentDetail(List<Long> rangeIds, List<Long> userIds, Long creatorId, 
                                           String creatorName, String status, String reason, 
                                           Long bizId, String bizType, Long assignRecordId,
                                           Date finishTime) {
        if (CollectionUtils.isEmpty(userIds) || CollectionUtils.isEmpty(rangeIds)) {
            return 0;
        }
        
        // 选择第一个范围ID作为指派ID
        Long assignmentId = rangeIds.get(0);
        
        // 构建bizId字符串: {type}_{id}
        String bizIdStr = bizType + "_" + bizId;
        
        List<AssignmentDetail> detailList = new ArrayList<>();
        Date now = new Date();

        for (Long userId : userIds) {
            AssignmentDetail detail = new AssignmentDetail();
            detail.setAssignmentId(assignmentId);
            detail.setUserid(userId);
            detail.setStatus(status);
            detail.setBizId(bizIdStr);
            detail.setType(bizType);
            detail.setTypeId(bizId);
            detail.setAssignRecordId(assignRecordId);
            detail.setGmtCreate(now);
            detail.setGmtModified(now);
            detail.setCreatorId(creatorId);
            detail.setCreatorName(creatorName);
            detail.setUpdaterId(creatorId);
            detail.setUpdaterName(creatorName);
            detail.setIsDel(0);
            detail.setFinishTime(finishTime);
            
            // 设置失败原因
            if (StringUtils.isNotBlank(reason)) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put("reason", reason);
                detail.setAttributes(Json.toJson(attributes));
            }

            detailList.add(detail);
        }

        // 批量插入
        if (!detailList.isEmpty()) {
            for (AssignmentDetail detail : detailList) {
                assignmentDetailMapper.insert(detail);
            }
        }

        return detailList.size();
    }
    /**
     * 发送钉钉通知
     * 
     * @param userIds 用户ID列表
     * @param bizId 业务ID
     * @param bizType 业务类型
     */
    protected void sendDingTalkNotification(List<Long> userIds, Long bizId,
                                            String bizType, CardSendInfo bizInfo) {
        List<String> dingTalkUserIds = getThirdPartyUserIds(userIds);
        // 根据 userIds 获取三方 userId, 然后发消息
        if (!Objects.equals(dingTalkWhiteClosed, Boolean.TRUE)) {
            dingTalkUserIds = dingTalkUserIds.stream()
                    .filter(whiteList::contains)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dingTalkUserIds)) {
                log.info("白名单拦截，过滤了用户" );
                // 发送消息后将通知状态更新为成功
                updateNotifyStatusToSuccess(userIds, bizId, bizType);
                return;
            }
        }

        Map<String, String> cardData = JSON.parseObject(
                JSON.toJSONString(bizInfo),
                new TypeReference<>() {
                }
        );


        boolean success = dingTalkApiClient.deliverSingleChatCard(
                cardData,
                dingTalkUserIds,
                "ding10dfcbc0c7dabea235c2f4657eb6378f"
        );
        if (!success) {
            throw new CommonException("发送钉钉通知失败");
        }
        log.info("发送钉钉通知给{}个用户，业务类型：{}，业务ID：{}", userIds.size(), bizType, bizId);
        
        // 发送消息后将通知状态更新为成功
        updateNotifyStatusToSuccess(userIds, bizId, bizType);
    }
    
    /**
     * 更新用户通知状态为成功
     *
     * @param userIds 用户ID列表
     * @param bizId   业务ID
     * @param bizType 业务类型
     */
    protected void updateNotifyStatusToSuccess(List<Long> userIds, Long bizId, String bizType) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 构建bizId字符串: {type}_{id}
        String bizIdStr = bizType + "_" + bizId;

        // 更新通知状态为成功
        LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignmentDetail::getBizId, bizIdStr)
                .eq(AssignmentDetail::getIsDel, 0)
                .eq(AssignmentDetail::getStatus, AssignStatusEnums.WAIT_NOTIFY.getCode())
                .in(AssignmentDetail::getUserid, userIds);

        AssignmentDetail updateDetail = new AssignmentDetail();
        updateDetail.setStatus(AssignStatusEnums.NOTIFY_SUCCESS.getCode());
        updateDetail.setGmtModified(new Date());

        int updatedCount = assignmentDetailMapper.update(updateDetail, queryWrapper);
        log.info("成功更新{}个用户的通知状态为成功，业务类型：{}，业务ID：{}", updatedCount, bizType, bizId);
    }
    
    /**
     * 获取用户对应的第三方用户ID列表
     * 
     * @param userIds 用户ID列表
     * @return 第三方用户ID列表
     */
    protected List<String> getThirdPartyUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        
        List<String> thirdPartyUserIds = new ArrayList<>();
        
        try {
            // 查询用户对应的钉钉用户ID
            LambdaQueryWrapper<UserThirdParty> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(UserThirdParty::getUserId, userIds)
                    .eq(UserThirdParty::getThirdPartyType, "dingtalk")
                    .eq(UserThirdParty::getIsDel, 0)
                    .select(UserThirdParty::getThirdPartyUserId);
            
            List<UserThirdParty> userThirdParties = userThirdPartyMapper.selectList(queryWrapper);
            
            if (!CollectionUtils.isEmpty(userThirdParties)) {
                thirdPartyUserIds = userThirdParties.stream()
                        .map(UserThirdParty::getThirdPartyUserId)
                        .collect(Collectors.toList());
            }
            
            log.info("获取到{}个用户的钉钉ID", thirdPartyUserIds.size());
        } catch (Exception e) {
            log.error("获取用户钉钉ID异常", e);
        }
        
        return thirdPartyUserIds;
    }
    
    /**
     * 获取部门下的用户ID列表
     * 
     * @param departmentIds 部门ID列表
     * @return 用户ID列表
     */
    protected List<Long> getDepartmentUsers(List<Long> departmentIds) {
        if (CollectionUtils.isEmpty(departmentIds)) {
            return Collections.emptyList();
        }
        
        List<Long> userIds = new ArrayList<>();
        OrgRequest request = new OrgRequest();
        request.setOrgIds(departmentIds);
        BaseResponse<Long> response = orgService.queryOrgUserIds(request);
        if (response != null && !CollectionUtils.isEmpty(response.getData())) {
            userIds.addAll(response.getData());
        }

        return userIds;
    }
    
    /**
     * 获取角色下的用户ID列表
     * 
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    protected List<Long> getRoleUsers(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        
        List<Long> userIds = new ArrayList<>();
        RoleRequest request = new RoleRequest();
        request.setIds(roleIds);
        BaseResponse<Long> response = roleService.queryRoleUserIds(request);
        if (response != null && !CollectionUtils.isEmpty(response.getData())) {
            userIds.addAll(response.getData());
        }

        return userIds;
    }
    
    /**
     * 解析目标ID字符串
     *
     * @param targetIdsStr 目标ID字符串，格式为 [1,2,3,4]
     * @return 目标ID列表
     */
    protected List<Long> parseTargetIds(String targetIdsStr) {
        List<Long> targetIds = new ArrayList<>();

        if (targetIdsStr != null && targetIdsStr.length() > 2) {
            // 去掉方括号，并按逗号分割
            String[] idsStr = targetIdsStr.substring(1, targetIdsStr.length() - 1).split(",");
            for (String idStr : idsStr) {
                try {
                    targetIds.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略无法解析的ID
                    log.warn("无法解析的ID: {}", idStr);
                }
            }
        }

        return targetIds;
    }
    
    /**
     * 根据业务类型和业务ID查询标题和图片
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 包含标题和图片URL的对象
     */

    protected CardSendInfo getTypeElementInfo(String bizType, Long bizId) {

        // 根据业务类型和ID查询标题和图片
        if (BizType.TRAIN.equals(bizType)) {
            // 查询培训信息
            Train train = trainMapper.selectById(bizId);
            if (Objects.isNull(train)) {
                throw new RuntimeException("未找到培训信息：id=" + bizId);
            }
            String pcUrl = nacosConstants.getDomain() + String.format(TRAIN_DETAIL_PC, train.getId());
            String mobileUrl = nacosConstants.getDomain() + String.format(TRAIN_DETAIL_MOBILE, train.getId());
            pcUrl = String.format(DING_TALK_PC_URL, URLEncoder.encode(pcUrl, StandardCharsets.UTF_8) + "$CORPID$" + "%26ddtab%3Dtrue", "题雀");
            mobileUrl = String.format(DING_TALK_MOBILE_URL, URLEncoder.encode(mobileUrl, StandardCharsets.UTF_8) + "$CORPID$");
            return CardSendInfo.builder()
                    .title(train.getName())
                    .picUrl(train.getCover())
                    .desc(train.getIntroduction())
                    .pcUrl(pcUrl)
                    .mobileUrl(mobileUrl)
                    .build();
        } else if (BizType.LEARNING_MAP.equals(bizType)) {
            // 查询学习地图信息
            LearningMap learningMap = learningMapMapper.selectById(bizId);
            if (Objects.isNull(learningMap)) {
                throw new RuntimeException("未找到学习地图信息：id=" + bizId);
            }
            String pcUrl = nacosConstants.getDomain() + String.format(LEARNING_MAP_DETAIL_PC, learningMap.getId());
            String mobileUrl = nacosConstants.getDomain() + String.format(LEARNING_MAP_DETAIL_MOBILE, learningMap.getId());
            pcUrl = String.format(DING_TALK_PC_URL, URLEncoder.encode(pcUrl, StandardCharsets.UTF_8) + "$CORPID$" + "%26ddtab%3Dtrue", "题雀");
            mobileUrl = String.format(DING_TALK_MOBILE_URL, URLEncoder.encode(mobileUrl, StandardCharsets.UTF_8) + "$CORPID$");
            return CardSendInfo.builder()
                    .title(learningMap.getName())
                    .picUrl(learningMap.getCover())
                    .desc(learningMap.getIntroduction())
                    .pcUrl(pcUrl)
                    .mobileUrl(mobileUrl)
                    .build();
        }
        throw new RuntimeException("不支持的业务类型：" + bizType);
    }
//
//    public static void main(String[] args) {
//        String url = String.format(DING_TALK_PC_URL, URLEncoder.encode("https://tique-daily.linkheer.com/map/detail/7?corpId=", StandardCharsets.UTF_8) + "$CORPID$" + "%26ddtab%3Dtrue", "题雀");
//        System.out.println(url);
//
//    }
}
