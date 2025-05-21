package com.learn.dto.toc.personal;

import lombok.Data;

/**
 * @author yujintao
 * @date 2025/5/13
 */
@Data
public class UserLearningTotalResponse {

    private Long allTotal;

    private Long courseTotal;

    private Long mapTotal;

    private Long trainTotal;
}
