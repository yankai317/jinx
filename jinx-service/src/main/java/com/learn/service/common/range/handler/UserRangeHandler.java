package com.learn.service.common.range.handler;

import com.learn.dto.common.RangeQueryResponse;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.dto.UserQueryRequest;
import com.learn.service.dto.UserQueryResponse;
import com.learn.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户范围处理器
 * 责任链模式中处理用户信息的具体处理器
 */
@Component
@Slf4j
public class UserRangeHandler extends RangeHandler {
    
    @Autowired
    private UserService userService;
    
    @Override
    public Object handle(CommonRangeQueryResponse response) {
        log.info("处理用户范围信息");
        
        // 获取当前处理结果，如果为空则创建新的响应对象
        RangeQueryResponse result = (RangeQueryResponse) (nextHandler != null ? nextHandler.handle(response) : new RangeQueryResponse());
        
        // 处理用户信息
        List<RangeQueryResponse.RangeUser> userInfos = new ArrayList<>();
        
        // 如果有用户ID，则查询用户详细信息
        if (!CollectionUtils.isEmpty(response.getUserIds())) {
            // 构建用户查询请求
            UserQueryRequest userQueryRequest = new UserQueryRequest();
            userQueryRequest.setUserIds(response.getUserIds());

            // 查询用户信息
            List<UserQueryResponse> userResponses = userService.queryUsers(userQueryRequest);

            // 转换为RangeUser对象
            if (!CollectionUtils.isEmpty(userResponses)) {
                userInfos = userResponses.stream()
                        .map(user -> {
                            RangeQueryResponse.RangeUser rangeUser = new RangeQueryResponse.RangeUser();
                            rangeUser.setUserId(user.getUserId());
                            rangeUser.setUserName(user.getNickname());
                            rangeUser.setAvatar(user.getAvatar());
                            return rangeUser;
                        })
                        .collect(Collectors.toList());
            }
        }
        
        // 设置用户信息
        result.setUserInfos(userInfos);
        
        return result;
    }
}
