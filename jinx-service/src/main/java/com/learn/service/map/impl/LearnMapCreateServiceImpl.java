package com.learn.service.map.impl;

import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.map.LearningMapCreateRequest;
import com.learn.dto.map.LearningMapCreateResponse;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.dingtalk.DingTalkDepartmentSyncService;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.map.LearnMapCreateService;
import com.learn.service.operation.OperationLogService;
import com.learn.common.dto.UserTokenInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.learn.infrastructure.repository.entity.LearningMap.ATTRIBUTES_KEY_ENABLE_AUTO_ASSIGN;

/**
 * 学习地图创建服务实现类
 */
@Service
@Slf4j
public class LearnMapCreateServiceImpl implements LearnMapCreateService {

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired(required = false)
    private DingTalkDepartmentSyncService dingTalkSyncService;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RangeSetService rangeSetService;

    private static final String BIZ_TYPE_MAP_STAGE = "MAP_STAGE";

    /**
     * 创建学习地图
     *
     * @param request 创建学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 创建学习地图响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningMapCreateResponse createLearningMap(LearningMapCreateRequest request, Long userId, String userName) {
        // 参数校验
        validateCreateRequest(request);

        // 创建学习地图基本信息
        LearningMap learningMap = createLearningMapBasic(request, userId, userName);

        // 创建学习地图阶段和任务
        createLearningMapStagesAndTasks(request.getStages(), learningMap, userId, userName);
        
        // 关联证书内容
        createCertificateRelation(request, learningMap.getId(), userId, userName);

        // 设置学习地图可见范围和协同管理
        setLearningMapVisibilityAndCollaborators(request, learningMap.getId(), userId, userName);

        // 如果需要创建钉钉群
        if (request.getDingtalkGroup() != null && request.getDingtalkGroup() == 1) {
            createDingtalkGroup(learningMap);
        }

        // 记录操作日志
        try {
            UserTokenInfo userInfo = new UserTokenInfo();
            userInfo.setUserId(userId);
            
            // 构建操作详情JSON
            Map<String, Object> operationDetail = new HashMap<>();
            operationDetail.put("name", learningMap.getName());
            operationDetail.put("cover", learningMap.getCover());
            operationDetail.put("introduction", learningMap.getIntroduction());
            operationDetail.put("creditRule", learningMap.getCreditRule());
            operationDetail.put("requiredCredit", learningMap.getRequiredCredit());
            operationDetail.put("electiveCredit", learningMap.getElectiveCredit());
            operationDetail.put("categoryIds", learningMap.getCategoryIds());
            operationDetail.put("certificateRule", learningMap.getCertificateRule());
            operationDetail.put("certificateId", learningMap.getCertificateId());
            operationDetail.put("theme", learningMap.getTheme());
            
            String operationDetailJson = objectMapper.writeValueAsString(operationDetail);
            operationLogService.recordCreateOperation(learningMap.getId(), BizType.LEARNING_MAP, operationDetailJson, userInfo);
        } catch (Exception e) {
            log.error("记录创建学习地图操作日志失败", e);
            // 不影响主流程，继续执行
        }
        
        // 构建响应
        LearningMapCreateResponse response = new LearningMapCreateResponse();
        response.setId(learningMap.getId());
        response.setName(learningMap.getName());

        return response;
    }

    /**
     * 校验创建学习地图请求
     *
     * @param request 创建学习地图请求
     */
    private void validateCreateRequest(LearningMapCreateRequest request) {
        // 校验必填字段
        if (StringUtils.isBlank(request.getName())) {
            throw new CommonException("地图名称不能为空");
        }

        if (request.getStages() == null || request.getStages().isEmpty()) {
            throw new CommonException("学习地图阶段不能为空");
        }

        // 校验证书规则
        if (request.getCertificateRule() != null && request.getCertificateRule() == 1) {
            if (request.getCertificateId() == null) {
                throw new CommonException("整体发放证书时，证书ID不能为空");
            }
        }

        // 校验阶段
        for (LearningMapCreateRequest.StageRequest stage : request.getStages()) {
            if (StringUtils.isBlank(stage.getName())) {
                throw new CommonException("阶段名称不能为空");
            }

            if (stage.getStageOrder() == null) {
                throw new CommonException("阶段顺序不能为空");
            }

            // 校验开放类型
            if (stage.getOpenType() != null) {
                if (stage.getOpenType() == 1) {
                    // 固定时间
                    if (StringUtils.isBlank(stage.getStartTime()) || StringUtils.isBlank(stage.getEndTime())) {
                        throw new CommonException("固定开放时间模式下，开始时间和结束时间不能为空");
                    }
                } else if (stage.getOpenType() == 2) {
                    // 学习期限
                    if (stage.getDurationDays() == null) {
                        throw new CommonException("学习期限模式下，学习期限天数不能为空");
                    }
                }
            }

            // 校验学分规则
            if (request.getCreditRule() != null && request.getCreditRule() == 1) {
                if (stage.getCredit() == null) {
                    throw new CommonException("按阶段发放学分时，阶段学分不能为空");
                }
            }

            // 校验证书规则
            if (request.getCertificateRule() != null && request.getCertificateRule() == 2) {
                if (stage.getCertificateId() == null) {
                    throw new CommonException("按阶段发放证书时，阶段证书ID不能为空");
                }
            }

            // 校验任务
            if (stage.getTasks() == null || stage.getTasks().isEmpty()) {
                throw new CommonException("阶段任务不能为空");
            }

            for (LearningMapCreateRequest.TaskRequest task : stage.getTasks()) {
                if (StringUtils.isBlank(task.getType())) {
                    throw new CommonException("任务类型不能为空");
                }

                if (task.getContentId() == null) {
                    throw new CommonException("任务内容ID不能为空");
                }
            }
        }
    }

    /**
     * 创建学习地图基本信息
     *
     * @param request 创建学习地图请求
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 学习地图实体
     */
    private LearningMap createLearningMapBasic(LearningMapCreateRequest request, Long userId, String userName) {
        LearningMap learningMap = new LearningMap();
        learningMap.setName(request.getName());
        learningMap.setCover(request.getCover());
        learningMap.setIntroduction(request.getIntroduction());
        
        // 设置学分规则
        learningMap.setCreditRule(request.getCreditRule() != null ? request.getCreditRule() : 0);
        learningMap.setRequiredCredit(request.getRequiredCredit() != null ? request.getRequiredCredit() : 0);
        learningMap.setElectiveCredit(request.getElectiveCredit() != null ? request.getElectiveCredit() : 0);
        
        // 设置分类IDs
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            learningMap.setCategoryIds(request.getCategoryIds());
        }
        
        // 设置证书规则
        learningMap.setCertificateRule(request.getCertificateRule() != null ? request.getCertificateRule() : 0);
        learningMap.setCertificateId(request.getCertificateId());
        
        // 设置钉钉群
        learningMap.setDingtalkGroup(request.getDingtalkGroup() != null ? request.getDingtalkGroup() : 0);
        
        // 设置开放时间
        if (StringUtils.isNotBlank(request.getStartTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                learningMap.setStartTime(sdf.parse(request.getStartTime()));
            } catch (ParseException e) {
                log.error("解析开始时间失败", e);
                throw new CommonException("开始时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
            }
        }
        
        if (StringUtils.isNotBlank(request.getEndTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                learningMap.setEndTime(sdf.parse(request.getEndTime()));
            } catch (ParseException e) {
                log.error("解析结束时间失败", e);
                throw new CommonException("结束时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
            }
        }
        
        // 设置解锁方式
        learningMap.setUnlockMode(request.getUnlockMode() != null ? request.getUnlockMode() : 0);
        
        // 设置主题
        learningMap.setTheme(StringUtils.isNotBlank(request.getTheme()) ? request.getTheme() : "business");
        
        // 设置创建人信息
        learningMap.setCreatorId(userId);
        learningMap.setCreatorName(userName);
        learningMap.setUpdaterId(userId);
        learningMap.setUpdaterName(userName);
        
        // 设置时间信息
        Date now = new Date();
        learningMap.setGmtCreate(now);
        learningMap.setGmtModified(now);
        learningMap.setIsDel(0);
        learningMap.appendAttributes(ATTRIBUTES_KEY_ENABLE_AUTO_ASSIGN, Boolean.TRUE);
        
        // 插入数据库
        learningMapMapper.insert(learningMap);
        
        return learningMap;
    }

    /**
     * 创建学习地图阶段和任务
     *
     * @param stages 阶段请求列表
     * @param learningMap 学习地图实体
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void createLearningMapStagesAndTasks(List<LearningMapCreateRequest.StageRequest> stages, 
                                                LearningMap learningMap, 
                                                Long userId, 
                                                String userName) {
        if (stages == null || stages.isEmpty()) {
            return;
        }
        
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (LearningMapCreateRequest.StageRequest stageRequest : stages) {
            // 创建阶段
            LearningMapStage stage = new LearningMapStage();
            stage.setMapId(learningMap.getId());
            stage.setName(stageRequest.getName());
            stage.setStageOrder(stageRequest.getStageOrder());
            stage.setOpenType(stageRequest.getOpenType() != null ? stageRequest.getOpenType() : 0);
            
            // 设置开放时间
            if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 1) {
                try {
                    if (StringUtils.isNotBlank(stageRequest.getStartTime())) {
                        stage.setStartTime(sdf.parse(stageRequest.getStartTime()));
                    }
                    if (StringUtils.isNotBlank(stageRequest.getEndTime())) {
                        stage.setEndTime(sdf.parse(stageRequest.getEndTime()));
                    }
                } catch (ParseException e) {
                    log.error("解析阶段开放时间失败", e);
                    throw new CommonException("阶段开放时间格式错误，正确格式为：yyyy-MM-dd HH:mm:ss");
                }
            }
            
            // 设置学习期限
            if (stageRequest.getOpenType() != null && stageRequest.getOpenType() == 2) {
                stage.setDurationDays(stageRequest.getDurationDays());
            }
            
            // 设置阶段学分
            if (learningMap.getCreditRule() == 1) {
                stage.setCredit(stageRequest.getCredit());
            }
            
            // 设置阶段证书
            if (learningMap.getCertificateRule() == 2) {
                stage.setCertificateId(stageRequest.getCertificateId());
            }
            
            // 设置创建人信息
            stage.setCreatorId(userId);
            stage.setCreatorName(userName);
            stage.setUpdaterId(userId);
            stage.setUpdaterName(userName);
            
            // 设置时间信息
            stage.setGmtCreate(now);
            stage.setGmtModified(now);
            stage.setIsDel(0);
            
            // 插入数据库
            learningMapStageMapper.insert(stage);

            // 插入关联关系
            ContentRelation relation = new ContentRelation();
            relation.setBizId(learningMap.getId());
            relation.setBizType(BizType.LEARNING_MAP);
            // 处理Content类型
            relation.setContentType(BizType.MAP_STAGE);
            relation.setContentId(stage.getId());
            relation.setSortOrder(stageRequest.getStageOrder());
            relation.setIsRequired(1);
            // 设置创建人信息
            relation.setCreatorId(userId);
            relation.setCreatorName(userName);
            relation.setUpdaterId(userId);
            relation.setUpdaterName(userName);
            // 设置时间信息
            relation.setGmtCreate(now);
            relation.setGmtModified(now);
            relation.setIsDel(0);

            // 创建关联关系
            contentRelationMapper.insert(relation);
            
            // 创建任务
            createLearningMapTasks(stageRequest.getTasks(), stage.getId(), userId, userName);
        }
    }

    /**
     * 创建学习地图任务
     * 注意：ContentRelation中bizType不存在Content类型，需要将Content区分为Course或MAP_STAGE
     *
     * @param tasks 任务请求列表
     * @param stageId 阶段ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void createLearningMapTasks(List<LearningMapCreateRequest.TaskRequest> tasks, 
                                       Long stageId, 
                                       Long userId, 
                                       String userName) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        Date now = new Date();
        
        for (LearningMapCreateRequest.TaskRequest taskRequest : tasks) {
            ContentRelation relation = new ContentRelation();
            relation.setBizId(stageId);
            relation.setBizType(BIZ_TYPE_MAP_STAGE);

            // 处理Content类型
            relation.setContentType(taskRequest.getType());
            relation.setContentBizType(taskRequest.getSubType());
            relation.setContentId(taskRequest.getContentId());
            relation.setSortOrder(taskRequest.getSortOrder() != null ? taskRequest.getSortOrder() : 0);
            relation.setIsRequired(taskRequest.getIsRequired() != null && taskRequest.getIsRequired() ? 1 : 0);
            
            // 设置创建人信息
            relation.setCreatorId(userId);
            relation.setCreatorName(userName);
            relation.setUpdaterId(userId);
            relation.setUpdaterName(userName);
            
            // 设置时间信息
            relation.setGmtCreate(now);
            relation.setGmtModified(now);
            relation.setIsDel(0);
            
            // 插入数据库
            contentRelationMapper.insert(relation);
        }
    }

    /**
     * 设置学习地图可见范围和协同管理
     *
     * @param request 创建学习地图请求
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void setLearningMapVisibilityAndCollaborators(LearningMapCreateRequest request, Long mapId, Long userId, String userName) {
        // 设置可见范围
        rangeSetService.setVisibilityRange(BizType.LEARNING_MAP, mapId, request);

        // 设置协同管理
        rangeSetService.setCollaborators(BizType.LEARNING_MAP, mapId, request);
    }

    /**
     * 创建钉钉群
     * 注意：由于DingTalkSyncService接口中没有createGroup方法，这里只记录日志，不实际创建钉钉群
     *
     * @param learningMap 学习地图实体
     */
    private void createDingtalkGroup(LearningMap learningMap) {
        try {
            log.info("尝试创建钉钉群: {}", learningMap.getName());
            // 由于DingTalkSyncService接口中没有createGroup方法，这里只记录日志，不实际创建钉钉群
            // 如果后续DingTalkSyncService接口添加了createGroup方法，可以取消注释下面的代码
            // String groupId = dingTalkSyncService.createGroup(learningMap.getName(), "学习地图：" + learningMap.getName());
            // if (StringUtils.isNotBlank(groupId)) {
            //     learningMap.setDingtalkGroupId(groupId);
            //     learningMapMapper.updateById(learningMap);
            // }
        } catch (Exception e) {
            log.error("创建钉钉群失败", e);
            // 不抛出异常，继续执行后续逻辑
        }
    }

    /**
     * 创建证书关联
     *
     * @param request 创建学习地图请求
     * @param mapId 学习地图ID
     * @param userId 用户ID
     * @param userName 用户名称
     */
    private void createCertificateRelation(LearningMapCreateRequest request, Long mapId, Long userId, String userName) {
        Date now = new Date();
        List<ContentRelation> certificateRelations = new ArrayList<>();
        
        // 处理整体证书关联
        if (request.getCertificateId() != null) {
            // 创建证书关联记录
            ContentRelation certificateRelation = new ContentRelation();
            certificateRelation.setBizId(mapId);
            certificateRelation.setBizType(BizType.LEARNING_MAP);
            certificateRelation.setContentType(BizType.CERTIFICATE);
            certificateRelation.setContentId(request.getCertificateId());
            certificateRelation.setSortOrder(0);
            certificateRelation.setIsRequired(1);
            certificateRelation.setCreatorId(userId);
            certificateRelation.setCreatorName(userName);
            certificateRelation.setUpdaterId(userId);
            certificateRelation.setUpdaterName(userName);
            certificateRelation.setGmtCreate(now);
            certificateRelation.setGmtModified(now);
            certificateRelation.setIsDel(0);
            
            certificateRelations.add(certificateRelation);
        }
        
        // 处理阶段证书关联
        if (request.getCertificateRule() != null && request.getCertificateRule() == 2 && request.getStages() != null) {
            for (LearningMapCreateRequest.StageRequest stage : request.getStages()) {
                if (stage.getCertificateId() != null) {
                    ContentRelation stageCertificateRelation = new ContentRelation();
                    stageCertificateRelation.setBizId(mapId);
                    stageCertificateRelation.setBizType(BizType.MAP_STAGE);
                    stageCertificateRelation.setContentType(BizType.CERTIFICATE);
                    stageCertificateRelation.setContentId(stage.getCertificateId());
                    stageCertificateRelation.setSortOrder(stage.getStageOrder() != null ? stage.getStageOrder() : 0);
                    stageCertificateRelation.setIsRequired(1);
                    stageCertificateRelation.setCreatorId(userId);
                    stageCertificateRelation.setCreatorName(userName);
                    stageCertificateRelation.setUpdaterId(userId);
                    stageCertificateRelation.setUpdaterName(userName);
                    stageCertificateRelation.setGmtCreate(now);
                    stageCertificateRelation.setGmtModified(now);
                    stageCertificateRelation.setIsDel(0);
                    certificateRelations.add(stageCertificateRelation);
                }
            }
        }
        
        // 批量插入证书关联
        if (!certificateRelations.isEmpty()) {
            for (ContentRelation relation : certificateRelations) {
                contentRelationMapper.insert(relation);
            }
        }
    }
}
