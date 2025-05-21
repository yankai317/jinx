package com.learn.service.certificate.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.exception.CommonException;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.service.certificate.CertificateManageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 证书管理服务实现类
 */
@Service
@Slf4j
public class CertificateManageServiceImpl implements CertificateManageService {

    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private TrainMapper trainMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private UserCertificateMapper userCertificateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Certificate createCertificate(Certificate certificate, Long creatorId, String creatorName) {
        // 参数校验
        if (certificate == null) {
            throw new CommonException("证书信息不能为空");
        }
        if (StringUtils.isBlank(certificate.getName())) {
            throw new CommonException("证书名称不能为空");
        }
        if (StringUtils.isBlank(certificate.getTemplateUrl())) {
            throw new CommonException("证书模板URL不能为空");
        }
        
        // 检查证书名称是否重复
        LambdaQueryWrapper<Certificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Certificate::getName, certificate.getName())
                .eq(Certificate::getIsDel, 0);
        Long count = certificateMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new CommonException("证书名称已存在");
        }
        
        // 设置创建人信息
        certificate.setCreatorId(creatorId);
        certificate.setCreatorName(creatorName);
        certificate.setUpdaterId(creatorId);
        certificate.setUpdaterName(creatorName);
        
        // 设置创建时间和更新时间
        Date now = new Date();
        certificate.setGmtCreate(now);
        certificate.setGmtModified(now);
        
        // 设置逻辑删除标记
        certificate.setIsDel(0);// 插入数据库
        certificateMapper.insert(certificate);
        
        return certificate;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Certificate updateCertificate(Certificate certificate, Long updaterId, String updaterName) {
        // 参数校验
        if (certificate == null) {
            throw new CommonException("证书信息不能为空");
        }
        if (certificate.getId() == null || certificate.getId() <= 0) {
            throw new CommonException("证书ID不能为空且必须大于0");
        }
        
        // 查询原证书信息
        Certificate existCertificate = certificateMapper.selectById(certificate.getId());
        if (existCertificate == null || existCertificate.getIsDel() == 1) {
            throw new CommonException("证书不存在或已被删除");
        }
        
        // 如果更新了证书名称，需要检查名称是否重复
        if (StringUtils.isNotBlank(certificate.getName()) && !certificate.getName().equals(existCertificate.getName())) {
            LambdaQueryWrapper<Certificate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Certificate::getName, certificate.getName())
                    .eq(Certificate::getIsDel, 0)
                    .ne(Certificate::getId, certificate.getId());
            Long count = certificateMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new CommonException("证书名称已存在");
            }
            existCertificate.setName(certificate.getName());
        }
        
        // 更新证书描述（如有变更）
        if (certificate.getDescription() != null) {
            existCertificate.setDescription(certificate.getDescription());
        }
        
        // 更新证书模板URL（如有变更）
        if (StringUtils.isNotBlank(certificate.getTemplateUrl())) {
            existCertificate.setTemplateUrl(certificate.getTemplateUrl());
        }// 更新过期时间（如有变更）
        if (certificate.getExpireTime() != null) {
            existCertificate.setExpireTime(certificate.getExpireTime());
        }// 设置更新人信息
        existCertificate.setUpdaterId(updaterId);
        existCertificate.setUpdaterName(updaterName);
        
        // 设置更新时间
        existCertificate.setGmtModified(new Date());
        
        // 更新数据库
        certificateMapper.updateById(existCertificate);
        
        return existCertificate;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCertificate(Long id, Long updaterId, String updaterName) {
        // 参数校验
        if (id == null || id <= 0) {
            throw new CommonException("证书ID不能为空且必须大于0");
        }
        
        // 查询证书是否存在
        Certificate certificate = certificateMapper.selectById(id);
        if (certificate == null || certificate.getIsDel() == 1) {
            throw new CommonException("证书不存在或已被删除");
        }
        
        // 检查证书是否被培训引用
        LambdaQueryWrapper<Train> trainQueryWrapper = new LambdaQueryWrapper<>();
        trainQueryWrapper.eq(Train::getCertificateId, id)
                .eq(Train::getIsDel, 0);
        Long trainCount = trainMapper.selectCount(trainQueryWrapper);
        if (trainCount > 0) {
            throw new CommonException("该证书已被培训引用，无法删除");
        }
        
        // 检查证书是否被content_relation表引用（train有效引用）
        LambdaQueryWrapper<ContentRelation> contentRelationQueryWrapper = new LambdaQueryWrapper<>();
        contentRelationQueryWrapper.eq(ContentRelation::getContentId, id)
                .eq(ContentRelation::getContentType, "CERTIFICATE")
                .eq(ContentRelation::getIsDel, 0);
        Long contentRelationCount = contentRelationMapper.selectCount(contentRelationQueryWrapper);
        if (contentRelationCount > 0) {
            throw new CommonException("该证书已被培训内容引用，无法删除");
        }
        
        // 检查证书是否被学习地图引用
        LambdaQueryWrapper<LearningMap> mapQueryWrapper = new LambdaQueryWrapper<>();
        mapQueryWrapper.eq(LearningMap::getCertificateId, id)
                .eq(LearningMap::getIsDel, 0);
        Long mapCount = learningMapMapper.selectCount(mapQueryWrapper);
        if (mapCount > 0) {
            throw new CommonException("该证书已被学习地图引用，无法删除");
        }
        
        // 检查证书是否被学习地图阶段引用
        LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
        stageQueryWrapper.eq(LearningMapStage::getCertificateId, id)
                .eq(LearningMapStage::getIsDel, 0);
        Long stageCount = learningMapStageMapper.selectCount(stageQueryWrapper);
        if (stageCount > 0) {
            throw new CommonException("该证书已被学习地图阶段引用，无法删除");
        }
        
        // 逻辑删除证书
        certificate.setIsDel(1);
        certificate.setUpdaterId(updaterId);
        certificate.setUpdaterName(updaterName);
        certificate.setGmtModified(new Date());
        
        int result = certificateMapper.updateById(certificate);
        return result > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCertificate revokeCertificate(Long userCertificateId, String reason, Long updaterId, String updaterName) {
        // 参数校验
        if (userCertificateId == null || userCertificateId <= 0) {
            throw new CommonException("用户证书ID不能为空且必须大于0");
        }
        
        // 查询用户证书是否存在
        UserCertificate userCertificate = userCertificateMapper.selectById(userCertificateId);
        if (userCertificate == null || userCertificate.getIsDel() == 1) {
            throw new CommonException("用户证书不存在或已被删除");
        }
        
        // 检查证书状态是否已经是撤销状态
        if (userCertificate.getStatus() == 2) {
            throw new CommonException("该证书已经被撤销");
        }// 更新证书状态为已撤销
        userCertificate.setStatus(2); // 2-已撤销
        userCertificate.setUpdaterId(updaterId);
        userCertificate.setUpdaterName(updaterName);
        userCertificate.setGmtModified(new Date());
        
        // 更新数据库
        userCertificateMapper.updateById(userCertificate);
        
        return userCertificate;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRevokeCertificate(List<Long> userCertificateIds, String reason, Long updaterId, String updaterName) {
        // 参数校验
        if (userCertificateIds == null || userCertificateIds.isEmpty()) {
            throw new CommonException("用户证书ID列表不能为空");
        }
        
        if (StringUtils.isBlank(reason)) {
            throw new CommonException("撤销原因不能为空");
        }
        
        // 批量查询用户证书
        LambdaQueryWrapper<UserCertificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserCertificate::getId, userCertificateIds)
                .eq(UserCertificate::getIsDel, 0);
        List<UserCertificate> userCertificates = userCertificateMapper.selectList(queryWrapper);
        
        // 检查是否所有证书都存在
        if (userCertificates.size() != userCertificateIds.size()) {
            throw new CommonException("部分用户证书不存在或已被删除");
        }
        
        // 筛选出未撤销的证书ID
        List<Long> notRevokedIds = userCertificates.stream()
                .filter(cert -> cert.getStatus() != 2)
                .map(UserCertificate::getId)
                .toList();
        
        if (notRevokedIds.isEmpty()) {
            log.info("所有证书已经是撤销状态，无需操作");
            return;
        }
        
        Date now = new Date();
        
        // 使用批量更新方式，一次性更新所有未撤销的证书
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCertificate> updateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.in(UserCertificate::getId, notRevokedIds)
                .eq(UserCertificate::getIsDel, 0)
                .ne(UserCertificate::getStatus, 2)
                .set(UserCertificate::getStatus, 2)
                .set(UserCertificate::getUpdaterId, updaterId)
                .set(UserCertificate::getUpdaterName, updaterName)
                .set(UserCertificate::getGmtModified, now);
        
        userCertificateMapper.update(null, updateWrapper);

        log.info("批量撤销证书成功，共撤销 {} 个证书", notRevokedIds.size());
    }
}
