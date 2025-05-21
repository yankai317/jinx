package com.learn.service.auth.strategy.impl;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserGetRequest;
import com.dingtalk.api.request.OapiUserGetuserinfoRequest;
import com.dingtalk.api.response.OapiUserGetResponse;
import com.dingtalk.api.response.OapiUserGetuserinfoResponse;
import com.learn.service.auth.strategy.ThirdPartyLoginStrategy;
import com.learn.service.dingtalk.common.DingTalkApiClient;
import com.learn.service.dto.ThirdPartyUserInfo;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import shade.com.alibaba.fastjson2.JSONObject;

/**
 * 钉钉登录策略实现
 * 实现通过钉钉临时授权码获取用户信息的流程
 */
@Component
@Slf4j
public class DingTalkLoginStrategy implements ThirdPartyLoginStrategy {

    
    @Autowired
    private DingTalkApiClient dingTalkApiClient;
    
    private static final String THIRD_PARTY_TYPE = "dingtalk";
    
    // API URL常量
    private static final String API_GET_USERID_BY_CODE = "https://oapi.dingtalk.com/topapi/v2/user/getuserinfo";
    private static final String API_GET_USER_DETAIL = "https://oapi.dingtalk.com/topapi/v2/user/get";

    @Override
    public String getThirdPartyType() {
        return THIRD_PARTY_TYPE;
    }

    @Override
    public ThirdPartyUserInfo getUserInfoByCode(String code, String corpId) {
        try {
            // 1. 通过授权码直接获取userId
            String accessToken = dingTalkApiClient.getAccessToken(corpId, 0);
            String userId = getUserIdByCode(code, accessToken);

            // 2. 获取用户详细信息
            OapiUserGetResponse userDetail = getUserDetail(userId, accessToken);
            if (!userDetail.isSuccess()) {
                log.error("获取钉钉用户详细信息失败: {}", userDetail.getErrmsg());
                throw new RuntimeException("Failed to get user detail from DingTalk");
            }

            // 3. 构建返回结果
            ThirdPartyUserInfo userInfo = new ThirdPartyUserInfo();
            userInfo.setThirdPartyType(THIRD_PARTY_TYPE);
            userInfo.setThirdPartyUserId(userId);
            userInfo.setThirdPartyUsername(userDetail.getName());
            userInfo.setAvatar(userDetail.getAvatar());
            userInfo.setEmail(userDetail.getEmail());
            userInfo.setMobile(userDetail.getMobile());

            return userInfo;
        } catch (ApiException e) {
            log.error("获取钉钉用户信息失败: {}", e.getMessage());
            throw new RuntimeException("Failed to get user info from DingTalk", e);
        }
    }
    
    /**
     * 通过临时授权码获取用户ID
     * @param code 临时授权码
     * @param accessToken 访问令牌
     * @return 用户ID
     */
    private String getUserIdByCode(String code, String accessToken) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient(API_GET_USERID_BY_CODE);
        OapiUserGetuserinfoRequest req = new OapiUserGetuserinfoRequest();
        req.setCode(code);
        req.setHttpMethod("GET");
        OapiUserGetuserinfoResponse response = client.execute(req, accessToken);

        if (!response.isSuccess()
                || StringUtils.isEmpty(response.getBody())) {
            log.error("通过授权码获取userId失败: {}", response.getErrmsg());
            throw new RuntimeException("Failed to get userId from DingTalk");
        }
        JSONObject object = JSONObject.parseObject(response.getBody());
        OapiUserGetuserinfoResponse userInfo = JSONObject.parseObject(
                object.getString("result"),
                OapiUserGetuserinfoResponse.class);
        return userInfo.getUserid();
    }
    
    /**
     * 获取用户详细信息
     * @param userId 用户ID
     * @param accessToken 访问令牌
     * @return 用户详细信息
     */
    private OapiUserGetResponse getUserDetail(String userId, String accessToken) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient(API_GET_USER_DETAIL);
        OapiUserGetRequest req = new OapiUserGetRequest();
        req.setUserid(userId);
        req.setHttpMethod("GET");
        return client.execute(req, accessToken);
    }
} 
