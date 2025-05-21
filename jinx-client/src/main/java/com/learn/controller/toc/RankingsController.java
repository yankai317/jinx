package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.rankings.RankingsResponse;
import com.learn.service.toc.RankingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜控制器
 */
@RestController
@RequestMapping("/api/toc")
@Slf4j
public class RankingsController {

    @Autowired
    private RankingsService rankingsService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取学习排行榜数据
     *
     * @param type 排行榜类型：all-全员排行，department-部门排行
     * @param departmentId 部门ID，type=department时必填
     * @param limit 返回数量限制，默认10
     * @param request HTTP请求
     * @return 排行榜数据
     */
    @GetMapping("/rankings")
    public ApiResponse<RankingsResponse> getRankings(
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            HttpServletRequest request) {

        // 参数校验
        if ("department".equals(type) && departmentId == null) {
            return ApiResponse.error(400, "部门排行榜需要提供部门ID");
        }

        // 获取当前用户ID
        Long userId = userTokenUtil.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }

        // 如果是部门排行榜，需要使用指定的部门ID
        if ("department".equals(type) && departmentId != null) {
            // 调用服务获取排行榜数据
            RankingsResponse response = rankingsService.getRankings(type, limit, userId, departmentId.longValue());
            return ApiResponse.success(response);
        } else {
            // 全员排行榜
            RankingsResponse response = rankingsService.getRankings("all", limit, userId, null);
            return ApiResponse.success(response);
        }
    }
}
