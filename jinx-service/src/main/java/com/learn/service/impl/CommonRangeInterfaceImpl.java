package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import com.learn.common.util.ConverterUtils;
import com.learn.constants.BizConstants;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.*;
import com.learn.common.exception.CommonException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.learn.common.util.ConverterUtils.parseTargetIds;

/**
 * 通用范围处理表实现类
 */
@Service
public class CommonRangeInterfaceImpl implements CommonRangeInterface {

    private static final Logger log = LoggerFactory.getLogger(CommonRangeInterfaceImpl.class);

    @Autowired
    private CommonRangeMapper commonRangeMapper;
    
    @Autowired
    private DepartmentUserMapper departmentUserMapper;
    
    @Autowired
    private OrgRoleUserMapper orgRoleUserMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private CoursesMapper coursesMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;

    /**
     * 批量创建范围
     *
     * @param request 创建范围请求对象
     * @return 创建范围响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonRangeCreateResponse batchCreateRange(CommonRangeCreateRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getModelType())
                || StringUtils.isEmpty(request.getType())
                || request.getTypeId() == null
                || Objects.isNull(request.getTargetTypeAndIds())) {
            throw new CommonException("参数不能为空");
        }

        // 准备批量插入的数据
        List<CommonRange> rangeList = new ArrayList<>();
        Date now = new Date();

        // 遍历每种目标类型及其对应的ID列表
        for (Map.Entry<String, List<Long>> entry : request.getTargetTypeAndIds().entrySet()) {
            String targetType = entry.getKey();
            List<Long> targetIds = entry.getValue().stream().distinct().collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(targetIds)) {
                // 创建CommonRange对象
                CommonRange commonRange = new CommonRange();
                commonRange.setModelType(request.getModelType());
                commonRange.setType(request.getType());
                commonRange.setTypeId(request.getTypeId());
                // 设置typeBizId字段，格式：{type}_{typeId}
                commonRange.setTypeBizId(request.getType() + "_" + request.getTypeId());
                commonRange.setTargetType(targetType);
                // 将List<Long>转换为JSON字符串格式 [1,2,3,4]
                commonRange.setTargetIds(targetIds.toString().replace(" ", ""));
                commonRange.setCreatorId(request.getCreatorId());
                commonRange.setCreatorName(request.getCreatorName());
                commonRange.setGmtCreate(now);
                commonRange.setGmtModified(now);
                commonRange.setIsDel(0);

                rangeList.add(commonRange);
            }
        }

        // 批量插入数据
        if (!rangeList.isEmpty()) {
            commonRangeMapper.batchInsert(rangeList);
        }

        // 设置响应
        CommonRangeCreateResponse response = new CommonRangeCreateResponse();
        response.setSuccess(true);
        response.setRangeIds(new ArrayList<>());
        for (CommonRange range : rangeList) {
            response.getRangeIds().add(range.getId());
        }

        return response;
    }

    /**
     * 根据目标ID查询在范围内的业务ID列表
     *
     * @param request 查询范围请求对象
     * @return 查询范围响应对象列表
     */
    @Override
    public List<CommonRangeQueryResponse> queryBusinessIdsByTargets(CommonRangeQueryRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getModelType()) || StringUtils.isEmpty(request.getType())) {
            throw new CommonException("参数不能为空");
        }

        // 使用全文索引查询
        List<CommonRange> ranges = commonRangeMapper.queryRangesByTargets(
                request.getModelType(), request.getType(), request.getTargetTypeAndIds());

        // 按业务ID分组
        Map<Long, List<CommonRange>> businessIdRangesMap = ranges.stream()
                .collect(Collectors.groupingBy(CommonRange::getTypeId));

        // 构建响应对象
        List<CommonRangeQueryResponse> responseList = new ArrayList<>();
        for (Map.Entry<Long, List<CommonRange>> entry : businessIdRangesMap.entrySet()) {
            Long businessId = entry.getKey();
            List<CommonRange> businessRanges = entry.getValue();

            CommonRangeQueryResponse response = new CommonRangeQueryResponse();
            response.setSuccess(true);
            response.setTypeId(businessId);
            
            // 设置业务类型
            if (!businessRanges.isEmpty()) {
                response.setType(businessRanges.get(0).getType());
            }

            // 提取部门、角色、用户ID
            List<Long> departmentIds = new ArrayList<>();
            List<Long> roleIds = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();

            for (CommonRange range : businessRanges) {
                String targetType = range.getTargetType();
                String targetIdsStr = range.getTargetIds();

                if (targetIdsStr != null && targetIdsStr.length() > 2) {
                    List<Long> targetIds = parseTargetIds(targetIdsStr);

                    if ("department".equals(targetType)) {
                        departmentIds.addAll(targetIds);
                    } else if ("role".equals(targetType)) {
                        roleIds.addAll(targetIds);
                    } else if ("user".equals(targetType)) {
                        userIds.addAll(targetIds);
                    }
                }
            }

            response.setDepartmentIds(departmentIds);
            response.setRoleIds(roleIds);
            response.setUserIds(userIds);

            responseList.add(response);
        }

        return responseList;
    }

    /**
     * 查询业务ID配置的范围信息
     *
     * @param request 查询范围请求对象
     * @return 查询范围响应对象
     */
    @Override
    public CommonRangeQueryResponse queryRangeConfigByBusinessId(CommonRangeQueryRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getModelType()) || StringUtils.isEmpty(request.getType()) ||
                request.getTypeId() == null) {
            throw new CommonException("参数不能为空");
        }

        // 查询范围配置
        List<CommonRange> ranges = commonRangeMapper.queryRangeByBusinessId(
                request.getModelType(), request.getType(), request.getTypeId());

        // 提取部门、角色、用户ID
        List<Long> departmentIds = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        for (CommonRange range : ranges) {
            String targetType = range.getTargetType();
            String targetIdsStr = range.getTargetIds();

            if (targetIdsStr != null && targetIdsStr.length() > 2) {
                List<Long> targetIds = parseTargetIds(targetIdsStr);
                if ("department".equals(targetType)) {
                    departmentIds.addAll(targetIds);
                } else if ("role".equals(targetType)) {
                    roleIds.addAll(targetIds);
                } else if ("user".equals(targetType)) {
                    userIds.addAll(targetIds);
                }
            }
        }
        // 设置响应
        CommonRangeQueryResponse response = new CommonRangeQueryResponse();
        response.setSuccess(true);
        response.setTypeId(request.getTypeId());
        response.setType(request.getType());
        response.setDepartmentIds(departmentIds);
        response.setRoleIds(roleIds);
        response.setUserIds(userIds);
        if (CollectionUtils.isNotEmpty(ranges)) {
            response.setCreatorId(ranges.getFirst().getCreatorId());
        }

        return response;
    }

    /**
     * 删除业务ID的范围配置
     *
     * @param request 删除范围请求对象
     * @return 删除范围响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRangeByBusinessId(CommonRangeDeleteRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getModelType()) || StringUtils.isEmpty(request.getType()) ||
                request.getTypeId() == null) {
            throw new CommonException("参数不能为空");
        }// 构建更新条件
        LambdaUpdateWrapper<CommonRange> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CommonRange::getModelType, request.getModelType())
                .eq(CommonRange::getType, request.getType())
                .eq(CommonRange::getTypeId, request.getTypeId())
                .set(CommonRange::getIsDel, 1)
                .set(CommonRange::getGmtModified, new Date());

        // 执行逻辑删除
        commonRangeMapper.update(null, updateWrapper);// 设置响应

        return true;
    }

    /**
     * 更新业务ID的范围配置
     *
     * @param request 更新范围请求对象
     * @return 更新范围响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonRangeUpdateResponse updateRangeByBusinessId(CommonRangeUpdateRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getModelType())
                || StringUtils.isEmpty(request.getType())
                || request.getTypeId() == null
                || Objects.isNull(request.getTargetTypeAndIds())) {
            throw new CommonException("参数不能为空");
        }

        // 先删除原有的范围配置
        CommonRangeDeleteRequest deleteRequest = new CommonRangeDeleteRequest();
        deleteRequest.setModelType(request.getModelType());
        deleteRequest.setType(request.getType());
        deleteRequest.setTypeId(request.getTypeId());
        Boolean result = deleteRangeByBusinessId(deleteRequest);

        if (!Objects.equals(result, true)) {
            throw new CommonException("删除原有范围配置失败");
        }

        // 再创建新的范围配置
        CommonRangeCreateRequest createRequest = new CommonRangeCreateRequest();
        createRequest.setModelType(request.getModelType());
        createRequest.setType(request.getType());
        createRequest.setTypeId(request.getTypeId());
        createRequest.setTargetTypeAndIds(request.getTargetTypeAndIds());
        createRequest.setCreatorId(request.getUpdaterId());
        createRequest.setCreatorName(request.getUpdaterName());
        CommonRangeCreateResponse createResponse = batchCreateRange(createRequest);

        if (!createResponse.getSuccess()) {
            throw new CommonException("创建新范围配置失败");
        }

        // 设置响应
        CommonRangeUpdateResponse response = new CommonRangeUpdateResponse();
        response.setSuccess(true);
        response.setUpdatedCount(1);

        return response;
    }

    /**
     * 批量场景下
     * @param request
     */
    @Override
    public void checkUserHasRightsIfNotThrowException(CommonRangeQueryRequest request) {

        if (Objects.nonNull(request.getTypeId())) {
            if (CollectionUtils.isEmpty(request.getTypeIds())){
                request.setTypeIds(new ArrayList<>());
            }
            request.getTypeIds().add(request.getTypeId());
        }
        // 参数校验
        if (StringUtils.isEmpty(request.getUserId())
                || StringUtils.isEmpty(request.getModelType())
                || StringUtils.isEmpty(request.getType())
                || CollectionUtils.isEmpty(request.getTypeIds())) {
            throw new CommonException("参数不能为空");
        }
        // 当前用户创建的资源
        List<Long> creatorTypeIds = getCreatorTypeIds(request);
        List<Long> typeIds = request.getTypeIds();
        typeIds.removeAll(creatorTypeIds);
        if (CollectionUtils.isEmpty(typeIds)) {
            return;
        }
        // 复用已有方法，在某个业务模块ID下，通过用户ID及其部门、角色判断是否有权限
        List<CommonRangeQueryResponse> responses = listCommonRangeByCondition(request);
        List<Long> hasRightsTypeIds = responses.stream().map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        List<Long> noRightsTypeIds = new ArrayList<>();
        for (Long typeId : request.getTypeIds()) {
            if (hasRightsTypeIds.contains(typeId)) {
                continue;
            }
            noRightsTypeIds.add(typeId);
        }
        if (CollectionUtils.isNotEmpty(noRightsTypeIds)) {
            throw new CommonException("没有操作以下id的权限:" + String.join(",", noRightsTypeIds
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())));
        }

    }

    @NotNull
    private List<Long> getCreatorTypeIds(CommonRangeQueryRequest request) {
        // 检查用户是否为创建者，如果是创建者，则拥有所有权限，无需经过后续的校验
        Long userId = Long.valueOf(request.getUserId());
        String type = request.getType();
        List<Long> typeIds = request.getTypeIds();

        // 根据type和typeIds查询对应的实体，检查用户是否为创建者
        List<Long> creatorTypeIds = new ArrayList<>();

        if (BizType.TRAIN.equals(type)) {
            // 查询培训实体
            LambdaQueryWrapper<Train> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Train::getId, typeIds)
                    .eq(Train::getCreatorId, userId)
                    .eq(Train::getIsDel, 0);
            List<Train> trains = trainMapper.selectList(queryWrapper);
            creatorTypeIds.addAll(trains.stream().map(Train::getId).collect(Collectors.toList()));
        } else if (BizType.COURSE.equals(type)) {
            // 查询课程实体
            LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Courses::getId, typeIds)
                    .eq(Courses::getCreatorId, userId)
                    .eq(Courses::getIsDel, 0);
            List<Courses> courses = coursesMapper.selectList(queryWrapper);
            creatorTypeIds.addAll(courses.stream().map(Courses::getId).collect(Collectors.toList()));
        } else if (BizType.LEARNING_MAP.equals(type)) {
            // 查询学习地图实体
            LambdaQueryWrapper<LearningMap> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(LearningMap::getId, typeIds)
                    .eq(LearningMap::getCreatorId, userId)
                    .eq(LearningMap::getIsDel, 0);
            List<LearningMap> learningMaps = learningMapMapper.selectList(queryWrapper);
            creatorTypeIds.addAll(learningMaps.stream().map(LearningMap::getId).collect(Collectors.toList()));
        }
        return creatorTypeIds;
    }

    /**
     * 判断用户是否有权限
     * @param request 查询范围请求对象
     * @return 是否有权限
     */
    @Override
    public Boolean checkUserHasRight(CommonRangeQueryRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getUserId()) || StringUtils.isEmpty(request.getModelType()) || 
            StringUtils.isEmpty(request.getType()) || request.getTypeId() == null) {
            throw new CommonException("参数不能为空");
        }
        
        // 复用已有方法，在某个业务模块ID下，通过用户ID及其部门、角色判断是否有权限
        List<CommonRangeQueryResponse> responses = listCommonRangeByCondition(request);
        
        // 如果列表为空或无权限项，返回false
        if (responses.isEmpty()) {
            return false;
        }
        
        // 检查是否包含指定的业务ID
        for (CommonRangeQueryResponse response : responses) {
            if (Objects.equals(response.getTypeId(), request.getTypeId())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 根据用户ID和业务模块类型查询有权限的业务ID列表
     * 1. MUST 先根据用户 查询用户的角色、部门、人员id
     * 2. 根据角色、部门、人员id、功能枚举、业务模块类型、业务模块id，查询角色、部门、人员id是否在范围内
     * @param request 查询范围请求对象
     * @return 查询范围响应对象列表
     */
    @Override
    public List<CommonRangeQueryResponse> listCommonRangeByCondition(CommonRangeQueryRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getUserId()) || StringUtils.isEmpty(request.getModelType())) {
            throw new CommonException("用户ID和功能模块类型不能为空");
        }
        
        try {
            // 1. 必须先查询用户的角色、部门ID列表
            List<Long> departmentIds = queryUserDepartmentIds(request.getUserId());
            List<Long> roleIds = queryUserRoleIds(request.getUserId());
            
            log.info("用户[{}]的部门IDs: {}, 角色IDs: {}", request.getUserId(), departmentIds, roleIds);
            List<String> userIds = Lists.newArrayList(BizConstants.DEFAULT_USER_ID.toString());
            if (StringUtils.isNotBlank(request.getUserId())) {
                userIds.add(request.getUserId());
            }
            // 2. 根据用户ID、部门ID、角色ID查询用户有权限的业务ID列表
            List<CommonRange> ranges = commonRangeMapper.queryRangesByUser(
                    request.getModelType(), 
                    request.getType(),
                    userIds,
                    departmentIds, 
                    roleIds);
            
            if (CollectionUtils.isEmpty(ranges)) {
                return Collections.emptyList();
            }
            
            // 3. 按业务ID和业务类型分组处理结果
            Map<String, List<CommonRange>> groupedRanges = ranges.stream()
                    .collect(Collectors.groupingBy(range -> range.getTypeId() + "_" + range.getType()));
            
            List<CommonRangeQueryResponse> responseList = new ArrayList<>();
            
            for (Map.Entry<String, List<CommonRange>> entry : groupedRanges.entrySet()) {
                List<CommonRange> rangeList = entry.getValue();
                if (CollectionUtils.isEmpty(rangeList)) {
                    continue;
                }
                
                CommonRange firstRange = rangeList.get(0);
                
                // 解析目标ID
                List<Long> resultUserIds = new ArrayList<>();
                List<Long> deptIds = new ArrayList<>();
                List<Long> rIds = new ArrayList<>();
                
                for (CommonRange range : rangeList) {
                    String targetType = range.getTargetType();
                    List<Long> targetIds = ConverterUtils.parseTargetIds(range.getTargetIds());
                    
                    if ("user".equals(targetType)) {
                        resultUserIds.addAll(targetIds);
                    } else if ("department".equals(targetType)) {
                        deptIds.addAll(targetIds);
                    } else if ("role".equals(targetType)) {
                        rIds.addAll(targetIds);
                    }
                }
                
                // 构建响应对象
                CommonRangeQueryResponse response = new CommonRangeQueryResponse();
                response.setSuccess(true);
                response.setTypeId(firstRange.getTypeId());
                response.setType(firstRange.getType());
                response.setUserIds(resultUserIds);
                response.setDepartmentIds(deptIds);
                response.setRoleIds(rIds);
                
                responseList.add(response);
            }
            
            return responseList;
            
        } catch (Exception e) {
            log.error("查询用户权限列表失败", e);
            throw new CommonException("查询用户权限列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据范围ids查询范围
     *
     * @param rangeIds 范围ID列表
     * @return 范围对象列表
     */
    @Override
    public List<CommonRange> listCommonRangeByRangeIds(List<Long> rangeIds) {
        if (CollectionUtils.isEmpty(rangeIds)) {
            return Collections.emptyList();
        }
        
        try {
            LambdaQueryWrapper<CommonRange> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(CommonRange::getId, rangeIds)
                    .eq(CommonRange::getIsDel, 0);
            
            return commonRangeMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("根据范围IDs查询范围失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询用户所属部门ID列表
     * @param userId 用户ID
     * @return 部门ID列表
     */
    private List<Long> queryUserDepartmentIds(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return Collections.emptyList();
        }
        
        try {
            // 查询用户所属部门
            LambdaQueryWrapper<DepartmentUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DepartmentUser::getUserId, Long.valueOf(userId))
                       .eq(DepartmentUser::getIsDel, 0);
            
            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(departmentUsers)) {
                return Collections.emptyList();
            }
            
            // 提取部门ID列表
            return departmentUsers.stream()
                               .map(DepartmentUser::getDepartmentId)
                               .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询用户部门失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 查询用户所属角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    private List<Long> queryUserRoleIds(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return Collections.emptyList();
        }
        
        try {
            // 查询用户所属角色
            LambdaQueryWrapper<OrgRoleUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrgRoleUser::getUserId, Long.valueOf(userId))
                       .eq(OrgRoleUser::getIsDel, 0);
            
            List<OrgRoleUser> roleUsers = orgRoleUserMapper.selectList(queryWrapper);
            
            if (CollectionUtils.isEmpty(roleUsers)) {
                return Collections.emptyList();
            }
            
            // 提取角色ID列表
            return roleUsers.stream()
                         .map(OrgRoleUser::getOrgRoleId)
                         .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询用户角色失败", e);
            return Collections.emptyList();
        }
    }

}
