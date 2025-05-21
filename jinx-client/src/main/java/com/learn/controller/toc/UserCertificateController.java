package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.exception.CommonException;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.certificate.UserCertificatesRequest;
import com.learn.dto.toc.certificate.UserCertificatesResponse;
import com.learn.dto.toc.certificate.CertificateDetailResponse;
import com.learn.service.toc.UserCertificateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户证书控制器
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin
@Slf4j
public class UserCertificateController {

    @Autowired
    private UserCertificateService userCertificateService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取用户证书列表
     *
     * @param request HTTP请求
     * @param status 证书状态：0-全部，1-有效，2-已过期，3-已撤销，默认0
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 用户证书列表
     */
    @GetMapping("/certificates")
    public ApiResponse<UserCertificatesResponse> getUserCertificates(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        // 构建请求参数
        UserCertificatesRequest certificatesRequest = new UserCertificatesRequest();
        
        // 设置状态参数
        switch (status) {
            case 0:
                certificatesRequest.setStatus("all"); // 全部
                break;
            case 1:
                certificatesRequest.setStatus("valid"); // 有效
                break;
            case 2:
                certificatesRequest.setStatus("expired"); // 已过期
                break;
            case 3:
                certificatesRequest.setStatus("revoked"); // 已撤销
                break;
            default:
                certificatesRequest.setStatus("all"); // 默认全部
                break;
        }
        
        // 设置分页参数
        certificatesRequest.setPageNum(pageNum);
        certificatesRequest.setPageSize(pageSize);
        
        try {
            // 调用服务获取用户证书列表
            UserCertificatesResponse response = userCertificateService.getUserCertificates(userId, certificatesRequest);
            return ApiResponse.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取用户证书列表失败", e);
            return ApiResponse.error(500, "获取用户证书列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取证书详情
     *
     * @param request HTTP请求
     * @param certificateId 用户证书ID
     * @return 证书详情
     */
    @GetMapping("/certificate/{certificateId}")
    public ApiResponse<CertificateDetailResponse> getCertificateDetail(
            HttpServletRequest request,
            @PathVariable Long certificateId) {
        
        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        try {
            // 调用服务获取证书详情
            CertificateDetailResponse response = userCertificateService.getCertificateDetail(userId, certificateId);
            return ApiResponse.success("获取成功", response);
        } catch (CommonException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取证书详情失败", e);
            return ApiResponse.error(500, "获取证书详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载证书
     *
     * @param request HTTP请求
     * @param certificateId 用户证书ID
     * @return 证书下载URL
     */
    @GetMapping("/certificate/{certificateId}/download")
    public ApiResponse<String> downloadCertificate(
            HttpServletRequest request,
            @PathVariable Long certificateId) {
        
        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        try {
            // 调用服务获取证书下载URL
            String downloadUrl = userCertificateService.downloadCertificate(userId, certificateId);
            return ApiResponse.success("获取成功", downloadUrl);
        } catch (CommonException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取证书下载链接失败", e);
            return ApiResponse.error(500, "获取证书下载链接失败：" + e.getMessage());
        }
    }
}
