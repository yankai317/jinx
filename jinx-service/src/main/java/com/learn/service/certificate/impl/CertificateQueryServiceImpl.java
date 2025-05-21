package com.learn.service.certificate.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.learn.common.dto.ContentBO;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.dto.certificate.CertificateDTO;
import com.learn.dto.certificate.CertificateDetailDTO;
import com.learn.dto.certificate.CertificateListRequest;
import com.learn.dto.certificate.CertificateListResponse;
import com.learn.dto.certificate.CertificateUsersRequest;
import com.learn.dto.certificate.CertificateUsersResponse;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.certificate.CertificateQueryService;
import com.learn.service.toc.ContentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 证书查询服务实现类
 */
@Service
@Slf4j
public class CertificateQueryServiceImpl implements CertificateQueryService {

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;@Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private UserMapper userMapper;@Autowired
    private DepartmentUserMapper departmentUserMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ContentService contentService;

    @Override
    public CertificateListResponse getCertificateList(CertificateListRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<Certificate> queryWrapper = new LambdaQueryWrapper<>();
        
        // 设置未删除条件
        queryWrapper.eq(Certificate::getIsDel, 0);
        
        // 根据证书名称模糊查询
        if (StringUtils.isNotBlank(request.getName())) {
            queryWrapper.like(Certificate::getName, request.getName());
        }
        
        // 根据创建人ID查询
        if (request.getCreatorId() != null) {
            queryWrapper.eq(Certificate::getCreatorId, request.getCreatorId());
        }
        
        // 根据创建时间范围查询
        if (StringUtils.isNotBlank(request.getStartTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startDate = sdf.parse(request.getStartTime());
                queryWrapper.ge(Certificate::getGmtCreate, startDate);
            } catch (Exception e) {
                log.error("解析开始时间异常", e);
            }
        }
        
        if (StringUtils.isNotBlank(request.getEndTime())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date endDate = sdf.parse(request.getEndTime());
                queryWrapper.le(Certificate::getGmtCreate, endDate);
            } catch (Exception e) {
                log.error("解析结束时间异常", e);
            }
        }
        
        // 只看我创建的
        if (request.getOnlyMine() != null && request.getOnlyMine()) {
            // 这里需要从上下文中获取当前用户ID，暂时使用请求中的creatorId
            if (request.getCreatorId() != null) {
                queryWrapper.eq(Certificate::getCreatorId, request.getCreatorId());
            }
        }
        
        // 设置排序
        if (StringUtils.isNotBlank(request.getSortField())) {
            String sortField = request.getSortField();
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            
            // 根据不同的排序字段设置排序条件
            if ("gmt_create".equals(sortField)) {
                if (isAsc) {
                    queryWrapper.orderByAsc(Certificate::getGmtCreate);
                } else {
                    queryWrapper.orderByDesc(Certificate::getGmtCreate);
                }
            } else if ("name".equals(sortField)) {
                if (isAsc) {
                    queryWrapper.orderByAsc(Certificate::getName);
                } else {
                    queryWrapper.orderByDesc(Certificate::getName);
                }
            } else {
                // 默认按创建时间降序
                queryWrapper.orderByDesc(Certificate::getGmtCreate);
            }
        } else {
            // 默认按创建时间降序
            queryWrapper.orderByDesc(Certificate::getGmtCreate);
        }
        
        // 分页查询
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        Page<Certificate> page = new Page<>(pageNum, pageSize);
        IPage<Certificate> certificatePage = certificateMapper.selectPage(page, queryWrapper);
        
        // 查询每个证书的发放数量
        List<Certificate> certificateList = certificatePage.getRecords();
        List<Long> certificateIds = certificateList.stream()
                .map(Certificate::getId)
                .collect(Collectors.toList());// 统计每个证书的发放数量
        Map<Long, Long> issuedCountMap = getIssuedCountMap(certificateIds);
        
        // 构建返回结果
        CertificateListResponse response = new CertificateListResponse();
        response.setTotal((int) certificatePage.getTotal());
        response.setList(convertToCertificateDTOList(certificateList, issuedCountMap));
        
        return response;
    }@Override
    public CertificateDetailDTO getCertificateDetail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("证书ID不能为空");
        }
        
        // 查询证书基本信息
        Certificate certificate = certificateMapper.selectById(id);
        if (certificate == null || certificate.getIsDel() == 1) {
            throw new IllegalArgumentException("证书不存在或已删除");
        }
        
        // 查询证书发放数量
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCertificate::getCertificateId, id);
        queryWrapper.eq(UserCertificate::getIsDel, 0);
        Long issuedCount = userCertificateMapper.selectCount(queryWrapper);
        
        // 查询证书使用情况 - 培训
        List<CertificateDetailDTO.CertificateUsageDTO> usages = new ArrayList<>();
        LambdaQueryWrapper<Train> trainQueryWrapper = new LambdaQueryWrapper<>();
        trainQueryWrapper.eq(Train::getCertificateId, id);
        trainQueryWrapper.eq(Train::getIsDel, 0);
        List<Train> trainList = trainMapper.selectList(trainQueryWrapper);
        for (Train train : trainList) {
            CertificateDetailDTO.CertificateUsageDTO usage = new CertificateDetailDTO.CertificateUsageDTO();
            usage.setType(BizType.TRAIN);
            usage.setId(train.getId());
            usage.setName(train.getName());
            usages.add(usage);
        }
        
        // 查询证书使用情况 - 学习地图
        LambdaQueryWrapper<LearningMap> mapQueryWrapper = new LambdaQueryWrapper<>();
        mapQueryWrapper.eq(LearningMap::getCertificateId, id);
        mapQueryWrapper.eq(LearningMap::getIsDel, 0);
        List<LearningMap> mapList = learningMapMapper.selectList(mapQueryWrapper);
        for (LearningMap map : mapList) {
            CertificateDetailDTO.CertificateUsageDTO usage = new CertificateDetailDTO.CertificateUsageDTO();
            usage.setType(BizType.LEARNING_MAP);
            usage.setId(map.getId());
            usage.setName(map.getName());
            usages.add(usage);
        }// 查询证书使用情况 - 学习地图阶段
        LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
        stageQueryWrapper.eq(LearningMapStage::getCertificateId, id);
        stageQueryWrapper.eq(LearningMapStage::getIsDel, 0);
        List<LearningMapStage> stageList = learningMapStageMapper.selectList(stageQueryWrapper);
        for (LearningMapStage stage : stageList) {
            // 查询学习地图信息
            LearningMap map = learningMapMapper.selectById(stage.getMapId());
            if (map != null && map.getIsDel() == 0) {
                CertificateDetailDTO.CertificateUsageDTO usage = new CertificateDetailDTO.CertificateUsageDTO();
                usage.setType(BizType.LEARNING_MAP);
                usage.setId(map.getId());
                usage.setName(map.getName());
                usages.add(usage);
            }
        }
        
        // 组装返回结果
        CertificateDetailDTO detailDTO = new CertificateDetailDTO();
        detailDTO.setId(certificate.getId());
        detailDTO.setName(certificate.getName());
        detailDTO.setDescription(certificate.getDescription());
        detailDTO.setTemplateUrl(certificate.getTemplateUrl());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 格式化过期时间
        if (certificate.getExpireTime() != null) {
            detailDTO.setExpireTime(sdf.format(certificate.getExpireTime()));
        }
        
        detailDTO.setCreatorId(certificate.getCreatorId());
        detailDTO.setCreatorName(certificate.getCreatorName());
        // 格式化创建时间
        if (certificate.getGmtCreate() != null) {
            detailDTO.setGmtCreate(sdf.format(certificate.getGmtCreate()));
        }
        
        detailDTO.setIssuedCount(issuedCount);
        detailDTO.setUsages(usages);
        
        return detailDTO;
    }
    
    @Override
    public CertificateUsersResponse getCertificateUsers(Long id, CertificateUsersRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("证书ID不能为空");
        }
        
        // 查询证书是否存在
        Certificate certificate = certificateMapper.selectById(id);
        if (certificate == null || certificate.getIsDel() == 1) {
            throw new IllegalArgumentException("证书不存在或已删除");
        }// 构建查询条件
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCertificate::getCertificateId, id);
        queryWrapper.eq(UserCertificate::getIsDel, 0);
        
        // 根据状态筛选
        if (StringUtils.isNotBlank(request.getStatus()) && !"all".equals(request.getStatus())) {
            if ("valid".equals(request.getStatus())) {
                queryWrapper.eq(UserCertificate::getStatus, 0);
            } else if ("expired".equals(request.getStatus())) {
                queryWrapper.eq(UserCertificate::getStatus, 1);
            } else if ("revoked".equals(request.getStatus())) {
                queryWrapper.eq(UserCertificate::getStatus, 2);
            }
        }
        
        // 根据来源类型筛选
        if (StringUtils.isNotBlank(request.getSourceType())) {
            List<String> sourceTypes = BizType.LEARNING_MAP.equals(request.getSourceType()) ? Lists.newArrayList(BizType.LEARNING_MAP, BizType.MAP_STAGE) : Lists.newArrayList(request.getSourceType());
            queryWrapper.in(UserCertificate::getSourceType, sourceTypes);
        }
        
        // 分页查询
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        Page<UserCertificate> page = new Page<>(pageNum, pageSize);
        
        // 如果有部门ID筛选，需要先查询该部门下的用户ID
        List<Long> departmentUserIds = null;
        if (request.getDepartmentId() != null) {
            LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
            duQueryWrapper.eq(DepartmentUser::getDepartmentId, request.getDepartmentId());
            duQueryWrapper.eq(DepartmentUser::getIsDel, 0);
            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duQueryWrapper);
            departmentUserIds = departmentUsers.stream()
                    .map(DepartmentUser::getUserId)
                    .collect(Collectors.toList());
            
            // 如果部门下没有用户，直接返回空结果
            if (departmentUserIds.isEmpty()) {
                CertificateUsersResponse response = new CertificateUsersResponse();
                response.setTotal(0);
                response.setList(new ArrayList<>());
                return response;
            }
            
            queryWrapper.in(UserCertificate::getUserId, departmentUserIds);
        }
        
        // 如果有关键词搜索，需要先查询匹配的用户ID
        if (StringUtils.isNotBlank(request.getKeyword())) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getIsDel, 0);
            userQueryWrapper.and(wrapper -> wrapper
                    .like(User::getNickname, request.getKeyword())
                    .or()
                    .like(User::getEmployeeNo, request.getKeyword())
            );
            
            List<User> users = userMapper.selectList(userQueryWrapper);
            List<Long> userIds = users.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());
            
            // 如果没有匹配的用户，直接返回空结果
            if (userIds.isEmpty()) {
                CertificateUsersResponse response = new CertificateUsersResponse();
                response.setTotal(0);
                response.setList(new ArrayList<>());
                return response;
            }// 如果已经有部门筛选，需要取交集
            if (departmentUserIds != null && !departmentUserIds.isEmpty()) {
                userIds.retainAll(departmentUserIds);
                if (userIds.isEmpty()) {
                    CertificateUsersResponse response = new CertificateUsersResponse();
                    response.setTotal(0);
                    response.setList(new ArrayList<>());
                    return response;
                }
            }
            
            queryWrapper.in(UserCertificate::getUserId, userIds);
        }
        
        // 执行分页查询
        IPage<UserCertificate> userCertificatePage = userCertificateMapper.selectPage(page, queryWrapper);
        List<UserCertificate> userCertificates = userCertificatePage.getRecords();
        
        // 如果没有记录，直接返回空结果
        if (userCertificates.isEmpty()) {
            CertificateUsersResponse response = new CertificateUsersResponse();
            response.setTotal(0);
            response.setList(new ArrayList<>());
            return response;
        }
        
        // 获取用户ID列表
        List<Long> userIds = userCertificates.stream()
                .map(UserCertificate::getUserId)
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getUserId, userIds);
        userQueryWrapper.eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        
        // 批量查询用户部门信息
        LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
        duQueryWrapper.in(DepartmentUser::getUserId, userIds);
        duQueryWrapper.eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duQueryWrapper);
        
        // 获取部门ID列表
        List<Long> departmentIds = departmentUsers.stream()
                .map(DepartmentUser::getDepartmentId)
                .collect(Collectors.toList());
        
        // 批量查询部门信息
        Map<Long, String> departmentNameMap = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            LambdaQueryWrapper<Department> departmentQueryWrapper = new LambdaQueryWrapper<>();
            departmentQueryWrapper.in(Department::getId, departmentIds);
            departmentQueryWrapper.eq(Department::getIsDel, 0);
            List<Department> departments = departmentMapper.selectList(departmentQueryWrapper);
            departmentNameMap = departments.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));
        }
        
        // 构建用户ID到部门名称的映射
        Map<Long, String> userDepartmentMap = new HashMap<>();
        for (DepartmentUser du : departmentUsers) {
            String departmentName = departmentNameMap.get(du.getDepartmentId());
            if (departmentName != null) {
                userDepartmentMap.put(du.getUserId(), departmentName);
            }
        }
        
        // 获取来源ID列表
        List<Pair<String, Long>> contentSearchKeys = userCertificates.stream()
                .map(v -> Pair.of(v.getSourceType(), v.getSourceId()))
                .distinct()
                .collect(Collectors.toList());
        Map<String, ContentBO> contentMap = contentService.getContentMap(contentSearchKeys);
        
        // 构建返回结果
        List<CertificateUsersResponse.CertificateUserDTO> userDTOList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (UserCertificate uc : userCertificates) {
            User user = userMap.get(uc.getUserId());
            if (user == null) {
                continue;
            }
            
            CertificateUsersResponse.CertificateUserDTO userDTO = new CertificateUsersResponse.CertificateUserDTO();
            userDTO.setUserId(user.getUserId());
            userDTO.setUserName(user.getNickname());
            userDTO.setEmployeeNo(user.getEmployeeNo());
            userDTO.setDepartment(userDepartmentMap.getOrDefault(user.getUserId(), ""));
            userDTO.setCertificateNo(uc.getCertificateNo());
            // 设置来源名称
            if (StringUtils.isNotBlank(uc.getSourceType()) && uc.getSourceId() != null) {
                ContentBO content = contentMap.get(uc.getSourceType() + "_" + uc.getSourceId());
                if (null != content) {
                    userDTO.setSourceType(content.getType());
                    if (BizType.MAP_STAGE.equals(content.getType()) && StringUtils.isNotBlank(uc.getAttribute(AttributeKey.MAP_ID))) {
                        LearningMap learningMap = learningMapMapper.selectById(Long.valueOf(uc.getAttribute(AttributeKey.MAP_ID)));
                        userDTO.setSourceName(String.format("[%s]-%s", learningMap.getName(), content.getTitle()));
                    } else {
                        userDTO.setSourceName(content.getTitle());
                    }
                }
            }
            
            // 格式化颁发时间
            if (uc.getIssueTime() != null) {
                userDTO.setIssueTime(sdf.format(uc.getIssueTime()));
            }
            
            // 格式化过期时间
            if (uc.getExpireTime() != null) {
                userDTO.setExpireTime(sdf.format(uc.getExpireTime()));
            }
            
            userDTO.setStatus(uc.getStatus());
            userDTO.setStatusName(userDTO.getStatusName());
            
            userDTOList.add(userDTO);
        }
        
        // 构建返回对象
        CertificateUsersResponse response = new CertificateUsersResponse();
        response.setTotal((int) userCertificatePage.getTotal());
        response.setList(userDTOList);
        
        return response;
    }
    
    /**
     * 获取证书发放数量映射
     *
     * @param certificateIds 证书ID列表
     * @return 证书ID -> 发放数量的映射
     */
    private Map<Long, Long> getIssuedCountMap(List<Long> certificateIds) {
        if (certificateIds == null || certificateIds.isEmpty()) {
            return Map.of();
        }
        
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserCertificate::getCertificateId, certificateIds);
        queryWrapper.eq(UserCertificate::getIsDel, 0);List<UserCertificate> userCertificates = userCertificateMapper.selectList(queryWrapper);
        // 统计每个证书的发放数量
        return userCertificates.stream()
                .collect(Collectors.groupingBy(
                        UserCertificate::getCertificateId,
                        Collectors.counting()
                ));
    }
    
    /**
     * 将Certificate实体列表转换为CertificateDTO列表
     *
     * @param certificateList 证书实体列表
     * @param issuedCountMap 证书发放数量映射
     * @return CertificateDTO列表
     */
    private List<CertificateDTO> convertToCertificateDTOList(List<Certificate> certificateList, Map<Long, Long> issuedCountMap) {
        if (certificateList == null || certificateList.isEmpty()) {
            return new ArrayList<>();
        }SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        return certificateList.stream()
                .map(certificate -> {
                    CertificateDTO dto = new CertificateDTO();
                    dto.setId(certificate.getId());
                    dto.setName(certificate.getName());
                    dto.setDescription(certificate.getDescription());
                    dto.setTemplateUrl(certificate.getTemplateUrl());
                    
                    // 格式化过期时间
                    if (certificate.getExpireTime() != null) {
                        dto.setExpireTime(sdf.format(certificate.getExpireTime()));
                    }
                    
                    dto.setCreatorName(certificate.getCreatorName());
                    
                    // 格式化创建时间
                    if (certificate.getGmtCreate() != null) {
                        dto.setGmtCreate(sdf.format(certificate.getGmtCreate()));
                    }
                    
                    // 设置发放数量
                    Long issuedCount = issuedCountMap.getOrDefault(certificate.getId(), 0L);
                    dto.setIssuedCount(issuedCount.intValue());
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
