package com.learn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author yujintao
 * @date 2025/5/12
 */
@Data
@AllArgsConstructor
@Builder
public class ContentBO {

    private Long id;

    private String title;

    private String coverImg;

    private String description;

    private Integer credit;


    private String type;

    /**
     * document...
     */
    private String contentType;

}
