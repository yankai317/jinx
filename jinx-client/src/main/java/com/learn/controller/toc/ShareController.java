package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.share.ShareContentResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容分享控制器
 */
@RestController
@RequestMapping("/api/share")
@CrossOrigin
@Slf4j
public class ShareController {

    @Autowired
    private ShareService shareService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取内容分享信息
     *
     * @param type 内容类型：course-课程，train-培训，map-学习地图
     * @param id 内容ID
     * @param request HTTP请求
     * @return 分享信息
     */
    @GetMapping
    public ApiResponse<ShareContentResponse> shareContent(
            @RequestParam String type,
            @RequestParam Integer id,
            HttpServletRequest request) {
        log.info("获取内容分享信息请求，type: {}, id: {}", type, id);

        // 验证用户token
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(request);
        if (userInfo == null) {
            return ApiResponse.error(401, "用户未登录");
        }

        // 参数校验
        if (!StringUtils.hasText(type)) {
            return ApiResponse.error(400, "内容类型不能为空");
        }
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "内容ID不合法");
        }

        // 验证内容类型是否合法
        if (!isValidContentType(type)) {
            return ApiResponse.error(400, "不支持的内容类型");
        }

        // 调用服务获取分享内容信息
        return shareService.getShareContent(type, id);
    }

    /**
     * 验证内容类型是否合法
     *
     * @param type 内容类型
     * @return 是否合法
     */
    private boolean isValidContentType(String type) {
        return BizType.COURSE.equals(type) || BizType.TRAIN.equals(type) || BizType.LEARNING_MAP.equals(type) || "certificate".equals(type);
    }
}
