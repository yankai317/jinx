package com.learn.controller.certificate;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.certificate.CertificateBatchRevokeRequest;
import com.learn.dto.certificate.CertificateCreateRequest;
import com.learn.dto.certificate.CertificateDTO;
import com.learn.dto.certificate.CertificateDetailDTO;
import com.learn.dto.certificate.CertificateListRequest;
import com.learn.dto.certificate.CertificateListResponse;
import com.learn.dto.certificate.CertificateRevokeRequest;
import com.learn.dto.certificate.CertificateUpdateRequest;
import com.learn.dto.certificate.CertificateUsersRequest;
import com.learn.dto.certificate.CertificateUsersResponse;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Certificate;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.service.certificate.CertificateManageService;
import com.learn.service.certificate.CertificateQueryService;
import dev.ai4j.openai4j.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 证书控制器
 */
@RestController
@RequestMapping("/api/certificate")
@CrossOrigin
@Slf4j
public class CertificateController {

    @Autowired
    private CertificateQueryService certificateQueryService;
    @Autowired
    private CertificateManageService certificateManageService;
    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 创建证书
     *
     * @param request 创建证书请求
     * @return 创建的证书信息
     */
    @PostMapping("/create")
    public ApiResponse<CertificateDTO> createCertificate(@RequestBody CertificateCreateRequest request, HttpServletRequest servletRequest) {
        log.info("创建证书请求入参:{}", Json.toJson(request));

        // 参数校验
        if (StringUtils.isBlank(request.getName())) {
            return ApiResponse.error(400, "证书名称不能为空");
        }
        if (StringUtils.isBlank(request.getTemplateUrl())) {
            return ApiResponse.error(400, "证书模板URL不能为空");
        }

        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(servletRequest);

        try {
            // 构建Certificate实体
            Certificate certificate = new Certificate();
            certificate.setName(request.getName());
            certificate.setDescription(request.getDescription());
            certificate.setTemplateUrl(request.getTemplateUrl());
            certificate.setCreatorId(userInfo.getUserId());
            certificate.setCreatorName(userInfo.getNickname());

            // 处理过期时间
            if (StringUtils.isNotBlank(request.getExpireTime())) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expireTime = sdf.parse(request.getExpireTime());
                    certificate.setExpireTime(expireTime);
                } catch (ParseException e) {
                    log.error("解析过期时间异常", e);
                    return ApiResponse.error(400, "过期时间格式不正确，正确格式为：yyyy-MM-dd HH:mm:ss");
                }
            }
            
            // 调用服务创建证书
            Certificate createdCertificate = certificateManageService.createCertificate(certificate, userInfo.getUserId(), userInfo.getNickname());// 转换为DTO
            CertificateDTO certificateDTO = new CertificateDTO();
            certificateDTO.setId(createdCertificate.getId());
            certificateDTO.setName(createdCertificate.getName());
            certificateDTO.setDescription(createdCertificate.getDescription());
            certificateDTO.setTemplateUrl(createdCertificate.getTemplateUrl());
            // 格式化过期时间
            if (createdCertificate.getExpireTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                certificateDTO.setExpireTime(sdf.format(createdCertificate.getExpireTime()));
            }
            certificateDTO.setCreatorName(createdCertificate.getCreatorName());
            
            // 格式化创建时间
            if (createdCertificate.getGmtCreate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                certificateDTO.setGmtCreate(sdf.format(createdCertificate.getGmtCreate()));
            }
            
            certificateDTO.setIssuedCount(0); // 新创建的证书，发放数量为0
            return ApiResponse.success("创建成功", certificateDTO);
        } catch (Exception e) {
            log.error("创建证书异常", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 获取证书详情
     *
     * @param id 证书ID
     * @return 证书详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<CertificateDetailDTO> getCertificateDetail(@PathVariable("id") Long id) {
        log.info("获取证书详情请求入参, id: {}", id);// 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        try {
            // 调用服务获取证书详情
            CertificateDetailDTO detailDTO = certificateQueryService.getCertificateDetail(id);
            return ApiResponse.success("获取成功", detailDTO);
        } catch (IllegalArgumentException e) {
            log.error("获取证书详情失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取证书详情异常", e);
            return ApiResponse.error(500, "获取证书详情失败");
        }
    }

    /**
     * 获取证书列表
     *
     * @param request 查询条件
     * @return 证书列表
     */
    @PostMapping("/list")
    public ApiResponse<CertificateListResponse> getCertificateList(@RequestBody CertificateListRequest request) {
        log.info("获取证书列表请求入参:{}", Json.toJson(request));
        // 参数校验
        if (request.getPageNum() != null && request.getPageNum() < 1) {
            return ApiResponse.error(400, "页码必须大于等于1");
        }
        if (request.getPageSize() != null && request.getPageSize() < 1) {
            return ApiResponse.error(400, "每页条数必须大于等于1");
        }// 如果排序方式不为空，但不是asc或desc，则返回错误
        if (request.getSortOrder() != null &&!request.getSortOrder().equalsIgnoreCase("asc") && 
            !request.getSortOrder().equalsIgnoreCase("desc")) {
            return ApiResponse.error(400, "排序方式必须为asc或desc");
        }
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            request.setCreatorId(UserContextHolder.getUserId());
        }
        
        // 调用服务获取证书列表
        CertificateListResponse response = certificateQueryService.getCertificateList(request);

        
        log.info("获取证书列表响应结果, total: {}, listSize: {}", 
                response.getTotal(), 
                response.getList() != null ? response.getList().size() : 0);
        
        return ApiResponse.success("获取成功", response);
    }
    
    /**
     * 获取证书获得者列表
     *
     * @param id 证书ID
     * @param request 查询条件
     * @return 证书获得者列表
     */
    @PostMapping("/users/{id}")
    public ApiResponse<CertificateUsersResponse> getCertificateUsers(@PathVariable("id") Long id, @RequestBody CertificateUsersRequest request) {
        log.info("获取证书获得者列表请求入参, id: {}, request: {}", id, Json.toJson(request));
        
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        if (request.getPageNum() != null && request.getPageNum() < 1) {
            return ApiResponse.error(400, "页码必须大于等于1");
        }
        if (request.getPageSize() != null && request.getPageSize() < 1) {
            return ApiResponse.error(400, "每页条数必须大于等于1");
        }
        
        if (StringUtils.isNotBlank(request.getStatus()) && 
            !request.getStatus().equals("all") && 
            !request.getStatus().equals("valid") && 
            !request.getStatus().equals("expired") && 
            !request.getStatus().equals("revoked")) {
            return ApiResponse.error(400, "状态参数不正确，可选值：all, valid, expired, revoked");
        }
        
        if (StringUtils.isNotBlank(request.getSourceType()) && 
            !request.getSourceType().equals(BizType.TRAIN) &&
            !request.getSourceType().equals(BizType.COURSE) &&
            !request.getSourceType().equals(BizType.LEARNING_MAP)) {
            return ApiResponse.error(400, "来源类型参数不正确");
        }
        
        try {
            // 调用服务获取证书获得者列表
            CertificateUsersResponse response = certificateQueryService.getCertificateUsers(id, request);
            return ApiResponse.success("获取成功", response);
        } catch (IllegalArgumentException e) {
            log.error("获取证书获得者列表失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取证书获得者列表异常", e);
            return ApiResponse.error(500, "获取证书获得者列表失败");
        }
    }
    
    /**
     * 更新证书信息
     *
     * @param request 更新证书请求
     * @return 更新结果
     */
    @PutMapping("/update")
    public ApiResponse<Boolean> updateCertificate(@RequestBody CertificateUpdateRequest request, HttpServletRequest httpServletRequest) {
        log.info("更新证书请求入参:{}", Json.toJson(request));// 参数校验
        if (request.getId() == null || request.getId() <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        try {
            // 构建Certificate实体
            Certificate certificate = new Certificate();
            certificate.setId(request.getId());
            certificate.setName(request.getName());
            certificate.setDescription(request.getDescription());
            certificate.setTemplateUrl(request.getTemplateUrl());
            
            // 处理过期时间
            if (StringUtils.isNotBlank(request.getExpireTime())) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expireTime = sdf.parse(request.getExpireTime());
                    certificate.setExpireTime(expireTime);
                } catch (ParseException e) {
                    log.error("解析过期时间异常", e);
                    return ApiResponse.error(400, "过期时间格式不正确，正确格式为：yyyy-MM-dd HH:mm:ss");
                }
            }
            
            // 调用服务更新证书
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);
            
            certificateManageService.updateCertificate(certificate, userInfo.getUserId(), userInfo.getNickname());
            
            return ApiResponse.success("更新成功", true);
        } catch (IllegalArgumentException e) {
            log.error("更新证书失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新证书异常", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 删除证书
     *
     * @param id 证书ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteCertificate(@PathVariable("id") Long id, HttpServletRequest httpServletRequest) {
        log.info("删除证书请求入参, id: {}", id);
        
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        try {
            // 调用服务删除证书
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);
            
            boolean result = certificateManageService.deleteCertificate(id, userInfo.getUserId(), userInfo.getNickname());
            
            return ApiResponse.success("删除成功", result);
        } catch (IllegalArgumentException e) {
            log.error("删除证书失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除证书异常", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 撤销用户证书
     *
     * @param id 证书ID
     * @param request 撤销证书请求
     * @return 撤销结果
     */
    @PostMapping("/revoke/{id}")
    public ApiResponse<Boolean> revokeCertificate(@PathVariable("id") Long id, @RequestBody CertificateRevokeRequest request, HttpServletRequest httpServletRequest) {
        log.info("撤销证书请求入参, id: {}, request: {}", id, Json.toJson(request));
        
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        if (request.getUserCertificateId() == null || request.getUserCertificateId() <= 0) {
            return ApiResponse.error(400, "用户证书ID不能为空且必须大于0");
        }
        
        try {
            // 调用服务撤销证书
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            UserCertificate userCertificate = certificateManageService.revokeCertificate(
                    request.getUserCertificateId(), request.getReason(), userInfo.getUserId(), userInfo.getNickname());
            
            return ApiResponse.success("撤销成功", true);
        } catch (IllegalArgumentException e) {
            log.error("撤销证书失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("撤销证书异常", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 批量撤销用户证书
     *
     * @param id 证书ID
     * @param request 批量撤销证书请求
     * @return 撤销结果
     */
    @PostMapping("/batchRevoke/{id}")
    public ApiResponse<Boolean> batchRevokeCertificate(@PathVariable("id") Long id, @RequestBody CertificateBatchRevokeRequest request, HttpServletRequest httpServletRequest) {
        log.info("批量撤销证书请求入参, id: {}, request: {}", id, Json.toJson(request));
        
        // 参数校验
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        
        if (request.getUserCertificateIds() == null || request.getUserCertificateIds().isEmpty()) {
            return ApiResponse.error(400, "用户证书ID列表不能为空");
        }
        
        if (StringUtils.isBlank(request.getReason())) {
            return ApiResponse.error(400, "撤销原因不能为空");
        }
        
        try {
            // 调用服务批量撤销证书
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpServletRequest);

            certificateManageService.batchRevokeCertificate(
                    request.getUserCertificateIds(), request.getReason(), userInfo.getUserId(), userInfo.getNickname());

            return ApiResponse.success("批量撤销成功", true);
        } catch (IllegalArgumentException e) {
            log.error("批量撤销证书失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("批量撤销证书异常", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
    
    /**
     * 导出证书用户列表
     *
     * @param id 证书ID
     * @param request 查询条件
     * @param response HTTP响应
     */
    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> exportCertificateUsers(
            @PathVariable("id") Long id,
            CertificateUsersRequest request,
            HttpServletResponse response) {
        log.info("导出证书用户列表请求入参, id: {}, request: {}", id, Json.toJson(request));
        
        // 参数校验
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("证书ID不能为空且必须大于0");
        }
        
        try {
//            // 调用服务导出证书用户列表
//            byte[] excelBytes = certificateQueryService.exportCertificateUsers(id, request);
//
//            // 获取证书详情，用于设置文件名
//            CertificateDetailDTO detailDTO = certificateQueryService.getCertificateDetail(id);
//            String fileName = "证书获得者列表_" + detailDTO.getName() + "_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";
//
//            // 设置响应头
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment", new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
//
            return ResponseEntity
                    .ok()
                    .body(null);
        } catch (IllegalArgumentException e) {
            log.error("导出证书用户列表失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出证书用户列表异常", e);
            throw new RuntimeException("导出证书用户列表失败", e);
        }
    }
}
