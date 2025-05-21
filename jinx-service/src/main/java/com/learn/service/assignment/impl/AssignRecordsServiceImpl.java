package com.learn.service.assignment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.enums.RangeTargetTypeEnums;
import com.learn.common.util.ConverterUtils;
import com.learn.dto.assignment.AssignRecordDetailQueryRequest;
import com.learn.dto.assignment.AssignRecordDetailQueryResponse;
import com.learn.dto.assignment.AssignRecordsQueryRequest;
import com.learn.dto.assignment.AssignRecordsQueryResponse;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.AssignmentDetail;
import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.AssignRecordsMapper;
import com.learn.infrastructure.repository.mapper.AssignmentDetailMapper;
import com.learn.infrastructure.repository.mapper.CommonRangeMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.assignment.AssignRecordsService;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.org.OrgDTO;
import com.learn.service.dto.org.OrgRequest;
import com.learn.service.dto.role.RoleDTO;
import com.learn.service.dto.role.RoleRequest;
import com.learn.service.org.OrgService;
import com.learn.service.role.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 指派记录服务实现类
 * 处理指派记录相关的业务逻辑
 */
@Service
@Slf4j
public class AssignRecordsServiceImpl implements AssignRecordsService {

    @Autowired
    private AssignRecordsMapper assignRecordsMapper;

    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;

    @Autowired
    private CommonRangeMapper commonRangeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrgService orgService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 查询指派记录
     * 根据type_{type_id}查询assign_records中删除状态为false的数据
     * 并根据range_ids查询门店、角色、人员的名称
     *
     * @param request 查询请求参数
     * @return 指派记录列表
     */
    @Override
    public List<AssignRecordsQueryResponse> queryAssignRecords(AssignRecordsQueryRequest request) {
        log.info("查询指派记录，参数：{}", request);

        if (request == null || request.getType() == null || request.getTypeId() == null) {
            log.error("查询指派记录参数不能为空");
            return Collections.emptyList();
        }

        // 构建查询条件
        LambdaQueryWrapper<AssignRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignRecords::getType, request.getType())
                .eq(AssignRecords::getTypeId, request.getTypeId())
                .eq(AssignRecords::getIsDel, false)
                .orderByDesc(AssignRecords::getGmtCreate);

        // 分页查询
        Page<AssignRecords> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<AssignRecords> resultPage = assignRecordsMapper.selectPage(page, queryWrapper);

        // 转换为响应对象
        List<AssignRecordsQueryResponse> responseList = new ArrayList<>();
        for (AssignRecords record : resultPage.getRecords()) {
            AssignRecordsQueryResponse response = convertToResponse(record);
            responseList.add(response);
        }

        log.info("查询指派记录成功，共{}条记录", responseList.size());
        return responseList;
    }

    /**
     * 查询指派记录总数
     *
     * @param request 查询请求参数
     * @return 记录总数
     */
    @Override
    public Long countAssignRecords(AssignRecordsQueryRequest request) {
        log.info("统计指派记录数量，参数：{}", request);

        if (request == null || request.getType() == null || request.getTypeId() == null) {
            log.error("统计指派记录参数不能为空");
            return 0L;
        }

        // 构建查询条件
        LambdaQueryWrapper<AssignRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignRecords::getType, request.getType())
                .eq(AssignRecords::getTypeId, request.getTypeId())
                .eq(AssignRecords::getIsDel, false);

        // 统计记录数
        long count = assignRecordsMapper.selectCount(queryWrapper);

        log.info("统计指派记录数量成功，共{}条记录", count);
        return count;
    }

    /**
     * 查询指派记录明细
     * 根据指派记录ID查询assignment_detail中的明细
     *
     * @param request 查询请求参数
     * @return 指派记录明细列表
     */
    @Override
    public List<AssignRecordDetailQueryResponse> queryAssignRecordDetails(AssignRecordDetailQueryRequest request) {
        log.info("查询指派记录明细，参数：{}", request);

        if (request == null || request.getAssignRecordId() == null) {
            log.error("查询指派记录明细参数不能为空");
            return Collections.emptyList();
        }

        // 构建查询条件
        LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignmentDetail::getAssignRecordId, request.getAssignRecordId())
                .eq(AssignmentDetail::getIsDel, 0)
                .orderByDesc(AssignmentDetail::getGmtCreate);

        // 分页查询
        Page<AssignmentDetail> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<AssignmentDetail> resultPage = assignmentDetailMapper.selectPage(page, queryWrapper);

        // 获取所有用户ID
        Set<Long> userIds = resultPage.getRecords().stream()
                .map(AssignmentDetail::getUserid)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getUserId, userIds)
                    .eq(User::getIsDel, 0);
            List<User> users = userMapper.selectList(userQueryWrapper);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, user -> user, (u1, u2) -> u1));
        }

        // 转换为响应对象
        List<AssignRecordDetailQueryResponse> responseList = new ArrayList<>();
        for (AssignmentDetail detail : resultPage.getRecords()) {
            AssignRecordDetailQueryResponse response = convertToDetailResponse(detail, userMap);
            responseList.add(response);
        }

        log.info("查询指派记录明细成功，共{}条记录", responseList.size());
        return responseList;
    }

    /**
     * 查询指派记录明细总数
     *
     * @param request 查询请求参数
     * @return 记录总数
     */
    @Override
    public Long countAssignRecordDetails(AssignRecordDetailQueryRequest request) {
        log.info("统计指派记录明细数量，参数：{}", request);

        if (request == null || request.getAssignRecordId() == null) {
            log.error("统计指派记录明细参数不能为空");
            return 0L;
        }

        try {
            // 构建查询条件
            LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AssignmentDetail::getAssignRecordId, request.getAssignRecordId())
                    .eq(AssignmentDetail::getIsDel, 0);

            // 统计记录数
            long count = assignmentDetailMapper.selectCount(queryWrapper);

            log.info("统计指派记录明细数量成功，共{}条记录", count);
            return count;
        } catch (Exception e) {
            log.error("统计指派记录明细数量失败", e);
            return 0L;
        }
    }

    /**
     * 将实体转换为响应对象
     *
     * @param record 指派记录实体
     * @return 指派记录响应对象
     */
    private AssignRecordsQueryResponse convertToResponse(AssignRecords record) {
        AssignRecordsQueryResponse response = new AssignRecordsQueryResponse();
        response.setId(record.getId());
        response.setType(record.getType());
        response.setTypeId(record.getTypeId());
        response.setRangeIds(record.getRangeIds());
        response.setDeadline(record.getDeadline());
        response.setAssignType(record.getAssignType());
        response.setStatus(record.getStatus());
        response.setGmtCreate(record.getGmtCreate());
        response.setCreatorId(record.getCreatorId());
        response.setCreatorName(record.getCreatorName());

        // 解析范围ID列表
        List<Long> rangeIds = parseRangeIds(record.getRangeIds());
        if (!CollectionUtils.isEmpty(rangeIds)) {
            // 查询范围信息
            List<CommonRange> rangeList = commonRangeInterface.listCommonRangeByRangeIds(rangeIds);
            if (!CollectionUtils.isEmpty(rangeList)) {
                // 按范围类型分组
                Map<String, List<CommonRange>> rangeTypeMap = rangeList.stream()
                        .collect(Collectors.groupingBy(CommonRange::getTargetType));

                // 处理部门范围
                List<CommonRange> departmentRanges = rangeTypeMap.getOrDefault(RangeTargetTypeEnums.DEPARTMENT.getCode(), Collections.emptyList());
                if (!CollectionUtils.isEmpty(departmentRanges)) {
                    List<Long> departmentIds = new ArrayList<>();
                    for (CommonRange range : departmentRanges) {
                        departmentIds.addAll(ConverterUtils.parseTargetIds(range.getTargetIds()));
                    }

                    if (!CollectionUtils.isEmpty(departmentIds)) {
                        // 获取部门信息
                        OrgRequest orgRequest = new OrgRequest();
                        orgRequest.setOrgIds(departmentIds);
                        BaseResponse<OrgDTO> orgResponse = orgService.queryOrgs(orgRequest);

                        if (orgResponse != null && !CollectionUtils.isEmpty(orgResponse.getData())) {
                            List<AssignRecordsQueryResponse.DepartmentInfo> departmentInfos = orgResponse.getData().stream().map(org -> {
                                AssignRecordsQueryResponse.DepartmentInfo departmentInfo = new AssignRecordsQueryResponse.DepartmentInfo();
                                departmentInfo.setDepartmentId(org.getId());
                                departmentInfo.setDepartmentName(org.getName());
                                return departmentInfo;
                            }).collect(Collectors.toList());

                            response.setDepartments(departmentInfos);
                        }
                    }
                }

                // 处理角色范围
                List<CommonRange> roleRanges = rangeTypeMap.getOrDefault(RangeTargetTypeEnums.ROLE.getCode(), Collections.emptyList());
                if (!CollectionUtils.isEmpty(roleRanges)) {
                    List<Long> roleIds = new ArrayList<>();
                    for (CommonRange range : roleRanges) {
                        roleIds.addAll(ConverterUtils.parseTargetIds(range.getTargetIds()));
                    }

                    if (!CollectionUtils.isEmpty(roleIds)) {
                        // 获取角色信息
                        RoleRequest roleRequest = new RoleRequest();
                        roleRequest.setIds(roleIds);
                        BaseResponse<RoleDTO> roleResponse = roleService.queryRoles(roleRequest);

                        if (roleResponse != null && !CollectionUtils.isEmpty(roleResponse.getData())) {
                            List<AssignRecordsQueryResponse.RoleInfo> roleInfos = roleResponse.getData().stream().map(role -> {
                                AssignRecordsQueryResponse.RoleInfo roleInfo = new AssignRecordsQueryResponse.RoleInfo();
                                roleInfo.setRoleId(role.getId());
                                roleInfo.setRoleName(role.getName());
                                return roleInfo;
                            }).collect(Collectors.toList());

                            response.setRoles(roleInfos);
                        }
                    }
                }

                // 处理用户范围
                List<CommonRange> userRanges = rangeTypeMap.getOrDefault(RangeTargetTypeEnums.USER.getCode(), Collections.emptyList());
                if (!CollectionUtils.isEmpty(userRanges)) {
                    List<Long> userIds = new ArrayList<>();
                    for (CommonRange range : userRanges) {
                        userIds.addAll(ConverterUtils.parseTargetIds(range.getTargetIds()));
                    }

                    if (!CollectionUtils.isEmpty(userIds)) {
                        // 查询用户信息
                        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
                        userQueryWrapper.in(User::getUserId, userIds)
                                .eq(User::getIsDel, 0);
                        List<User> users = userMapper.selectList(userQueryWrapper);

                        if (!CollectionUtils.isEmpty(users)) {
                            List<AssignRecordsQueryResponse.UserInfo> userInfos = users.stream().map(user -> {
                                AssignRecordsQueryResponse.UserInfo userInfo = new AssignRecordsQueryResponse.UserInfo();
                                userInfo.setUserId(user.getId());
                                userInfo.setUserName(user.getNickname());
                                return userInfo;
                            }).collect(Collectors.toList());

                            response.setUsers(userInfos);
                        }
                    }
                }
            }
        }

        return response;
    }

    /**
     * 将明细实体转换为响应对象
     *
     * @param detail  指派明细实体
     * @param userMap 用户信息映射
     * @return 指派明细响应对象
     */
    private AssignRecordDetailQueryResponse convertToDetailResponse(AssignmentDetail detail, Map<Long, User> userMap) {
        AssignRecordDetailQueryResponse response = new AssignRecordDetailQueryResponse();
        response.setId(detail.getId());
        response.setUserId(detail.getUserid());
        response.setStatus(detail.getStatus());
        response.setGmtCreate(detail.getGmtCreate());
        response.setGmtModified(detail.getGmtModified());

        // 设置用户名称
        User user = userMap.get(detail.getUserid());
        if (user != null) {
            response.setUserName(user.getNickname());
        }

        // 解析失败原因
        if (StringUtils.hasText(detail.getAttributes())) {
            String attributes = detail.getAttributes();
            if (attributes.contains("reason")) {
                // 简单解析JSON字符串中的reason字段
                int reasonStart = attributes.indexOf("\"reason\":\"");
                if (reasonStart != -1) {
                    reasonStart += 10; // "reason":"的长度
                    int reasonEnd = attributes.indexOf("\"", reasonStart);
                    if (reasonEnd != -1) {
                        String reason = attributes.substring(reasonStart, reasonEnd);
                        response.setFailReason(reason);
                    }
                }
            }
        }

        return response;
    }

    /**
     * 解析范围ID字符串
     *
     * @param rangeIdsStr 范围ID字符串，格式为 [1,2,3,4]
     * @return 范围ID列表
     */
    private List<Long> parseRangeIds(String rangeIdsStr) {
        List<Long> rangeIds = new ArrayList<>();

        if (rangeIdsStr != null && rangeIdsStr.length() > 2) {
            // 去掉方括号，并按逗号分割
            String[] idsStr = rangeIdsStr.substring(1, rangeIdsStr.length() - 1).split(",");
            for (String idStr : idsStr) {
                try {
                    rangeIds.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略无法解析的ID
                    log.warn("无法解析的ID: {}", idStr);
                }
            }
        }

        return rangeIds;
    }
}
