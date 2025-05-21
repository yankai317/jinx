package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.exception.CommonException;
import com.learn.common.util.FileUrlUtil;
import com.learn.constants.BizType;
import com.learn.dto.toc.certificate.CertificateDetailResponse;
import com.learn.dto.toc.certificate.UserCertificatesRequest;
import com.learn.dto.toc.certificate.UserCertificatesResponse;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.service.FileService;
import com.learn.service.toc.UserCertificateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户证书服务实现类
 */
@Service
@Slf4j
public class UserCertificateServiceImpl implements UserCertificateService {

    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private CertificateMapper certificateMapper;
    
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private FileService fileService;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public UserCertificatesResponse getUserCertificates(Long userId, UserCertificatesRequest request) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        // 设置默认值
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
        
        // 构建查询条件
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCertificate::getUserId, userId);
        queryWrapper.eq(UserCertificate::getIsDel, 0); // 未删除的记录
        
        // 根据状态过滤
        if (StringUtils.hasText(request.getStatus()) && !"all".equals(request.getStatus())) {
            switch (request.getStatus()) {
                case "valid":
                    queryWrapper.eq(UserCertificate::getStatus, 0);
                    break;
                case "expired":
                    queryWrapper.eq(UserCertificate::getStatus, 1);
                    break;
                case "revoked":
                    queryWrapper.eq(UserCertificate::getStatus, 2);
                    break;
                default:
                    // 默认不做过滤
                    break;
            }
        }
        
        // 按颁发时间降序排序
        queryWrapper.orderByDesc(UserCertificate::getIssueTime);
        
        // 分页查询
        Page<UserCertificate> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<UserCertificate> userCertificatePage = userCertificateMapper.selectPage(page, queryWrapper);
        
        // 获取证书ID列表
        List<Long> certificateIds = userCertificatePage.getRecords().stream()
                .map(UserCertificate::getCertificateId)
                .collect(Collectors.toList());
        
        // 批量查询证书信息
        List<Certificate> certificates = new ArrayList<>();
        if (!certificateIds.isEmpty()) {
            certificates = certificateMapper.selectBatchIds(certificateIds);
        }
        
        // 构建证书ID到证书的映射
        Map<Long, Certificate> certificateMap = certificates.stream()
                .collect(Collectors.toMap(Certificate::getId, Function.identity()));
        
        // 获取来源ID列表
        List<Long> trainSourceIds = new ArrayList<>();
        List<Long> mapSourceIds = new ArrayList<>();
        
        for (UserCertificate userCertificate : userCertificatePage.getRecords()) {
            if (BizType.TRAIN.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
                trainSourceIds.add(userCertificate.getSourceId());
            } else if (BizType.LEARNING_MAP.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
                mapSourceIds.add(userCertificate.getSourceId());
            }
        }
        
        // 批量查询培训和学习地图信息
        Map<Long, Train> trainMap = new java.util.HashMap<>();
        Map<Long, LearningMap> mapMap = new java.util.HashMap<>();
        
        if (!trainSourceIds.isEmpty()) {
            List<Train> trains = trainMapper.selectBatchIds(trainSourceIds);
            trainMap = trains.stream().collect(Collectors.toMap(Train::getId, Function.identity()));
        }
        
        if (!mapSourceIds.isEmpty()) {
            List<LearningMap> maps = learningMapMapper.selectBatchIds(mapSourceIds);
            mapMap = maps.stream().collect(Collectors.toMap(LearningMap::getId, Function.identity()));
        }
        
        // 构建响应对象
        UserCertificatesResponse response = new UserCertificatesResponse();
        response.setTotal(userCertificatePage.getTotal());
        response.setPageNum(request.getPageNum());
        response.setPageSize(request.getPageSize());
        response.setTotalPages((int) Math.ceil((double) userCertificatePage.getTotal() / request.getPageSize()));
        
        // 构建证书列表
        List<UserCertificatesResponse.UserCertificateDTO> certificateDTOList = new ArrayList<>();
        for (UserCertificate userCertificate : userCertificatePage.getRecords()) {
            UserCertificatesResponse.UserCertificateDTO dto = new UserCertificatesResponse.UserCertificateDTO();
            dto.setId(userCertificate.getId());
            dto.setCertificateId(userCertificate.getCertificateId());
            dto.setCertificateNo(userCertificate.getCertificateNo());
            dto.setSourceType(userCertificate.getSourceType());
            dto.setSourceTypeName(userCertificate.getSourceTypeName());
            dto.setStatus(userCertificate.getStatus());
            
            // 设置颁发时间和过期时间
            if (userCertificate.getIssueTime() != null) {
                dto.setIssueTime(DATE_FORMAT.format(userCertificate.getIssueTime()));
            }
            if (userCertificate.getExpireTime() != null) {
                dto.setExpireTime(DATE_FORMAT.format(userCertificate.getExpireTime()));
            }
            
            // 设置证书名称和模板URL
            Certificate certificate = certificateMap.get(userCertificate.getCertificateId());
            if (certificate != null) {
                dto.setName(certificate.getName());
                dto.setTemplateUrl(certificate.getTemplateUrl());
            }
            
            // 设置来源名称
            if (BizType.TRAIN.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
                Train train = trainMap.get(userCertificate.getSourceId());
                if (train != null) {
                    dto.setSourceName(train.getName());
                }
            } else if (BizType.LEARNING_MAP.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
                LearningMap map = mapMap.get(userCertificate.getSourceId());
                if (map != null) {
                    dto.setSourceName(map.getName());
                }
            } else if ("exam".equals(userCertificate.getSourceType())) {
                dto.setSourceName("考试");
            }
            
            certificateDTOList.add(dto);
        }
        
        response.setList(certificateDTOList);
        return response;
    }
    
    @Override
    public CertificateDetailResponse getCertificateDetail(Long userId, Long certificateId) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (certificateId == null) {
            throw new IllegalArgumentException("证书ID不能为空");
        }
        
        // 查询用户证书
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCertificate::getUserId, userId);
        queryWrapper.eq(UserCertificate::getId, certificateId);
        queryWrapper.eq(UserCertificate::getIsDel, 0); // 未删除的记录
        
        UserCertificate userCertificate = userCertificateMapper.selectOne(queryWrapper);
        if (userCertificate == null) {
            throw new CommonException("证书不存在或已被删除");
        }
        
        // 查询证书信息
        Certificate certificate = certificateMapper.selectById(userCertificate.getCertificateId());
        if (certificate == null) {
            throw new CommonException("证书模板不存在");
        }
        
        // 构建响应对象
        CertificateDetailResponse response = new CertificateDetailResponse();
        response.setId(userCertificate.getId());
        response.setCertificateId(userCertificate.getCertificateId());
        response.setName(certificate.getName());
        response.setCertificateNo(userCertificate.getCertificateNo());
        response.setTemplateUrl(certificate.getTemplateUrl());
        response.setSourceType(userCertificate.getSourceType());
        response.setSourceId(userCertificate.getSourceId());
        response.setStatus(userCertificate.getStatus());
        response.setDescription(certificate.getDescription());
        
        // 设置颁发时间和过期时间
        if (userCertificate.getIssueTime() != null) {
            response.setIssueTime(DATE_FORMAT.format(userCertificate.getIssueTime()));
        }
        if (userCertificate.getExpireTime() != null) {
            response.setExpireTime(DATE_FORMAT.format(userCertificate.getExpireTime()));
        }
        
        // 设置来源名称
        if (BizType.TRAIN.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
            Train train = trainMapper.selectById(userCertificate.getSourceId());
            if (train != null) {
                response.setSourceName(train.getName());
            }
        } else if (BizType.LEARNING_MAP.equals(userCertificate.getSourceType()) && userCertificate.getSourceId() != null) {
            LearningMap map = learningMapMapper.selectById(userCertificate.getSourceId());
            if (map != null) {
                response.setSourceName(map.getName());
            }
        } else if ("exam".equals(userCertificate.getSourceType())) {
            response.setSourceName("考试");
        }
        
        return response;
    }
    
    @Override
    public String downloadCertificate(Long userId, Long certificateId) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (certificateId == null) {
            throw new IllegalArgumentException("证书ID不能为空");
        }
        
        // 查询用户证书
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCertificate::getUserId, userId);
        queryWrapper.eq(UserCertificate::getId, certificateId);
        queryWrapper.eq(UserCertificate::getIsDel, 0); // 未删除的记录
        
        UserCertificate userCertificate = userCertificateMapper.selectOne(queryWrapper);
        if (userCertificate == null) {
            throw new CommonException("证书不存在或已被删除");
        }
        
        // 查询证书信息
        Certificate certificate = certificateMapper.selectById(userCertificate.getCertificateId());
        if (certificate == null) {
            throw new CommonException("证书模板不存在");
        }
        
        // 获取证书下载URL
        String downloadUrl = certificate.getTemplateUrl();
        if (!StringUtils.hasText(downloadUrl)) {
            throw new CommonException("证书模板URL不存在");
        }
        
        // 使用文件服务生成下载链接
        try {
            // 这里假设FileService有一个生成下载链接的方法
            // 实际实现可能需要根据项目的文件服务来调整
            return downloadUrl;
        } catch (Exception e) {
            log.error("生成证书下载链接失败", e);
            throw new CommonException("生成证书下载链接失败：" + e.getMessage());
        }
    }
}
