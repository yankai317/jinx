package com.learn.service.toc.impl;

import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.share.ShareContentResponse;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.service.toc.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 分享服务实现类
 */
@Service
public class ShareServiceImpl implements ShareService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private CertificateMapper certificateMapper;
    
    @Autowired
    private UserCertificateMapper userCertificateMapper;

    @Value("${app.domain:http://localhost:8080}")
    private String domain;

    @Value("${app.qrcode.api:https://api.qrserver.com/v1/create-qr-code/}")
    private String qrcodeApi;

    @Override
    public ApiResponse<ShareContentResponse> getShareContent(String type, Integer id) {
        // 参数校验
        if (!StringUtils.hasText(type) || id == null || id <= 0) {
            return ApiResponse.error(400, "参数错误");
        }

        // 根据类型获取内容信息
        ShareContentResponse response = new ShareContentResponse();
        String shareUrl;

        try {
            switch (type) {
                case BizType.COURSE:Courses course = coursesMapper.selectById(id);
                    if (course == null || course.getIsDel() == 1 || !"published".equals(course.getStatus())) {
                        return ApiResponse.error(404, "课程不存在或未发布");
                    }
                    response.setTitle(course.getTitle());
                    response.setDescription(course.getDescription());
                    response.setCoverImage(course.getCoverImage());
                    shareUrl = domain + "/course/" + id;
                    break;
                case BizType.TRAIN:
                    Train train = trainMapper.selectById(id);
                    if (train == null || train.getIsDel() == 1 || !"published".equals(train.getStatus())) {
                        return ApiResponse.error(404, "培训不存在或未发布");
                    }
                    response.setTitle(train.getName());
                    response.setDescription(train.getIntroduction());
                    response.setCoverImage(train.getCover());
                    shareUrl = domain + "/train/" + id;
                    break;
                case BizType.LEARNING_MAP:
                    LearningMap learningMap = learningMapMapper.selectById(id);
                    if (learningMap == null || learningMap.getIsDel() == 1) {
                        return ApiResponse.error(404, "学习地图不存在");
                    }
                    response.setTitle(learningMap.getName());
                    response.setDescription(learningMap.getIntroduction());
                    response.setCoverImage(learningMap.getCover());
                    shareUrl = domain + "/map/" + id;
                    break;
                case "certificate":
                    // 获取用户证书信息
                    UserCertificate userCertificate = userCertificateMapper.selectById(id);
                    if (userCertificate == null || userCertificate.getIsDel() == 1) {
                        return ApiResponse.error(404, "证书不存在");
                    }
                    
                    // 获取证书模板信息
                    Certificate certificate = certificateMapper.selectById(userCertificate.getCertificateId());
                    if (certificate == null || certificate.getIsDel() == 1) {
                        return ApiResponse.error(404, "证书模板不存在");
                    }
                    
                    response.setTitle(certificate.getName());
                    response.setDescription(certificate.getDescription());
                    response.setCoverImage(certificate.getTemplateUrl());
                    shareUrl = domain + "/certificate/" + id;
                    break;
                default:
                    return ApiResponse.error(400, "不支持的内容类型");
            }

            // 设置分享链接
            response.setShareUrl(shareUrl);

            // 生成二维码URL
            String encodedUrl = URLEncoder.encode(shareUrl, StandardCharsets.UTF_8.toString());
            String qrCodeUrl = qrcodeApi + "?size=200x200&data=" + encodedUrl;
            response.setQrCodeUrl(qrCodeUrl);

            return ApiResponse.success(response);
        } catch (Exception e) {
            throw new CommonException("生成分享信息失败: " + e.getMessage());
        }
    }
}
