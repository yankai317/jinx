package com.learn.controller.toc;

import com.alibaba.mos.boot.event.utils.MosBootEventUtils;
import com.alibaba.mos.boot.kv.ability.MosBootKVAbility;
import com.alibaba.mos.boot.kv.ability.MosBootLockAbility;
import com.alibaba.mos.boot.kv.ability.data.MosBootKV;
import com.alibaba.mos.boot.kv.ability.data.MosBootKVResult;
import com.alibaba.mos.boot.kv.ability.data.MosBootKVResultCode;
import com.alibaba.mos.boot.kv.spring.propeties.MosBootAllKvProperties;
import com.alibaba.mos.boot.util.MosBootSpiLoader;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.learning.RecordLearningProgressResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.LearningProgressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY;
import static com.learn.constants.KeyConstant.LEARNING_RECORD_KEY_EXPIRE_TIME;

/**
 * 学习进度控制器
 */
@RestController
@RequestMapping("/api/learning")
@CrossOrigin
@Slf4j
public class LearningProgressController {

    private final MosBootLockAbility lock = MosBootSpiLoader.getSpiService(MosBootLockAbility.class);

    private final static int EXPIRE_TIME = 5;

    @Autowired
    private LearningProgressService learningProgressService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 记录学习进度
     *
     * @param request 记录学习进度请求
     * @return 记录学习进度响应
     */
    @PostMapping("/record")
    public ApiResponse<Map<String, Object>> recordLearningProgress(
            @RequestBody RecordLearningProgressRequest request) {
        log.info("记录学习进度请求入参: {}", request);
        // 获取当前用户信息
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }


        // 参数校验
        if (StringUtils.isBlank(request.getContentType())) {
            return ApiResponse.error(400, "内容类型不能为空");
        }
        if (request.getContentId() == null) {
            return ApiResponse.error(400, "内容ID不能为空");
        }
        if (request.getProgress() == null) {
            return ApiResponse.error(400, "学习进度不能为空");
        }
        if (request.getDuration() == null) {
            return ApiResponse.error(400, "学习时长不能为空");
        }
        // 兼容
        if (BizType.SERIES_COURSE.equals(request.getParentType())) {
            request.setParentType(BizType.COURSE);
        }
        if (BizType.SERIES_COURSE.equals(request.getContentType())) {
            request.setContentType(BizType.COURSE);
        }

        if (!lock.tryLock(LEARNING_RECORD_KEY, request.getContentType() + "_" + request.getContentId(), LEARNING_RECORD_KEY_EXPIRE_TIME)) {
            throw new RuntimeException("正在记录学习进度中～～");
        }

        try {
            // 调用服务记录学习进度
            RecordLearningProgressResponse response = learningProgressService.recordProgress(
                    userId, request);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("status", "completed".equals(response.getStatus()) ? 1 : 0);
            data.put("progress", response.getProgress());
            data.put("isCompleted", "completed".equals(response.getStatus()));

            return ApiResponse.success("记录成功", data);
        } catch (Exception e) {
            log.error("记录学习进度失败", e);
            return ApiResponse.error(500, "记录学习进度失败: " + e.getMessage());
        } finally {
            lock.unlock(LEARNING_RECORD_KEY, request.getContentType() + "_" + request.getContentId());
        }
    }
}
