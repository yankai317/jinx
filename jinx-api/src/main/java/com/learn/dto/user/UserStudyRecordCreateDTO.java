package com.learn.dto.user;

import com.learn.constants.BizType;
import lombok.Data;

/**
 * @author yujintao
 * @date 2025/5/6
 */
@Data
public class UserStudyRecordCreateDTO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * @see BizType
     */
    private String bizType;

    /**
     * 例如课程id，培训id，学习地图id
     */
    private String bizId;

    /**
     * 学习来源：ASSIGN-指派 SELF-自学
     */
    private String source;
}
