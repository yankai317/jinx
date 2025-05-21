package com.learn.service.common.range;

import com.google.common.collect.Lists;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.enums.RangeTargetTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.dto.common.RangeBaseRequest;
import com.learn.dto.course.CourseDetailDTO;
import com.learn.dto.course.CourseUpdateRequest;
import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.TargetDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.learn.constants.BizConstants.DEFAULT_USER_ID;

/**
 * @author yujintao
 * @date 2025/4/29
 */
@Service
public class RangeSetService {

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 设置课程可见范围
     *
     * @param bizId   课程ID
     * @param request 创建课程请求
     */
    public void setVisibilityRange(String bizType, Long bizId, RangeBaseRequest request) {
        VisibilityDTO visibility = request.getVisibility();

        // 构建通用范围创建请求
        CommonRangeCreateRequest rangeRequest = new CommonRangeCreateRequest();
        rangeRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode()); // 功能模块类型：可见范围
        rangeRequest.setType(bizType); // 业务模块类型：课程
        rangeRequest.setTypeId(bizId); // 业务模块ID：课程ID
        rangeRequest.setCreatorId(request.getOperatorId());
        rangeRequest.setCreatorName(request.getOperatorName());

        // 设置目标范围
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();

        // 如果是全部可见
        if (Objects.isNull(visibility) || "ALL".equals(visibility.getType())) {
            targetTypeAndIds.put(RangeTargetTypeEnums.USER.getCode(), Lists.newArrayList(DEFAULT_USER_ID));
        } else {
            for (TargetDTO target : visibility.getTargets()) {
                targetTypeAndIds.put(target.getType(), target.getIds());
            }
        }
        rangeRequest.setTargetTypeAndIds(targetTypeAndIds);

        // 调用通用范围接口创建范围
        CommonRangeCreateResponse response = commonRangeInterface.batchCreateRange(rangeRequest);

        // 检查创建结果
        if (response == null || !response.getSuccess()) {
            String errorMsg = response != null ? response.getErrorMessage() : "未知错误";
            throw new CommonException("设置课程可见范围失败：" + errorMsg);
        }
    }


    /**
     * 更新可见范围
     *
     * @param bizId
     * @param request 更新课程请求
     */
    @Transactional
    public void updateVisibilityRange(String bizType, Long bizId, RangeBaseRequest request) {
        // 删除现有范围配置
        CommonRangeDeleteRequest deleteRequest = new CommonRangeDeleteRequest();
        deleteRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        deleteRequest.setType(bizType);
        deleteRequest.setTypeId(bizId);
        commonRangeInterface.deleteRangeByBusinessId(deleteRequest);

        // 设置新范围
        setVisibilityRange(bizType, bizId, request);
    }


    /**
     * 创建课程协同管理
     *
     * @param bizId   ID
     * @param request 更新课程请求
     */
    public void setCollaborators(String bizType, Long bizId, RangeBaseRequest request) {
        CollaboratorsDTO collaborators = request.getCollaborators() == null ? new CollaboratorsDTO() : request.getCollaborators();

        // 更新编辑者
        CommonRangeCreateRequest editorRangeRequest = new CommonRangeCreateRequest();
        editorRangeRequest.setModelType(RangeModelTypeEnums.EDITORS.getCode()); // 功能模块类型：编辑者
        editorRangeRequest.setType(bizType); // 业务模块类型：课程
        editorRangeRequest.setTypeId(bizId); // 业务模块ID：课程ID
        editorRangeRequest.setCreatorId(request.getOperatorId());
        editorRangeRequest.setCreatorName(request.getOperatorName());

        // 设置目标范围
        Map<String, List<Long>> editorTargetTypeAndIds = new HashMap<>();
        editorTargetTypeAndIds.put("user", collaborators.getEditors() != null ? collaborators.getEditors() : new ArrayList<>());
        editorRangeRequest.setTargetTypeAndIds(editorTargetTypeAndIds);

        // 调用通用范围接口更新范围
        CommonRangeCreateResponse editorResponse = commonRangeInterface.batchCreateRange(editorRangeRequest);

        // 检查更新结果
        if (editorResponse == null || !editorResponse.getSuccess()) {
            String errorMsg = editorResponse != null ? editorResponse.getErrorMessage() : "未知错误";
            throw new CommonException("创建课程编辑者失败：" + errorMsg);
        }

        // 更新普通协作者
        CommonRangeCreateRequest collaboratorRangeRequest = new CommonRangeCreateRequest();
        collaboratorRangeRequest.setModelType(RangeModelTypeEnums.COLLABORATORS.getCode()); // 功能模块类型：协作者
        collaboratorRangeRequest.setType(bizType); // 业务模块类型：课程
        collaboratorRangeRequest.setTypeId(bizId); // 业务模块ID：课程ID
        collaboratorRangeRequest.setCreatorId(request.getOperatorId());
        collaboratorRangeRequest.setCreatorName(request.getOperatorName());

        // 设置目标范围
        Map<String, List<Long>> collaboratorTargetTypeAndIds = new HashMap<>();
        collaboratorTargetTypeAndIds.put("user", collaborators.getUsers() != null ? collaborators.getUsers() : new ArrayList<>());
        collaboratorRangeRequest.setTargetTypeAndIds(collaboratorTargetTypeAndIds);

        // 调用通用范围接口更新范围
        CommonRangeCreateResponse collaboratorResponse = commonRangeInterface.batchCreateRange(collaboratorRangeRequest);

        // 检查更新结果
        if (collaboratorResponse == null || !collaboratorResponse.getSuccess()) {
            String errorMsg = collaboratorResponse != null ? collaboratorResponse.getErrorMessage() : "未知错误";
            throw new CommonException("创建课程协作者失败：" + errorMsg);
        }
    }

    /**
     * 更新课程协同
     *
     * @param bizId
     * @param request 更新课程请求
     */
    @Transactional
    public void updateCollaborators(String bizType, Long bizId, RangeBaseRequest request) {
        CollaboratorsDTO collaborators = request.getCollaborators();
        if ("ALL".equalsIgnoreCase(collaborators.getEditorType())) {
            collaborators.setEditors(Lists.newArrayList(DEFAULT_USER_ID));
        }
        if ("ALL".equalsIgnoreCase(collaborators.getUserType())) {
            collaborators.setUsers(Lists.newArrayList(DEFAULT_USER_ID));
        }
        // 删除协作者配置
        CommonRangeDeleteRequest deleteRequest = new CommonRangeDeleteRequest();
        deleteRequest.setModelType(RangeModelTypeEnums.COLLABORATORS.getCode());
        deleteRequest.setType(bizType);
        deleteRequest.setTypeId(bizId);
        commonRangeInterface.deleteRangeByBusinessId(deleteRequest);

        // 删除编辑者配置
        CommonRangeDeleteRequest deleteEditorRequest = new CommonRangeDeleteRequest();
        deleteEditorRequest.setModelType(RangeModelTypeEnums.EDITORS.getCode());
        deleteEditorRequest.setType(bizType);
        deleteEditorRequest.setTypeId(bizId);
        commonRangeInterface.deleteRangeByBusinessId(deleteEditorRequest);

        // 设置新范围
        setCollaborators(bizType, bizId, request);
    }


    /**
     * 处理可见范围信息
     *
     * @param bizType 业务类型
     * @param bizId   业务id
     */
    public VisibilityDTO processVisibilityInfo(String bizType, Long bizId) {
        // 查询可见范围配置
        CommonRangeQueryRequest request = new CommonRangeQueryRequest();
        request.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        request.setType(bizType);
        request.setTypeId(bizId);

        CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(request);

        VisibilityDTO visibilityDTO = new VisibilityDTO();

        if (response == null) {
            return visibilityDTO;
        }

        if (CollectionUtils.isNotEmpty(response.getUserIds())
                && response.getUserIds().contains(DEFAULT_USER_ID)) {
            // 所有人可见
            visibilityDTO.setType("ALL");
            return visibilityDTO;
        }

        // 有指定范围
        visibilityDTO.setType("PART");
        // 设置可见目标
        List<TargetDTO> targets = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(response.getDepartmentIds())) {
            targets.add(new TargetDTO("department", response.getDepartmentIds()));
        }
        if (CollectionUtils.isNotEmpty(response.getRoleIds())) {
            targets.add(new TargetDTO("role", response.getRoleIds()));
        }
        if (CollectionUtils.isNotEmpty(response.getUserIds())) {
            targets.add(new TargetDTO("user", response.getUserIds()));
        }
        visibilityDTO.setTargets(targets);

        return visibilityDTO;
    }


    /**
     * 处理协同管理信息
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     */
    public CollaboratorsDTO processCollaboratorsInfo(String bizType, Long bizId) {
        // 查询协同管理配置
        CommonRangeQueryRequest request = new CommonRangeQueryRequest();
        request.setModelType(RangeModelTypeEnums.COLLABORATORS.getCode()); // 协同管理类型
        request.setType(bizType);
        request.setTypeId(bizId);

        CommonRangeQueryResponse usersResponse = commonRangeInterface.queryRangeConfigByBusinessId(request);
        // 协同使用
        CollaboratorsDTO collaboratorsDTO = new CollaboratorsDTO();
        // 判断协同使用的类型
        if (Objects.isNull(usersResponse)
                || (usersResponse.getUserIds() != null
                && usersResponse.getUserIds().isEmpty())) {
            collaboratorsDTO.setUserType("NONE");
        } else if (Objects.nonNull(usersResponse.getUserIds())
                && usersResponse.getUserIds().contains(DEFAULT_USER_ID)) {
            collaboratorsDTO.setUserType("ALL");
        } else {
            collaboratorsDTO.setUserType("PART");
        }
        collaboratorsDTO.setUsers(usersResponse.getUserIds());

        // 协同编辑
        CommonRangeQueryRequest editorRequest = new CommonRangeQueryRequest();
        editorRequest.setModelType(RangeModelTypeEnums.EDITORS.getCode()); // 编辑类型
        editorRequest.setType(bizType);
        editorRequest.setTypeId(bizId);
        // 查询编辑
        CommonRangeQueryResponse editorResponse = commonRangeInterface.queryRangeConfigByBusinessId(editorRequest);
        // 组装结果
        List<Long> editors = new ArrayList<>();
        // 设置编辑者和用户
        if (editorResponse != null && CollectionUtils.isNotEmpty(editorResponse.getUserIds())) {
            editors.addAll(editorResponse.getUserIds());
        }
        // 判断协同编辑的类型
        if (Objects.isNull(editorResponse)
                || (editorResponse.getUserIds() != null
                && editorResponse.getUserIds().isEmpty())) {
            collaboratorsDTO.setEditorType("NONE");
        } else if (Objects.nonNull(editorResponse.getUserIds())
                && editorResponse.getUserIds().contains(DEFAULT_USER_ID)) {
            collaboratorsDTO.setEditorType("ALL");
        } else {
            collaboratorsDTO.setEditorType("PART");
        }
        collaboratorsDTO.setEditors(editors);

        return collaboratorsDTO;
    }

}
