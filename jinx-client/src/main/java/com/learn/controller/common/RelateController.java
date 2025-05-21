package com.learn.controller.common;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.course.CourseRelateDTO;
import com.learn.service.course.RelateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/4/27
 */
@RestController
@RequestMapping("/api/relate")
@Slf4j
public class RelateController {

    @Autowired
    private RelateService relateService;

    /**
     * 查询证书关联的内容
     *@param type 类型
     * @param bizId ID
     * @return 关联内容列表
     */
    @GetMapping("/query")
    public ApiResponse<List<CourseRelateDTO>> queryRelatedContent(@RequestParam Long bizId, @RequestParam String type) {
        log.info("查询关联内容请求，业务ID：{}, 业务类型", bizId, type);
        // 参数校验
        if (bizId == null || bizId <= 0) {
            return ApiResponse.error(400, "证书ID不能为空且必须大于0");
        }
        // 参数校验
        if (StringUtils.isBlank(type)) {
            return ApiResponse.error(400, "查询类型不能为空");
        }

        try {
            // 调用服务查询证书关联内容
            List<CourseRelateDTO> relateList = relateService.queryContentRelate(bizId, type);
            return ApiResponse.success("查询成功", relateList);
        } catch (Exception e) {
            log.error("查询关联内容失败", e);
            return ApiResponse.error(500, "查询关联内容失败：" + e.getMessage());
        }
    }
}
