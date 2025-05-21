package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.map.MapDetailResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.MapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习地图详情控制器
 */
@RestController
@RequestMapping("/api/map")
@Slf4j
public class MapDetailController {

    @Autowired
    private MapService mapService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取学习地图详情
     *
     * @param id 学习地图ID
     * @param request HTTP请求
     * @return 学习地图详情响应
     */
    @GetMapping("/detail")
    public MapDetailResponse getMapDetail(@RequestParam("id") Integer id, HttpServletRequest request) {
        log.info("获取学习地图详情，地图ID：{}", id);
        // 参数校验
        if (id == null || id <= 0) {
            MapDetailResponse response = new MapDetailResponse();
            response.setCode(400);
            response.setMessage("参数错误：学习地图ID无效");
            return response;
        }
        
        // 获取当前用户信息
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(request);
        if (userInfo == null) {
            MapDetailResponse response = new MapDetailResponse();
            response.setCode(401);
            response.setMessage("未登录或登录已过期");
            return response;
        }
        
        // 调用服务获取学习地图详情
        return mapService.getMapDetail(id.longValue(), userInfo.getUserId());
    }
}
