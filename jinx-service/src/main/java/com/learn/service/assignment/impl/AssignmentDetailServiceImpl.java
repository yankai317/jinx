package com.learn.service.assignment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.enums.*;
import com.learn.common.exception.CommonException;
import com.learn.common.util.ConverterUtils;
import com.learn.dto.assignment.AssignmentDetailRequest;
import com.learn.dto.assignment.AssignmentDetailResponse;
import com.learn.dto.common.AssignRecordRequest;
import com.learn.dto.common.AssignRecordResponse;
import com.learn.dto.common.AssignRequest;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.CommonRangeInterface;
import com.learn.service.assignment.AssignmentDetailService;
import com.learn.service.common.strategy.AssignTypeStrategy;
import com.learn.service.common.strategy.AssignTypeStrategyFactory;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 指派明细服务实现类
 * 处理指派明细相关的业务逻辑
 */
@Service
@Slf4j
public class AssignmentDetailServiceImpl implements AssignmentDetailService {

    @Autowired
    private CommonRangeMapper commonRangeMapper;

    @Autowired
    private AssignmentDetailMapper assignmentDetailMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private OrgRoleUserMapper orgRoleUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private AssignRecordsMapper assignRecordsMapper;

    @Autowired
    private OrgService orgService;

    @Autowired
    private RoleService roleService;


    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    @Autowired
    private AssignTypeStrategyFactory assignTypeStrategyFactory;

    /**
     * 同步指派范围到指派明细
     * 将指派范围中的部门和角色转换为具体的人员，并更新到指派明细表
     * 优化：使用批量操作替代一个一个查询或插入数据库
     *
     * @return 新增的指派明细数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncAssignmentRangeToDetail() {
        log.info("开始同步指派范围到指派明细");
        int totalInserted = 0;

        try {
            // 1. 批量读取 assign_records 表中还没截止的指派
            List<AssignRecords> assignRecords = getNotExpiredAssignRecords();
            log.info("查询到{}条未截止的指派记录", assignRecords.size());

            if (CollectionUtils.isEmpty(assignRecords)) {
                log.info("没有需要处理的指派记录");
                return 0;
            }

            // 2. 分组处理不同类型的指派记录，优先处理单次指派
            List<AssignRecords> onceRecords = new ArrayList<>();
            List<AssignRecords> autoRecords = new ArrayList<>();

            // 按指派类型分组
            for (AssignRecords record : assignRecords) {
                if (AssignTypeEnums.ONCE.getCode().equals(record.getAssignType())) {
                    onceRecords.add(record);
                } else if (AssignTypeEnums.AUTO.getCode().equals(record.getAssignType())) {
                    autoRecords.add(record);
                }
            }

            // 先处理单次指派记录
            log.info("开始处理{}条单次指派记录", onceRecords.size());
            for (AssignRecords record : onceRecords) {
                int inserted = processAssignRecord(record);
                totalInserted += inserted;
            }
            log.info("单次指派处理完成，共处理{}条记录", onceRecords.size());

            // 再处理自动指派记录
            log.info("开始处理{}条自动指派记录", autoRecords.size());
            for (AssignRecords record : autoRecords) {
                int inserted = processAssignRecord(record);
                totalInserted += inserted;
            }
            log.info("自动指派处理完成，共处理{}条记录", autoRecords.size());

            log.info("同步指派范围到指派明细完成，共新增{}条记录", totalInserted);
            return totalInserted;
        } catch (Exception e) {
            log.error("同步指派范围到指派明细失败", e);
            throw new CommonException("同步指派范围到指派明细失败: " + e.getMessage());
        }
    }

    /**
     * 处理单条指派记录
     *
     * @param record 指派记录
     * @return 处理的记录数
     */
    private int processAssignRecord(AssignRecords record) {
        // 解析范围ID列表
        List<Long> rangeIds = parseTargetIds(record.getRangeIds());
        if (CollectionUtils.isEmpty(rangeIds)) {
            log.warn("指派记录[{}]没有有效的范围ID，跳过处理", record.getId());
            return 0;
        }

        // 批量查询范围信息
        List<CommonRange> rangeList = commonRangeInterface.listCommonRangeByRangeIds(rangeIds);
        if (CollectionUtils.isEmpty(rangeList)) {
            log.warn("指派记录[{}]的范围ID不存在有效记录，跳过处理", record.getId());
            return 0;
        }

        // 根据指派类型获取对应的策略
        String assignType = record.getAssignType();
        AssignTypeStrategy strategy = assignTypeStrategyFactory.getStrategy(assignType);

        // 使用策略处理指派记录
        int inserted = strategy.processAssignRecord(record, rangeList, 
                AssignFinishedTimeTypeEnums.valueOf(record.getAssignFinishedTimeType()),
                record.getCustomFinishedDay(),
                record.getIfIsNotifyExistUser(),
                record.getNotifyUserAfterJoinDate());

        log.info("指派记录[{}]处理完成，插入{}条明细", record.getId(), inserted);
        return inserted;
    }

    /**
     * 获取未过期的指派记录
     * 包括两类:
     * 1. 单次指派且状态不是完成的
     * 2. 自动指派且截止时间未到的
     *
     * @return 未过期的指派记录列表
     */
    private List<AssignRecords> getNotExpiredAssignRecords() {
        LambdaQueryWrapper<AssignRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignRecords::getIsDel, 0);

        // 条件1：单次指派且状态不是完成的
        // 条件2：自动指派且截止时间未到或为空
        Date now = new Date();
        queryWrapper.and(wrapper ->
                // 单次指派且状态不是完成的
                wrapper.and(w -> w.eq(AssignRecords::getAssignType, AssignTypeEnums.ONCE.getCode())
                                .ne(AssignRecords::getStatus, AssignStatusEnums.SUCCESS.getCode())
                                .eq(AssignRecords::getIsDel, 0))
                        .or(
                                // 自动指派且截止时间未到或为空
                                w -> w.eq(AssignRecords::getAssignType, AssignTypeEnums.AUTO.getCode())
                                        .eq(AssignRecords::getIsDel, 0)
                                        .ge(AssignRecords::getDeadline, now)
                        )
        );

        return assignRecordsMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取CommonRange
     *
     * @param rangeId 范围ID
     * @return CommonRange对象
     */
    private CommonRange getCommonRangeById(Long rangeId) {
        return commonRangeMapper.selectById(rangeId);
    }

    /**
     * 获取部门下的用户ID列表
     *
     * @param departmentIds 部门ID列表
     * @return 用户ID列表
     */
    private List<Long> getDepartmentUsers(List<Long> departmentIds) {
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
    private List<Long> getRoleUsers(List<Long> roleIds) {
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
     * 获取业务可见范围内的用户ID列表
     *
     * @param bizId   业务ID
     * @param bizType 业务类型
     * @return 可见范围内的用户ID列表
     */
    private Set<Long> getVisibleUserIds(Long bizId, String bizType) {
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
     * 根据业务ID和类型获取已存在的用户ID列表
     *
     * @param bizId   业务ID
     * @param bizType 业务类型
     * @return 用户ID列表
     */
    private List<Long> getExistingUserIdsByBizId(Long bizId, String bizType) {
        // 查询与业务ID相关的CommonRange记录
        List<CommonRange> assignRanges = getAssignRangesByBusinessId(bizId);
        if (CollectionUtils.isEmpty(assignRanges)) {
            return Collections.emptyList();
        }

        // 获取所有指派ID
        List<Long> assignmentIds = assignRanges.stream()
                .map(CommonRange::getId)
                .collect(Collectors.toList());

        // 查询已存在的用户
        List<Long> existingUserIds = new ArrayList<>();
        for (Long assignmentId : assignmentIds) {
            existingUserIds.addAll(getExistingUserIdsByAssignmentId(assignmentId));
        }

        return existingUserIds;
    }

    /**
     * 根据业务ID获取指派范围记录
     *
     * @param businessId 业务ID
     * @return 指派范围记录列表
     */
    private List<CommonRange> getAssignRangesByBusinessId(Long businessId) {
        LambdaQueryWrapper<CommonRange> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommonRange::getModelType, "assignment")
                .eq(CommonRange::getTypeId, businessId)
                .eq(CommonRange::getIsDel, 0);

        return commonRangeMapper.selectList(queryWrapper);
    }

    /**
     * 批量插入指派明细
     *
     * @param assignmentId 指派ID
     * @param userIds      用户ID列表
     * @param creatorId    创建人ID
     * @param creatorName  创建人名称
     * @param status       状态码
     * @param reason       失败原因
     * @return 插入的记录数
     */
    private int batchInsertAssignmentDetail(Long assignmentId, List<Long> userIds, Long creatorId,
                                            String creatorName, String status, String reason) {
        if (CollectionUtils.isEmpty(userIds)) {
            return 0;
        }

        // 查询指派ID对应的CommonRange记录，获取业务类型和ID
        CommonRange range = commonRangeMapper.selectById(assignmentId);
        if (range == null) {
            log.error("指派ID[{}]对应的范围记录不存在", assignmentId);
            return 0;
        }

        // 构建bizId: {type}_{id}
        String bizId = range.getType() + "_" + range.getTypeId();
        
        // 从bizId中提取type和typeId
        String type = range.getType();
        Long typeId = range.getTypeId();

        // 查找关联的指派记录
        Long assignRecordId = findAssignRecordIdByRangeId(assignmentId);

        List<AssignmentDetail> detailList = new ArrayList<>();
        Date now = new Date();

        for (Long userId : userIds) {
            AssignmentDetail detail = new AssignmentDetail();
            detail.setAssignmentId(assignmentId);
            detail.setUserid(userId);
            detail.setStatus(status);
            detail.setBizId(bizId); // 设置bizId字段
            detail.setType(type); // 设置type字段
            detail.setTypeId(typeId); // 设置typeId字段
            // 设置指派记录ID
            if (assignRecordId != null) {
                detail.setAssignRecordId(assignRecordId);
            }
            detail.setGmtCreate(now);
            detail.setGmtModified(now);
            detail.setCreatorId(creatorId);
            detail.setCreatorName(creatorName);
            detail.setUpdaterId(creatorId);
            detail.setUpdaterName(creatorName);
            detail.setIsDel(0);

            // 设置失败原因
            if (StringUtils.hasText(reason)) {
                detail.setAttributes("{\"reason\":\"" + reason + "\"}");
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
     * 获取用户对应的第三方用户ID列表
     *
     * @param userIds 用户ID列表
     * @return 第三方用户ID列表
     */
    private List<String> getThirdPartyUserIds(List<Long> userIds) {
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
     * 获取生效的指派类型的范围记录
     *
     * @return 指派范围记录列表
     */
    private List<CommonRange> getEffectiveAssignmentRanges() {
        LambdaQueryWrapper<CommonRange> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommonRange::getModelType, "assignment")
                .eq(CommonRange::getIsDel, 0);

        // 添加时间条件：开始时间为空或已开始，结束时间为空或未结束
        Date now = new Date();
        queryWrapper.and(wrapper -> wrapper.isNull(CommonRange::getStartTime)
                .or(w -> w.le(CommonRange::getStartTime, now)));
        queryWrapper.and(wrapper -> wrapper.isNull(CommonRange::getEndTime)
                .or(w -> w.ge(CommonRange::getEndTime, now)));

        return commonRangeMapper.selectList(queryWrapper);
    }

    /**
     * 根据指派ID获取已存在的用户ID列表
     *
     * @param assignmentId 指派ID
     * @return 用户ID列表
     */
    @Override
    public List<Long> getExistingUserIdsByAssignmentId(Long assignmentId) {
        LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignmentDetail::getAssignmentId, assignmentId)
                .eq(AssignmentDetail::getIsDel, 0)
                .select(AssignmentDetail::getUserid);

        List<AssignmentDetail> details = assignmentDetailMapper.selectList(queryWrapper);
        return details.stream()
                .map(AssignmentDetail::getUserid)
                .collect(Collectors.toList());
    }

    /**
     * 批量插入指派明细
     *
     * @param assignmentId 指派ID
     * @param userIds      用户ID列表
     * @param creatorId    创建人ID
     * @param creatorName  创建人名称
     * @return 插入的记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertAssignmentDetail(Long assignmentId, List<Long> userIds, Long creatorId, String creatorName) {
        if (CollectionUtils.isEmpty(userIds)) {
            return 0;
        }

        // 查询指派ID对应的CommonRange记录，获取业务类型和ID
        CommonRange range = commonRangeMapper.selectById(assignmentId);
        if (range == null) {
            log.error("指派ID[{}]对应的范围记录不存在", assignmentId);
            return 0;
        }

        // 构建bizId: {type}_{id}
        String bizId = range.getType() + "_" + range.getTypeId();
        
        // 从bizId中提取type和typeId
        String type = range.getType();
        Long typeId = range.getTypeId();

        // 查找关联的指派记录
        Long assignRecordId = findAssignRecordIdByRangeId(assignmentId);

        List<AssignmentDetail> detailList = new ArrayList<>();
        Date now = new Date();

        for (Long userId : userIds) {
            AssignmentDetail detail = new AssignmentDetail();
            detail.setAssignmentId(assignmentId);
            detail.setUserid(userId);
            detail.setStatus("active");
            detail.setBizId(bizId); // 设置bizId字段
            detail.setType(type); // 设置type字段
            detail.setTypeId(typeId); // 设置typeId字段
            // 设置指派记录ID
            if (assignRecordId != null) {
                detail.setAssignRecordId(assignRecordId);
            }
            detail.setGmtCreate(now);
            detail.setGmtModified(now);
            detail.setCreatorId(creatorId);
            detail.setCreatorName(creatorName);
            detail.setUpdaterId(creatorId);
            detail.setUpdaterName(creatorName);
            detail.setIsDel(0);

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
     * 根据范围ID查找关联的指派记录ID
     *
     * @param rangeId 范围ID
     * @return 指派记录ID，如果不存在则返回null
     */
    private Long findAssignRecordIdByRangeId(Long rangeId) {
        if (rangeId == null) {
            return null;
        }

        try {
            LambdaQueryWrapper<AssignRecords> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AssignRecords::getIsDel, 0)
                    .like(AssignRecords::getRangeIds, rangeId.toString())
                    .orderByDesc(AssignRecords::getGmtCreate)
                    .last("LIMIT 1");

            AssignRecords record = assignRecordsMapper.selectOne(queryWrapper);
            return record != null ? record.getId() : null;
        } catch (Exception e) {
            log.warn("查找范围ID[{}]关联的指派记录失败: {}", rangeId, e.getMessage());
            return null;
        }
    }

    /**
     * 根据部门ID列表获取用户ID列表
     *
     * @param departmentIds 部门ID列表
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByDepartmentIds(List<Long> departmentIds) {
        if (CollectionUtils.isEmpty(departmentIds)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DepartmentUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DepartmentUser::getDepartmentId, departmentIds)
                .eq(DepartmentUser::getIsDel, 0)
                .select(DepartmentUser::getUserId);

        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(queryWrapper);
        return departmentUsers.stream()
                .map(DepartmentUser::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色ID列表获取用户ID列表
     *
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<OrgRoleUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(OrgRoleUser::getOrgRoleId, roleIds)
                .eq(OrgRoleUser::getIsDel, 0)
                .select(OrgRoleUser::getUserId);

        List<OrgRoleUser> roleUsers = orgRoleUserMapper.selectList(queryWrapper);
        return roleUsers.stream()
                .map(OrgRoleUser::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 查询指派记录
     * 根据业务ID查询指派记录列表
     *
     * @param businessId 业务ID
     * @param request    查询请求参数
     * @return 指派记录列表及分页信息
     */
    @Override
    public List<AssignRecordResponse> queryAssignRecords(Long businessId, AssignRecordRequest request) {
        log.info("查询业务ID[{}]的指派记录，参数：{}", businessId, request);

        if (businessId == null) {
            log.error("业务ID不能为空");
            return Collections.emptyList();
        }

        try {
            // 查询与业务ID相关的CommonRange记录
            List<CommonRange> assignRanges = getAssignRangesByBusinessId(businessId);
            if (CollectionUtils.isEmpty(assignRanges)) {
                log.info("未找到业务ID[{}]的指派范围记录", businessId);
                return Collections.emptyList();
            }

            // 获取所有指派ID
            List<Long> assignmentIds = assignRanges.stream()
                    .map(CommonRange::getId)
                    .collect(Collectors.toList());

            // 构建查询条件
            LambdaQueryWrapper<AssignmentDetail> queryWrapper = buildQueryWrapper(assignmentIds, request);

            // 分页查询
            Page<AssignmentDetail> page = new Page<>(request.getPageNum(), request.getPageSize());
            Page<AssignmentDetail> resultPage = assignmentDetailMapper.selectPage(page, queryWrapper);

            // 转换为响应对象
            List<AssignRecordResponse> responseList = convertToResponseList(resultPage.getRecords(), assignRanges);

            log.info("查询业务ID[{}]的指派记录成功，共{}条记录", businessId, responseList.size());
            return responseList;
        } catch (Exception e) {
            log.error("查询业务ID[{}]的指派记录失败", businessId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取指派记录总数
     *
     * @param businessId 业务ID
     * @param request    查询请求参数
     * @return 记录总数
     */
    @Override
    public Long countAssignRecords(Long businessId, AssignRecordRequest request) {
        log.info("统计业务ID[{}]的指派记录数量，参数：{}", businessId, request);

        if (businessId == null) {
            log.error("业务ID不能为空");
            return 0L;
        }

        try {
            // 查询与业务ID相关的CommonRange记录
            List<CommonRange> assignRanges = getAssignRangesByBusinessId(businessId);
            if (CollectionUtils.isEmpty(assignRanges)) {
                log.info("未找到业务ID[{}]的指派范围记录", businessId);
                return 0L;
            }

            // 获取所有指派ID
            List<Long> assignmentIds = assignRanges.stream()
                    .map(CommonRange::getId)
                    .collect(Collectors.toList());

            // 构建查询条件
            LambdaQueryWrapper<AssignmentDetail> queryWrapper = buildQueryWrapper(assignmentIds, request);

            // 统计记录数
            long count = assignmentDetailMapper.selectCount(queryWrapper);

            log.info("统计业务ID[{}]的指派记录数量成功，共{}条记录", businessId, count);
            return count;
        } catch (Exception e) {
            log.error("统计业务ID[{}]的指派记录数量失败", businessId, e);
            return 0L;
        }
    }

    /**
     * 构建查询条件
     *
     * @param assignmentIds 指派ID列表
     * @param request       查询请求参数
     * @return 查询条件
     */
    private LambdaQueryWrapper<AssignmentDetail> buildQueryWrapper(List<Long> assignmentIds, AssignRecordRequest request) {
        LambdaQueryWrapper<AssignmentDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(AssignmentDetail::getAssignmentId, assignmentIds)
                .eq(AssignmentDetail::getIsDel, 0);

        // 添加状态筛选
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(AssignmentDetail::getStatus, request.getStatus());
        }

        // 添加时间范围筛选
        if (StringUtils.hasText(request.getStartTime())) {
            queryWrapper.ge(AssignmentDetail::getGmtCreate, request.getStartTime());
        }
        if (StringUtils.hasText(request.getEndTime())) {
            queryWrapper.le(AssignmentDetail::getGmtCreate, request.getEndTime());
        }

        // 添加关键字搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(AssignmentDetail::getCreatorName, request.getKeyword())
                    .or()
                    .like(AssignmentDetail::getAttributes, request.getKeyword()));
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(AssignmentDetail::getGmtCreate);

        return queryWrapper;
    }

    /**
     * 将实体列表转换为响应对象列表
     *
     * @param detailList   指派明细列表
     * @param assignRanges 指派范围列表
     * @return 响应对象列表
     */
    private List<AssignRecordResponse> convertToResponseList(List<AssignmentDetail> detailList, List<CommonRange> assignRanges) {
        if (CollectionUtils.isEmpty(detailList)) {
            return Collections.emptyList();
        }

        // 按指派ID分组
        Map<Long, List<AssignmentDetail>> detailMap = detailList.stream()
                .collect(Collectors.groupingBy(AssignmentDetail::getAssignmentId));

        // 按指派ID分组
        Map<Long, CommonRange> rangeMap = assignRanges.stream()
                .collect(Collectors.toMap(CommonRange::getId, range -> range, (r1, r2) -> r1));

        // 获取所有用户ID
        Set<Long> userIds = detailList.stream()
                .map(AssignmentDetail::getUserid)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getId, userIds)
                    .eq(User::getIsDel, 0);
            List<User> users = userMapper.selectList(userQueryWrapper);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));
        }

        // 构建响应对象
        List<AssignRecordResponse> responseList = new ArrayList<>();
        for (Map.Entry<Long, List<AssignmentDetail>> entry : detailMap.entrySet()) {
            Long assignmentId = entry.getKey();
            List<AssignmentDetail> details = entry.getValue();

            CommonRange range = rangeMap.get(assignmentId);
            if (range == null) {
                continue;
            }

            AssignRecordResponse response = new AssignRecordResponse();
            response.setId(assignmentId);
            response.setOperatorId(range.getCreatorId());
            response.setOperatorName(range.getCreatorName());
            response.setOperationTime(range.getGmtCreate());
            response.setDeadline(range.getEndTime());

            // 设置操作范围摘要
            String operationScope = buildOperationScopeSummary(range);
            response.setOperationScope(operationScope);

            // 设置提醒状态
            response.setReminderStatus("已提醒"); // 默认值，可根据实际情况修改

            // 统计成功和失败数量
            long successCount = details.stream()
                    .filter(detail -> "active".equals(detail.getStatus()))
                    .count();
            long failedCount = details.size() - successCount;

            response.setSuccessCount((int) successCount);
            response.setFailedCount((int) failedCount);

            // 设置状态
            response.setStatus(successCount > 0 ? "指派成功" : "指派失败");

            // 设置成功和失败用户列表
            Map<Long, User> finalUserMap = userMap;
            List<AssignRecordResponse.UserInfo> successUsers = details.stream()
                    .filter(detail -> "active".equals(detail.getStatus()))
                    .map(detail -> {
                        AssignRecordResponse.UserInfo userInfo = new AssignRecordResponse.UserInfo();
                        userInfo.setUserId(detail.getUserid());

                        User user = finalUserMap.get(detail.getUserid());
                        if (user != null) {
                            userInfo.setUserName(user.getNickname());
                            // 部门信息需要另外查询，这里简化处理
                            userInfo.setDepartmentName("");
                        }
                        return userInfo;
                    })
                    .collect(Collectors.toList());

            List<AssignRecordResponse.UserInfo> failedUsers = details.stream()
                    .filter(detail -> !"active".equals(detail.getStatus()))
                    .map(detail -> {
                        AssignRecordResponse.UserInfo userInfo = new AssignRecordResponse.UserInfo();
                        userInfo.setUserId(detail.getUserid());

                        User user = finalUserMap.get(detail.getUserid());
                        if (user != null) {
                            userInfo.setUserName(user.getNickname());
                            // 部门信息需要另外查询，这里简化处理
                            userInfo.setDepartmentName("");
                        }

                        return userInfo;
                    })
                    .collect(Collectors.toList());

            response.setSuccessUsers(successUsers);
            response.setFailedUsers(failedUsers);

            responseList.add(response);
        }

        return responseList;
    }

    /**
     * 保存指派记录
     *
     * @param request       请求参数
     * @param rangeIds      范围ID列表
     * @param userId        创建人ID
     * @param nickname      创建人名称
     * @param assignEndTime
     * @return 保存的指派记录
     */
    @Override
    public AssignRecords saveAssignRecord(AssignRequest request, List<Long> rangeIds, Long userId, String nickname, Date assignEndTime) {
        String bizType = request.getBizType();
        Long bizId = request.getBizId();
        log.info("保存指派记录，业务类型：{}，业务ID：{}，范围IDs：{}", bizType, bizId, rangeIds);

        if (bizId == null
                || StringUtils.isEmpty(bizType)
                || CollectionUtils.isEmpty(rangeIds)
                || StringUtils.isEmpty(request.getAssignType())) {
            throw new CommonException("保存指派记录失败：业务ID、业务类型或范围ID不能为空");
        }

        // 创建指派记录
        AssignRecords assignRecord = new AssignRecords();
        assignRecord.setType(bizType);
        assignRecord.setTypeId(bizId);
        assignRecord.setRangeIds(rangeIds.toString().replace(" ", ""));

        // 设置默认的指派类型和状态
        assignRecord.setAssignType(AssignTypeEnums.ONCE.getCode()); // 默认为单次通知
        assignRecord.setStatus(AssignStatusEnums.WAIT.getCode()); // 默认为待通知状态
        assignRecord.setAssignType(request.getAssignType());
        assignRecord.setDeadline(assignEndTime);
        assignRecord.setAssignFinishedTimeType(request.getAssignFinishedTimeType());
        assignRecord.setCustomFinishedDay(request.getCustomFinishedDay());
        assignRecord.setIfIsNotifyExistUser(request.getIfIsNotifyExistUser());
        assignRecord.setNotifyUserAfterJoinDate(request.getNotifyUserAfterJoinDate());
        // 设置创建者信息
        assignRecord.setCreatorId(userId);
        assignRecord.setCreatorName(nickname);
        assignRecord.setUpdaterId(userId);
        assignRecord.setUpdaterName(nickname);
        assignRecord.setIsDel(Boolean.FALSE);

        // 保存指派记录
        assignRecordsMapper.insert(assignRecord);
        log.info("保存指派记录成功，ID：{}", assignRecord.getId());
        return assignRecord;
    }


    /**
     * 构建操作范围摘要
     *
     * @param range 指派范围
     * @return 操作范围摘要
     */
    private String buildOperationScopeSummary(CommonRange range) {
        StringBuilder summary = new StringBuilder();

        String targetType = range.getTargetType();
        String targetIdsStr = range.getTargetIds();
        List<Long> targetIds = parseTargetIds(targetIdsStr);

        int count = targetIds.size();

        if ("user".equals(targetType)) {
            summary.append("指派人员").append(count).append("个");
        } else if ("department".equals(targetType)) {
            summary.append("指派部门").append(count).append("个");
        } else if ("role".equals(targetType)) {
            summary.append("指派角色").append(count).append("个");
        } else {
            summary.append("指派").append(targetType).append(count).append("个");
        }

        return summary.toString();
    }

    /**
     * 解析目标ID字符串
     *
     * @param targetIdsStr 目标ID字符串，格式为 [1,2,3,4]
     * @return 目标ID列表
     */
    private List<Long> parseTargetIds(String targetIdsStr) {
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
     * 获取指派回显信息
     * 根据业务类型、业务ID和指派类型获取指派回显信息
     *
     * @param request 指派回显请求
     * @return 指派回显信息
     */
    @Override
    public AssignmentDetailResponse getAssignmentDetail(AssignmentDetailRequest request) {
        log.info("获取指派回显信息，请求参数: {}", request);

        // 参数校验
        if (request == null || StringUtils.isEmpty(request.getBizType())
                || request.getBizId() == null || StringUtils.isEmpty(request.getAssignType())) {
            throw new CommonException("参数不能为空");
        }

        AssignmentDetailResponse response = new AssignmentDetailResponse();
        String bizId = request.getBizType() + "_" + request.getBizId();

        // 按照单次指派和自动指派分别处理
        if (AssignTypeEnums.ONCE.getCode().equals(request.getAssignType())) {
            // 单次通知：直接从assignment_detail获取type_{type_id}下所有通知成功的用户
            return processOnceAssignmentDetail(bizId);
        } else if (AssignTypeEnums.AUTO.getCode().equals(request.getAssignType())) {
            // 自动通知：从assign_records下type_{type_id}中获取一条有效记录，展示门店、角色、人员信息
            return processAutoAssignmentDetail(request.getBizType(), request.getBizId());
        }

        return response;
    }

    /**
     * 处理单次指派回显
     * 直接从assignment_detail获取type_{type_id}下所有通知成功的用户
     *
     * @param bizId 业务ID (格式: type_id)
     * @return 指派回显信息
     */
    private AssignmentDetailResponse processOnceAssignmentDetail(String bizId) {
        AssignmentDetailResponse response = new AssignmentDetailResponse();

        // 查询通知成功的人员
        LambdaQueryWrapper<AssignmentDetail> detailQueryWrapper = new LambdaQueryWrapper<>();
        detailQueryWrapper.eq(AssignmentDetail::getBizId, bizId)
                .eq(AssignmentDetail::getIsDel, 0)
                .eq(AssignmentDetail::getStatus, AssignStatusEnums.NOTIFY_SUCCESS.getCode());

        List<AssignmentDetail> detailList = assignmentDetailMapper.selectList(detailQueryWrapper);

        if (CollectionUtils.isEmpty(detailList)) {
            log.info("未找到业务ID[{}]的通知成功记录", bizId);
            return response;
        }

        // 获取用户ID列表

        Map<Long, AssignmentDetail> userIdAndAssignDetailMap = detailList.stream().collect(Collectors.toMap(
                AssignmentDetail::getUserid, Function.identity(), (v1, v2) -> v1));
        Set<Long> userIds = userIdAndAssignDetailMap.keySet();
        // 查询用户信息
        if (CollectionUtils.isEmpty(userIds)) {
            return response;
        }
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getUserId, userIds).eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userQueryWrapper);

        List<AssignmentDetailResponse.AssignUserInfo> assignUserInfos = users.stream().map(user -> {
            AssignmentDetail assignmentDetail = userIdAndAssignDetailMap.get(user.getUserId());
            AssignmentDetailResponse.AssignUserInfo userInfo = new AssignmentDetailResponse.AssignUserInfo();
            userInfo.setUserId(user.getId());
            userInfo.setUserName(user.getNickname());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setAssignRecordId(assignmentDetail.getId());
            return userInfo;
        }).collect(Collectors.toList());
        response.setAssignUsers(assignUserInfos);
        return response;
    }

    /**
     * 处理自动指派回显
     * 从assign_records下type_{type_id}中获取一条有效记录，展示门店、角色、人员信息
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 指派回显信息
     */
    private AssignmentDetailResponse processAutoAssignmentDetail(String bizType, Long bizId) {
        AssignmentDetailResponse response = new AssignmentDetailResponse();

        // 获取最新的有效指派记录
        LambdaQueryWrapper<AssignRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssignRecords::getType, bizType)
                .eq(AssignRecords::getTypeId, bizId)
                .eq(AssignRecords::getAssignType, AssignTypeEnums.AUTO.getCode())
                .eq(AssignRecords::getIsDel, 0)
                .orderByDesc(AssignRecords::getGmtCreate)
                .last("LIMIT 1");

        AssignRecords assignRecord = assignRecordsMapper.selectOne(queryWrapper);

        if (assignRecord == null) {
            log.info("未找到业务类型[{}]业务ID[{}]的自动指派记录", bizType, bizId);
            return response;
        }

        // 获取范围ID列表
        List<Long> rangeIds = parseTargetIds(assignRecord.getRangeIds());
        if (CollectionUtils.isEmpty(rangeIds)) {
            log.info("指派记录[{}]没有有效的范围ID", assignRecord.getId());
            return response;
        }

        List<CommonRange> rangeList = commonRangeInterface.listCommonRangeByRangeIds(rangeIds);
        if (CollectionUtils.isEmpty(rangeList)) {
            log.info("指派记录[{}]的范围ID不存在有效记录", assignRecord.getId());
            return response;
        }
        response.setAutoAssignRecordId(assignRecord.getId());
        
        // 添加对指派记录中的其他字段进行回显设置
        response.setAssignFinishedTimeType(assignRecord.getAssignFinishedTimeType());
        response.setCustomFinishedDay(assignRecord.getCustomFinishedDay());
        response.setIfIsNotifyExistUser(assignRecord.getIfIsNotifyExistUser());
        response.setNotifyUserAfterJoinDate(assignRecord.getNotifyUserAfterJoinDate());

        // 根据范围类型分类
        Map<String, List<CommonRange>> rangeTypeMap = rangeList.stream()
                .collect(Collectors.groupingBy(CommonRange::getTargetType));

        // 处理部门范围
        List<CommonRange> departmentRanges = rangeTypeMap.getOrDefault(RangeTargetTypeEnums.DEPARTMENT.getCode(), Collections.emptyList());
        if (!CollectionUtils.isEmpty(departmentRanges)) {
            List<Long> departmentIds = new ArrayList<>();
            for (CommonRange range : departmentRanges) {
                departmentIds.addAll(
                        ConverterUtils.parseTargetIds(range.getTargetIds()));
            }

            if (!CollectionUtils.isEmpty(departmentIds)) {
                // 获取部门信息
                OrgRequest orgRequest = new OrgRequest();
                orgRequest.setOrgIds(departmentIds);
                BaseResponse<OrgDTO> orgResponse = orgService.queryOrgs(orgRequest);

                if (orgResponse != null && !CollectionUtils.isEmpty(orgResponse.getData())) {
                    List<AssignmentDetailResponse.AssignDepartmentInfo> departmentInfos = orgResponse.getData().stream().map(org -> {
                        AssignmentDetailResponse.AssignDepartmentInfo departmentInfo = new AssignmentDetailResponse.AssignDepartmentInfo();
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
                roleIds.addAll(
                        ConverterUtils.parseTargetIds(range.getTargetIds()));
            }

            if (!CollectionUtils.isEmpty(roleIds)) {
                // 获取角色信息
                RoleRequest roleRequest = new RoleRequest();
                roleRequest.setIds(roleIds);
                BaseResponse<RoleDTO> roleResponse = roleService.queryRoles(roleRequest);

                if (roleResponse != null && !CollectionUtils.isEmpty(roleResponse.getData())) {
                    List<AssignmentDetailResponse.AssignRoleInfo> roleInfos = roleResponse.getData().stream().map(role -> {
                        AssignmentDetailResponse.AssignRoleInfo roleInfo = new AssignmentDetailResponse.AssignRoleInfo();
                        roleInfo.setRoleId(role.getId());
                        roleInfo.setRoleName(role.getName());
                        return roleInfo;
                    }).collect(Collectors.toList());

                    response.setRoles(roleInfos);
                }
            }
        }

        // 处理人员范围
        List<CommonRange> userRanges = rangeTypeMap.getOrDefault(RangeTargetTypeEnums.USER.getCode(), Collections.emptyList());
        if (!CollectionUtils.isEmpty(userRanges)) {
            List<Long> userIds = new ArrayList<>();
            for (CommonRange range : userRanges) {
                userIds.addAll(
                        ConverterUtils.parseTargetIds(range.getTargetIds()));
            }

            if (!CollectionUtils.isEmpty(userIds)) {
                // 查询用户信息
                LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
                userQueryWrapper.in(User::getUserId, userIds).eq(User::getIsDel, 0);
                List<User> users = userMapper.selectList(userQueryWrapper);

                if (!CollectionUtils.isEmpty(users)) {
                    List<AssignmentDetailResponse.AssignUserInfo> assignUserInfos = users.stream().map(user -> {
                        AssignmentDetailResponse.AssignUserInfo userInfo = new AssignmentDetailResponse.AssignUserInfo();
                        userInfo.setUserId(user.getId());
                        userInfo.setUserName(user.getNickname());
                        userInfo.setAvatar(user.getAvatar());
                        return userInfo;
                    }).collect(Collectors.toList());

                    response.setAssignUsers(assignUserInfos);
                }
            }
        }
        return response;
    }

    /**
     * 根据ID获取指派记录
     *
     * @param assignRecordId 指派记录ID
     * @return 指派记录
     */
    @Override
    public AssignRecords getAssignRecordById(Long assignRecordId) {
        if (assignRecordId == null) {
            return null;
        }
        return assignRecordsMapper.selectById(assignRecordId);
    }

    /**
     * 更新指派记录
     *
     * @param assignRecord 指派记录
     * @return 更新结果
     */
    @Override
    public int updateAssignRecord(AssignRecords assignRecord) {
        if (assignRecord == null || assignRecord.getId() == null) {
            throw new CommonException("更新指派记录失败：记录ID不能为空");
        }
        return assignRecordsMapper.updateById(assignRecord);
    }
}
