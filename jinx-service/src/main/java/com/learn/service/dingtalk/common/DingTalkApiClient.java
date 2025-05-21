package com.learn.service.dingtalk.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.mos.boot.kv.ability.MosBootKVAbility;
import com.alibaba.mos.boot.kv.ability.data.MosBootKV;
import com.alibaba.mos.boot.kv.ability.data.MosBootKVResult;
import com.alibaba.mos.boot.util.MosBootSpiLoader;
import com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverRequest;
import com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverResponse;
import com.aliyun.dingtalkoauth2_1_0.models.GetCorpAccessTokenResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.learn.infrastructure.repository.entity.ThirdCropInfo;
import com.learn.infrastructure.repository.mapper.ThirdCropInfoMapper;
import com.learn.service.third.ThirdCropInfoService;
import com.learn.service.third.bo.ThirdCropInfoBO;
import com.taobao.api.ApiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉API客户端，封装基础API调用
 * 每个方法只负责单个API调用，不包含业务逻辑
 */
@Component
@Slf4j
public class DingTalkApiClient {

    @Value("${dingtalk.app.appkey}")
    @Getter
    private String suiteKey;

    @Value("${dingtalk.app.appsecret}")
    @Getter
    private String suiteSecret;

    @Value("${dingtalk.app.authCorpId}")
    private String authCorpId;
    @Autowired
    @Lazy
    private DingTalkApiClient dingTalkApiClient;

    @Autowired
    private ThirdCropInfoMapper thirdCropInfoMapper;

    @Autowired
    private ThirdCropInfoService thirdCropInfoService;
    private MosBootKVAbility mosBootKVAbility = MosBootSpiLoader.getSpiService(MosBootKVAbility.class);


    // 钉钉API地址常量
    private static final String API_DEPARTMENT_LIST = "https://oapi.dingtalk.com/topapi/v2/department/listsub";
    private static final String API_USER_LIST = "https://oapi.dingtalk.com/topapi/v2/user/list";
    private static final String API_ROLE_LIST = "https://oapi.dingtalk.com/topapi/role/list";
    private static final String API_ROLE_USERLIST = "https://oapi.dingtalk.com/topapi/role/simplelist";
    private static final String DINGTALK_ACCESS_TOKEN_CACHE_KEY = "dingtalk:key:%s";


    /**
     * 获取授权企业的访问令牌
     * 适用于钉钉ISV应用获取授权企业的access_token
     *
     * @param corpId 企业ID
     * @return 访问令牌
     */
    public String getAccessToken(String corpId, Integer retry) {
        if (Objects.isNull(retry) || retry > 3) {
            throw new RuntimeException("重试次数超过限制");
        }
        if (corpId == null || corpId.isEmpty()) {
            log.error("企业ID为空，无法获取访问令牌");
            throw new RuntimeException("企业ID为空，无法获取访问令牌");
        }

        // 检查本地缓存
        MosBootKVResult<String> cachedDingTalkCache = mosBootKVAbility.get(MosBootKV.of(String.format(DINGTALK_ACCESS_TOKEN_CACHE_KEY, corpId)), String.class);
        if (cachedDingTalkCache != null
                && StringUtils.isNotEmpty(cachedDingTalkCache.getValue())) {
            log.info("从本地缓存获取企业访问令牌: corpId={}", corpId);
            return cachedDingTalkCache.getValue();
        }

        try {
            // 从数据库获取企业信息
            LambdaQueryWrapper<ThirdCropInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ThirdCropInfo::getThirdId, corpId)
                    .eq(ThirdCropInfo::getThirdType, "dingtalk")
                    .eq(ThirdCropInfo::getIsDel, 0);

            ThirdCropInfo thirdCropInfo = thirdCropInfoMapper.selectOne(queryWrapper);

            if (thirdCropInfo == null) {
                log.error("未找到企业信息: corpId={}", corpId);
                throw new RuntimeException("企业ID为空，无法获取访问令牌");
            }

            // 从attributes字段获取suiteTicket
            String suiteTicket = null;
            JSONObject attributes = com.alibaba.fastjson.JSONObject.parseObject(thirdCropInfo.getAttributes());
            if (attributes != null) {
                suiteTicket = attributes.getString("suiteTicket");
            }

            if (suiteTicket == null || suiteTicket.isEmpty()) {
                log.error("企业suiteTicket为空: corpId={}", corpId);
                throw new RuntimeException("企业ID为空，无法获取访问令牌");
            }

            // 调用钉钉API获取token
            com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
            config.protocol = "https";
            config.regionId = "central";
            com.aliyun.dingtalkoauth2_1_0.Client client = new com.aliyun.dingtalkoauth2_1_0.Client(config);
            com.aliyun.dingtalkoauth2_1_0.models.GetCorpAccessTokenRequest getCorpAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetCorpAccessTokenRequest()
                    .setSuiteKey(suiteKey)
                    .setSuiteSecret(suiteSecret)
                    .setAuthCorpId(corpId)
                    .setSuiteTicket(suiteTicket);
            GetCorpAccessTokenResponse corpAccessToken = client.getCorpAccessToken(getCorpAccessTokenRequest);

            log.info("成功获取企业访问令牌: corpId={}", corpId);

            // 设置本地缓存，过期时间为返回的过期时间减去10分钟
            String accessToken = corpAccessToken.getBody().accessToken;
            long expiresIn = corpAccessToken.getBody().expireIn;
            // 过期时间减去10分钟(600秒)
            MosBootKV mosBootKV = MosBootKV.of(String.format(DINGTALK_ACCESS_TOKEN_CACHE_KEY, corpId));
            mosBootKV.setValue(accessToken);
            mosBootKV.setExpire(Integer.parseInt(expiresIn + "") - 600);
            mosBootKVAbility.put(mosBootKV);
            return accessToken;
        } catch (Exception e) {
            log.error("获取企业访问令牌异常: corpId={}", corpId, e);
            return getAccessToken(corpId, retry+1); // 添加返回语句
        }
    }

    /**
     * 兼容旧代码的访问令牌获取方法
     *
     * @return 默认企业的访问令牌
     */
    public String getAccessToken() {
        return dingTalkApiClient.getAccessToken(authCorpId, 0);
    }

    /**
     * 获取部门列表
     *
     * @param deptId 父部门ID，1表示根部门
     * @return 部门列表响应
     */
    public OapiV2DepartmentListsubResponse listDepartments(Long deptId) {
        String accessToken = getAccessToken();

        try {
            DingTalkClient client = new DefaultDingTalkClient(API_DEPARTMENT_LIST);
            OapiV2DepartmentListsubRequest req = new OapiV2DepartmentListsubRequest();
            req.setDeptId(deptId);
            req.setLanguage("zh_CN");
            return client.execute(req, accessToken);
        } catch (ApiException e) {
            log.error("获取部门列表异常, deptId={}", deptId, e);
            throw new RuntimeException("获取部门列表异常: deptId=" + deptId, e);
        }
    }

    /**
     * 获取部门用户列表
     *
     * @param deptId 部门ID
     * @param cursor 分页游标
     * @param size   分页大小
     * @return 用户列表响应
     */
    public OapiV2UserListResponse listDepartmentUsers(Long deptId, Long cursor, Long size) {
        String accessToken = getAccessToken();

        try {
            DingTalkClient client = new DefaultDingTalkClient(API_USER_LIST);
            OapiV2UserListRequest req = new OapiV2UserListRequest();
            req.setDeptId(deptId);
            req.setCursor(cursor);
            req.setSize(size);
            req.setOrderField("entry_asc");
            req.setContainAccessLimit(false);
            req.setLanguage("zh_CN");
            OapiV2UserListResponse response = client.execute(req, accessToken);

            if (Objects.nonNull(response) && Objects.equals(response.getErrcode(), 60003L)) {
                return null;
            }
            if (response == null || !response.isSuccess()) {
                log.error("获取钉钉部门用户列表失败, deptId={}", deptId);
                throw new RuntimeException("获取钉钉部门用户列表失败: deptId=" + deptId);
            }
            return response;
        } catch (ApiException e) {
            log.error("获取部门用户列表异常, deptId={}", deptId, e);
            throw new RuntimeException("获取部门用户列表异常: deptId=" + deptId, e);
        }
    }

    /**
     * 获取角色列表
     *
     * @param offset 分页偏移
     * @param size   分页大小
     * @return 角色列表响应
     */
    public OapiRoleListResponse listRoles(Long offset, Long size) {
        String accessToken = getAccessToken();

        try {
            DingTalkClient client = new DefaultDingTalkClient(API_ROLE_LIST);
            OapiRoleListRequest req = new OapiRoleListRequest();
            req.setSize(size);
            req.setOffset(offset);
            OapiRoleListResponse response = client.execute(req, accessToken);

            if (Objects.nonNull(response) && Objects.equals(response.getErrcode(), 60003L)) {
                return null;
            }
            if (response == null || !response.isSuccess()) {

                log.error("获取钉钉角色列表失败");
                throw new RuntimeException("获取钉钉角色列表失败");
            }
            return response;
        } catch (ApiException e) {
            log.error("获取角色列表异常", e);
            throw new RuntimeException("获取角色列表异常", e);
        }
    }

    /**
     * 获取角色用户列表
     *
     * @param roleId 角色ID
     * @param offset 分页偏移
     * @param size   分页大小
     * @return 角色用户列表响应
     */
    public OapiRoleSimplelistResponse listRoleUsers(Long roleId, Long offset, Long size) {
        String accessToken = getAccessToken();

        try {
            DingTalkClient client = new DefaultDingTalkClient(API_ROLE_USERLIST);
            OapiRoleSimplelistRequest req = new OapiRoleSimplelistRequest();
            req.setRoleId(roleId);
            req.setSize(size);
            req.setOffset(offset);
            return client.execute(req, accessToken);
        } catch (ApiException e) {
            log.error("获取角色用户列表异常, roleId={}", roleId, e);
            throw new RuntimeException("获取角色用户列表异常: roleId=" + roleId, e);
        }
    }


    /**
     * 发送钉钉卡片
     *
     * @param cardTemplateId 卡片模板ID
     * @param cardData       卡片数据
     * @param openSpaceId    开放空间ID
     * @param spaceType      空间类型
     * @param authCorpId     授权企业的CorpId，ISV应用需要提供
     * @return 是否发送成功
     */
    public boolean deliverCard(String cardTemplateId, Map<String, String> cardData,
                               String openSpaceId, String spaceType, String authCorpId, String robotCode) {
        try {
            com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
            config.protocol = "https";
            config.regionId = "central";
            com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverHeaders createAndDeliverHeaders = new com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverHeaders();
            createAndDeliverHeaders.xAcsDingtalkAccessToken = getAccessToken(authCorpId, 0);
            com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverRequest createAndDeliverRequest = new com.aliyun.dingtalkcard_1_0.models.CreateAndDeliverRequest();
            createAndDeliverRequest.setCardTemplateId(cardTemplateId);
            createAndDeliverRequest.setOpenSpaceId(openSpaceId);
            createAndDeliverRequest.setOutTrackId(UUID.randomUUID().toString());
//            createAndDeliverRequest.setCallbackType("STREAM");
            CreateAndDeliverRequest.CreateAndDeliverRequestCardData createAndDeliverRequestCardData = new CreateAndDeliverRequest.CreateAndDeliverRequestCardData();
            createAndDeliverRequestCardData.setCardParamMap(cardData);
            createAndDeliverRequest.setCardData(createAndDeliverRequestCardData);
            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel createAndDeliverRequestImRobotOpenDeliverModel = new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel();
            createAndDeliverRequestImRobotOpenDeliverModel.setSpaceType(spaceType);
            createAndDeliverRequestImRobotOpenDeliverModel.setRobotCode(robotCode);
            createAndDeliverRequest.setImRobotOpenDeliverModel(createAndDeliverRequestImRobotOpenDeliverModel);
            createAndDeliverRequest.setUserIdType(1);
            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel createAndDeliverRequestImRobotOpenSpaceModel = new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel();
            createAndDeliverRequestImRobotOpenSpaceModel.setSupportForward(true);
            createAndDeliverRequest.setImRobotOpenSpaceModel(createAndDeliverRequestImRobotOpenSpaceModel);
            log.info("卡片推送请求参数，info: {}", JSON.toJSONString(createAndDeliverRequest));
            com.aliyun.dingtalkcard_1_0.Client client = new com.aliyun.dingtalkcard_1_0.Client(config);
            CreateAndDeliverResponse andDeliverWithOptions = client.createAndDeliverWithOptions(createAndDeliverRequest, createAndDeliverHeaders, new RuntimeOptions());
            log.info("卡片推送成功，响应: {}", JSON.toJSONString(andDeliverWithOptions.getBody()));
            return true;
        } catch (Exception e) {
            log.error("卡片推送异常", e);
            return false;
        }
    }

    /**
     * 发送单聊卡片
     *
     * @param cardData   卡片数据
     * @param authCorpId 授权企业的CorpId，ISV应用需要提供
     * @return 是否发送成功
     */
    public boolean deliverSingleChatCard(Map<String, String> cardData,
                                         List<String> dingTalkUserIds, String authCorpId) {
        if (CollectionUtils.isEmpty(dingTalkUserIds)) {
            log.warn("钉钉用户ID列表为空，无法发送卡片");
            return false;
        }

        ThirdCropInfoBO thirdCropInfoBO = thirdCropInfoService.queryByThirdId(authCorpId);
        if (Objects.isNull(thirdCropInfoBO) || Objects.isNull(thirdCropInfoBO.getAttributes())) {
            throw new RuntimeException("企业信息不存在");
        }

        // 拼接多个钉钉用户ID，格式为：dtv1.card//IM_ROBOT.userId1;IM_ROBOT.userId2;
        StringBuilder openSpaceIdBuilder = new StringBuilder("dtv1.card//");
        for (String userId : dingTalkUserIds) {
            openSpaceIdBuilder.append("IM_ROBOT.").append(userId).append(";");
        }

        String openSpaceId = openSpaceIdBuilder.toString();
        log.info("发送钉钉卡片，接收用户：{}", openSpaceId);

        return deliverCard(thirdCropInfoBO.getAttributes().getCardTemplateId(),
                cardData,
                openSpaceId,
                "IM_ROBOT",
                authCorpId,
                thirdCropInfoBO.getAttributes().getRobotCode());
    }


}
