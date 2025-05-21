package com.learn.service.operation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.constants.BizType;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.OperationLog;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.operation.OperationLogService;
import com.learn.service.user.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 * 用于记录课程/培训/地图的操作日志
 */
@Service
@Slf4j
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 操作类型常量
     */
    private static final String OPERATION_TYPE_CREATE = "CREATE";
    private static final String OPERATION_TYPE_UPDATE = "UPDATE";
    private static final String OPERATION_TYPE_DELETE = "DELETE";
    private static final String OPERATION_TYPE_ASSIGN = "ASSIGN";
    private static final String OPERATION_TYPE_CANCEL_ASSIGN = "CANCEL";
    private static final String OPERATION_TYPE_PUBLISH = "PUBLISH";
    private static final String OPERATION_TYPE_UNPUBLISH = "UNPUBLISH";

    /**
     * 记录创建操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordCreateOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generateCreateSummary(bizType);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_CREATE, operationDetail, summary, userInfo);
    }

    /**
     * 记录更新操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordUpdateOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        // 获取当前数据库中的记录
        String currentRecordJson = getCurrentRecordJson(bizId, bizType);
        
        // 生成更新摘要
        String summary = generateSimpleUpdateSummary(bizType);
//        if (StringUtils.hasText(currentRecordJson)) {
//            summary = generateUpdateSummary(bizType, currentRecordJson, operationDetail);
//        } else {
//            summary = generateSimpleUpdateSummary(bizType);
//        }
        
        // 如果是培训或地图，还需要检查关联内容的变化
//        if (BizType.TRAIN.equals(bizType) || BizType.LEARNING_MAP.equals(bizType) || BizType.MAP_STAGE.equals(bizType)) {
//            String relationChanges = checkRelationChanges(bizId, bizType, operationDetail);
//            if (StringUtils.hasText(relationChanges)) {
//                summary = summary + relationChanges;
//            }
//        }
        
        saveOperationLog(bizId, bizType, OPERATION_TYPE_UPDATE, operationDetail, summary, userInfo);
    }
    
    /**
     * 获取当前数据库中记录的JSON表示
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 当前记录的JSON字符串
     */
    private String getCurrentRecordJson(Long bizId, String bizType) {
        try {
            Object record = null;
            
            // 根据业务类型查询不同的表
            switch (bizType) {
                case BizType.COURSE:
                    record = coursesMapper.selectById(bizId);
                    break;
                case BizType.TRAIN:
                    record = trainMapper.selectById(bizId);
                    break;
                case BizType.LEARNING_MAP:
                    record = learningMapMapper.selectById(bizId);
                    break;
                case BizType.MAP_STAGE:
                    record = learningMapStageMapper.selectById(bizId);
                    break;
                default:
                    log.warn("未知的业务类型: {}", bizType);
                    return null;
            }
            
            if (record == null) {
                log.warn("未找到业务记录: bizType={}, bizId={}", bizType, bizId);
                return null;
            }
            
            // 将实体对象转换为JSON字符串
            return objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.error("获取当前记录JSON失败", e);
            return null;
        }
    }

    /**
     * 记录删除操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordDeleteOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generateDeleteSummary(bizType);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_DELETE, operationDetail, summary, userInfo);
    }

    /**
     * 记录指派操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordAssignOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generateAssignSummary(bizType, operationDetail);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_ASSIGN, operationDetail, summary, userInfo);
    }

    /**
     * 记录取消指派操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordCancelAssignOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generateCancelAssignSummary(bizType, operationDetail);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_CANCEL_ASSIGN, operationDetail, summary, userInfo);
    }

    /**
     * 记录发布操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordPublishOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generatePublishSummary(bizType);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_PUBLISH, operationDetail, summary, userInfo);
    }

    /**
     * 记录取消发布操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情JSON
     * @param userInfo 操作用户信息
     */
    @Override
    public void recordUnpublishOperation(Long bizId, String bizType, String operationDetail, UserTokenInfo userInfo) {
        String summary = generateUnpublishSummary(bizType);
        saveOperationLog(bizId, bizType, OPERATION_TYPE_UNPUBLISH, operationDetail, summary, userInfo);
    }

    /**
     * 保存操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationType 操作类型
     * @param operationDetail 操作详情
     * @param operationSummary 操作摘要
     * @param userInfo 用户信息
     */
    private void saveOperationLog(Long bizId, String bizType, String operationType, 
                                 String operationDetail, String operationSummary, UserTokenInfo userInfo) {
        OperationLog log = new OperationLog();
        log.setBizId(bizId);
        log.setBizType(bizType);
        log.setOperationType(operationType);
        log.setOperationDetail(operationDetail);
        log.setOperationSummary(operationSummary);
        log.setOperatorId(userInfo.getUserId());
        UserInfoResponse userInfoResponse = userInfoService.getUserInfo(userInfo.getUserId());
        log.setOperatorName(userInfoResponse.getNickname());
        log.setCreatorName(userInfoResponse.getNickname());
        log.setUpdaterName(userInfoResponse.getNickname());

        log.setOperationTime(new Date());
        log.setGmtCreate(new Date());
        log.setCreatorId(userInfo.getUserId());
        log.setUpdaterId(userInfo.getUserId());
        log.setGmtModified(new Date());
        log.setIsDel(0);
        
        operationLogMapper.insert(log);
    }

    /**
     * 获取最近一次的操作日志
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @return 操作日志
     */
    private OperationLog getLastOperationLog(Long bizId, String bizType) {
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperationLog::getBizId, bizId)
                .eq(OperationLog::getBizType, bizType)
                .eq(OperationLog::getIsDel, 0)
                .orderByDesc(OperationLog::getOperationTime)
                .last("LIMIT 1");
        
        return operationLogMapper.selectOne(queryWrapper);
    }

    /**
     * 生成创建操作的摘要
     *
     * @param bizType 业务类型
     * @return 操作摘要
     */
    private String generateCreateSummary(String bizType) {
        switch (bizType) {
            case BizType.COURSE:
                return "创建了课程";
            case BizType.TRAIN:
                return "创建了培训";
            case BizType.LEARNING_MAP:
                return "创建了学习地图";
            case BizType.MAP_STAGE:
                return "创建了学习地图阶段";
            case BizType.CERTIFICATE:
                return "创建了证书";
            default:
                return "创建了内容";
        }
    }

    /**
     * 生成简单的更新操作摘要
     *
     * @param bizType 业务类型
     * @return 操作摘要
     */
    private String generateSimpleUpdateSummary(String bizType) {
        switch (bizType) {
            case BizType.COURSE:
                return "更新了课程";
            case BizType.TRAIN:
                return "更新了培训";
            case BizType.LEARNING_MAP:
                return "更新了学习地图";
            case BizType.MAP_STAGE:
                return "更新了学习地图阶段";
            case BizType.CERTIFICATE:
                return "更新了证书";
            default:
                return "更新了内容";
        }
    }

    /**
     * 生成更新操作的摘要，对比前后内容的差异
     *
     * @param bizType 业务类型
     * @param oldDetail 旧的操作详情
     * @param newDetail 新的操作详情
     * @return 操作摘要
     */
    private String generateUpdateSummary(String bizType, String oldDetail, String newDetail) {
        StringBuilder summary = new StringBuilder();
        
        try {
            JsonNode oldNode = objectMapper.readTree(oldDetail);
            JsonNode newNode = objectMapper.readTree(newDetail);
            
            // 根据业务类型，确定需要比较的字段
            Map<String, String> fieldNameMap = getFieldNameMap(bizType);
            
            // 比较字段变化
            for (Map.Entry<String, String> entry : fieldNameMap.entrySet()) {
                String field = entry.getKey();
                String displayName = entry.getValue();
                
                JsonNode oldValue = oldNode.get(field);
                JsonNode newValue = newNode.get(field);
                
                // 如果字段不存在于旧数据或新数据中，跳过
                if (oldValue == null || newValue == null) {
                    continue;
                }
                
                // 比较字段值是否有变化
                if (!oldValue.equals(newValue)) {
                    // 对于简单类型，直接比较值
                    if (oldValue.isValueNode() && newValue.isValueNode()) {
                        // 对于日期类型的特殊处理
                        if (field.contains("Time") || field.contains("Date")) {
                            // 如果是日期字段，格式化后再比较
                            String oldDate = formatDateValue(oldValue.asText());
                            String newDate = formatDateValue(newValue.asText());
                            
                            if (!oldDate.equals(newDate)) {
                                summary.append("将").append(displayName)
                                        .append("从「").append(oldDate).append("」")
                                        .append("修改为「").append(newDate).append("」；");
                            }
                        } else {
                            summary.append("将").append(displayName)
                                    .append("从「").append(oldValue.asText()).append("」")
                                    .append("修改为「").append(newValue.asText()).append("」；");
                        }
                    }
                    // 对于数组类型，比较数组内容
                    else if (oldValue.isArray() && newValue.isArray()) {
                        List<String> oldList = new ArrayList<>();
                        List<String> newList = new ArrayList<>();
                        
                        for (JsonNode item : oldValue) {
                            oldList.add(item.asText());
                        }
                        
                        for (JsonNode item : newValue) {
                            newList.add(item.asText());
                        }
                        
                        if (!oldList.equals(newList)) {
                            summary.append("修改了").append(displayName).append("；");
                        }
                    }
                    // 对于对象类型，简单标记为修改
                    else if (oldValue.isObject() && newValue.isObject()) {
                        summary.append("修改了").append(displayName).append("；");
                    }
                }
            }
            
            if (summary.length() == 0) {
                return generateSimpleUpdateSummary(bizType);
            }
            
            return summary.toString();
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败", e);
            return generateSimpleUpdateSummary(bizType);
        }
    }
    
    /**
     * 格式化日期值
     *
     * @param dateStr 日期字符串
     * @return 格式化后的日期字符串
     */
    private String formatDateValue(String dateStr) {
        if (StringUtils.isEmpty(dateStr) || "null".equals(dateStr)) {
            return "未设置";
        }
        
        try {
            // 尝试解析日期字符串
            Date date = new Date(Long.parseLong(dateStr));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        } catch (NumberFormatException e) {
            // 如果不是时间戳格式，直接返回原字符串
            return dateStr;
        }
    }

    /**
     * 生成删除操作的摘要
     *
     * @param bizType 业务类型
     * @return 操作摘要
     */
    private String generateDeleteSummary(String bizType) {
        switch (bizType) {
            case BizType.COURSE:
                return "删除了课程";
            case BizType.TRAIN:
                return "删除了培训";
            case BizType.LEARNING_MAP:
                return "删除了学习地图";
            case BizType.MAP_STAGE:
                return "删除了学习地图阶段";
            case BizType.CERTIFICATE:
                return "删除了证书";
            default:
                return "删除了内容";
        }
    }

    /**
     * 生成指派操作的摘要
     *
     * @param bizType 业务类型
     * @param operationDetail 操作详情
     * @return 操作摘要
     */
    private String generateAssignSummary(String bizType, String operationDetail) {
        String contentType = "";
        switch (bizType) {
            case BizType.COURSE:
                contentType = "课程";
                break;
            case BizType.TRAIN:
                contentType = "培训";
                break;
            case BizType.LEARNING_MAP:
                contentType = "学习地图";
                break;
            default:
                contentType = "内容";
        }
        
        try {
            JsonNode detailNode = objectMapper.readTree(operationDetail);
            JsonNode targetsNode = detailNode.get("targets");
            
            if (targetsNode != null && targetsNode.isArray()) {
                int count = targetsNode.size();
                return "指派" + contentType + "给" + count + "个用户/组织";
            }
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败", e);
        }
        
        return "指派了" + contentType;
    }

    /**
     * 生成取消指派操作的摘要
     *
     * @param bizType 业务类型
     * @param operationDetail 操作详情
     * @return 操作摘要
     */
    private String generateCancelAssignSummary(String bizType, String operationDetail) {
        String contentType = "";
        switch (bizType) {
            case BizType.COURSE:
                contentType = "课程";
                break;
            case BizType.TRAIN:
                contentType = "培训";
                break;
            case BizType.LEARNING_MAP:
                contentType = "学习地图";
                break;
            default:
                contentType = "内容";
        }
        
        try {
            JsonNode detailNode = objectMapper.readTree(operationDetail);
            JsonNode targetsNode = detailNode.get("targets");
            
            if (targetsNode != null && targetsNode.isArray()) {
                int count = targetsNode.size();
                return "取消指派" + contentType + "给" + count + "个用户/组织";
            }
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败", e);
        }
        
        return "取消指派了" + contentType;
    }

    /**
     * 生成发布操作的摘要
     *
     * @param bizType 业务类型
     * @return 操作摘要
     */
    private String generatePublishSummary(String bizType) {
        switch (bizType) {
            case BizType.COURSE:
                return "发布了课程";
            case BizType.TRAIN:
                return "发布了培训";
            case BizType.LEARNING_MAP:
                return "发布了学习地图";
            default:
                return "发布了内容";
        }
    }

    /**
     * 生成取消发布操作的摘要
     *
     * @param bizType 业务类型
     * @return 操作摘要
     */
    private String generateUnpublishSummary(String bizType) {
        switch (bizType) {
            case BizType.COURSE:
                return "取消发布了课程";
            case BizType.TRAIN:
                return "取消发布了培训";
            case BizType.LEARNING_MAP:
                return "取消发布了学习地图";
            default:
                return "取消发布了内容";
        }
    }

    /**
     * 获取字段名称映射
     *
     * @param bizType 业务类型
     * @return 字段名称映射
     */
    private Map<String, String> getFieldNameMap(String bizType) {
        Map<String, String> fieldNameMap = new HashMap<>();
        
        switch (bizType) {
            case BizType.COURSE:
                fieldNameMap.put("title", "课程名称");
                fieldNameMap.put("type", "课程类型");
                fieldNameMap.put("coverImage", "封面图片");
                fieldNameMap.put("instructorId", "讲师");
                fieldNameMap.put("description", "简介");
                fieldNameMap.put("credit", "学分");
                fieldNameMap.put("categoryIds", "分类");
                fieldNameMap.put("allowComments", "是否允许评论");
                fieldNameMap.put("article", "文章内容");
                fieldNameMap.put("appendixType", "附件类型");
                fieldNameMap.put("appendixPath", "附件路径");
                break;
            case BizType.TRAIN:
                fieldNameMap.put("name", "培训名称");
                fieldNameMap.put("cover", "封面图片");
                fieldNameMap.put("introduction", "培训简介");
                fieldNameMap.put("credit", "学分");
                fieldNameMap.put("categoryIds", "分类");
                fieldNameMap.put("allowComment", "是否允许评论");
                fieldNameMap.put("certificateId", "证书");
                fieldNameMap.put("status", "状态");
                break;
            case BizType.LEARNING_MAP:
                fieldNameMap.put("name", "地图名称");
                fieldNameMap.put("cover", "封面图片");
                fieldNameMap.put("introduction", "地图简介");
                fieldNameMap.put("creditRule", "学分规则");
                fieldNameMap.put("electiveCredit", "选修学分");
                fieldNameMap.put("requiredCredit", "必修学分");
                fieldNameMap.put("categoryIds", "分类");
                fieldNameMap.put("certificateRule", "证书规则");
                fieldNameMap.put("certificateId", "证书");
                fieldNameMap.put("dingtalkGroup", "钉钉群");
                fieldNameMap.put("startTime", "开始时间");
                fieldNameMap.put("endTime", "结束时间");
                fieldNameMap.put("unlockMode", "解锁方式");
                fieldNameMap.put("theme", "地图主题");
                break;
            case BizType.MAP_STAGE:
                fieldNameMap.put("name", "阶段名称");
                fieldNameMap.put("description", "阶段描述");
                fieldNameMap.put("credit", "学分");
                fieldNameMap.put("certificateId", "证书");
                fieldNameMap.put("sortOrder", "排序");
                break;
            case BizType.CERTIFICATE:
                fieldNameMap.put("name", "证书名称");
                fieldNameMap.put("cover", "封面图片");
                fieldNameMap.put("description", "证书描述");
                fieldNameMap.put("template", "证书模板");
                fieldNameMap.put("validDays", "有效天数");
                break;
            default:
                // 默认字段映射
                fieldNameMap.put("name", "名称");
                fieldNameMap.put("title", "标题");
                fieldNameMap.put("description", "描述");
                fieldNameMap.put("introduction", "简介");
        }
        
        return fieldNameMap;
    }

    /**
     * 检查关联内容的变化
     *
     * @param bizId 业务ID
     * @param bizType 业务类型
     * @param operationDetail 操作详情
     * @return 关联内容变化的摘要
     */
    private String checkRelationChanges(Long bizId, String bizType, String operationDetail) {
        try {
            // 从操作详情中获取关联内容
            JsonNode detailNode = objectMapper.readTree(operationDetail);
            JsonNode contentsNode = detailNode.get("contents");
            
            if (contentsNode == null || !contentsNode.isArray()) {
                return "";
            }
            
            // 获取当前数据库中的关联内容
            LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ContentRelation::getBizId, bizId)
                    .eq(ContentRelation::getBizType, bizType)
                    .eq(ContentRelation::getIsDel, 0);
            
            List<ContentRelation> existingRelations = contentRelationMapper.selectList(queryWrapper);
            
            // 将现有关联内容转换为Map，方便查找
            Map<String, List<ContentRelation>> existingRelationMap = existingRelations.stream()
                    .collect(Collectors.groupingBy(ContentRelation::getContentType));
            
            // 将新的关联内容转换为Map
            Map<String, List<JsonNode>> newRelationMap = new HashMap<>();
            for (JsonNode contentNode : contentsNode) {
                JsonNode contentTypeNode = contentNode.get("contentType");
                if (contentTypeNode == null) {
                    // 如果contentType字段不存在，跳过此节点或使用默认值
                    log.warn("关联内容缺少contentType字段: {}", contentNode);
                    continue;
                }
                String contentType = contentTypeNode.asText();
                if (!newRelationMap.containsKey(contentType)) {
                    newRelationMap.put(contentType, new ArrayList<>());
                }
                newRelationMap.get(contentType).add(contentNode);
            }
            
            // 比较关联内容的变化
            StringBuilder changes = new StringBuilder();
            
            // 检查新增的内容类型
            for (String contentType : newRelationMap.keySet()) {
                if (!existingRelationMap.containsKey(contentType)) {
                    changes.append("新增了").append(getContentTypeName(contentType)).append("；");
                } else {
                    // 比较同类型内容的数量变化
                    int oldCount = existingRelationMap.get(contentType).size();
                    int newCount = newRelationMap.get(contentType).size();
                    
                    if (oldCount != newCount) {
                        changes.append("修改了").append(getContentTypeName(contentType)).append("；");
                    }
                }
            }
            
            // 检查删除的内容类型
            for (String contentType : existingRelationMap.keySet()) {
                if (!newRelationMap.containsKey(contentType)) {
                    changes.append("删除了").append(getContentTypeName(contentType)).append("；");
                }
            }
            
            return changes.toString();
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败", e);
            return "";
        }
    }

    /**
     * 获取内容类型的显示名称
     *
     * @param contentType 内容类型
     * @return 显示名称
     */
    private String getContentTypeName(String contentType) {
        switch (contentType) {
            case "EXAM":
                return "考试";
            case "ASSIGNMENT":
                return "作业";
            case "SURVEY":
                return "调研";
            case "COURSE":
                return "课程";
            case "TRAIN":
                return "培训";
            default:
                return "内容";
        }
    }
}
