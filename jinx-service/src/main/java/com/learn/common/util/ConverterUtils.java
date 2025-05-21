package com.learn.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConverterUtils {

    /**
     * 解析目标ID字符串
     *
     * @param targetIdsStr 目标ID字符串，格式为 [1,2,3,4]
     * @return 目标ID列表
     */
    public static List<Long> parseTargetIds(String targetIdsStr) {
        List<Long> targetIds = new ArrayList<>();

        if (targetIdsStr != null && targetIdsStr.length() > 2) {
            // 去掉方括号，并按逗号分割
            String[] idsStr = targetIdsStr.substring(1, targetIdsStr.length() - 1).split(",");
            for (String idStr : idsStr) {
                try {
                    targetIds.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    // 忽略无法解析的ID
                    log.warn("无法解析的ID: {}", idStr);
                }
            }
        }
        return targetIds;
    }
}
