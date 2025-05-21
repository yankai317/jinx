package com.learn.dto.toc.home;

import lombok.Data;

import java.util.List;

/**
 * 排行榜DTO
 */
@Data
public class RankingsDTO {
    /**
     * 学习时长排行榜
     */
    private List<RankingItemDTO> studyDurationRanking;
    
    /**
     * 课程完成数排行榜
     */
    private List<RankingItemDTO> courseCompletionRanking;
    
    /**
     * 获得证书数排行榜
     */
    private List<RankingItemDTO> certificateRanking;
}
