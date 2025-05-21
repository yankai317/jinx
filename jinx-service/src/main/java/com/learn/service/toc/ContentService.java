package com.learn.service.toc;

import com.learn.common.dto.ContentBO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * @author yujintao
 * @date 2025/5/14
 */
public interface ContentService {

    Map<String, ContentBO> getContentMap(List<Pair<String, Long>> contentSearchKeys);

    ContentBO getContent(String contentType, Long contentId);
}
