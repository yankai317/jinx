package com.learn.service.dingtalk.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import com.learn.infrastructure.repository.entity.ThirdCropInfo;
import com.learn.infrastructure.repository.mapper.ThirdCropInfoMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import shade.com.alibaba.fastjson2.JSONObject;

import java.util.Date;

/**
 * 钉钉消息客户端,
 * 围绕消息钉钉 stream处理 逻辑
 */
@Component
@Slf4j
public class DingTalkStreamApiClient implements ApplicationRunner {

    @Value("${dingtalk.app.appkey}")
    @Getter
    private String suiteKey;

    @Value("${dingtalk.app.appsecret}")
    @Getter
    private String suiteSecret;

    @Autowired
    private ThirdCropInfoMapper thirdCropInfoMapper;

    /**
     * 应用启动时自动执行，初始化钉钉消息流监听
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("初始化钉钉消息流监听...");
        start();
        log.info("钉钉消息流监听初始化完成");
    }

    /**
     * 启动钉钉消息流监听
     */
    public void start() throws Exception {
        OpenDingTalkStreamClientBuilder
                .custom()
                .credential(new AuthClientCredential(suiteKey, suiteSecret))
                //注册事件监听
                .registerAllEventListener(event -> {
                    //仅处理
                    try {
                        //事件的唯一Id
                        String eventId = event.getEventId();
                        //事件类型
                        String eventType = event.getEventType();
                        //事件产生时间
                        Long bornTime = event.getEventBornTime();
                        //处理事件
                        log.info("收到钉钉事件: eventId={}, eventType={}, bornTime={}, data={}",
                                eventId, eventType, bornTime, JSONObject.toJSONString(event));
                        // 根据事件类型进行处理
                        if ("suite_ticket".equals(eventType)) {
                            processSuiteTicket(event);
                        }
                        //消费成功
                        return EventAckStatus.SUCCESS;
                    } catch (Exception e) {
                        //消费失败
                        log.error("处理钉钉事件异常", e);
                        return EventAckStatus.LATER;
                    }
                })
                .build().start();
    }

    /**
     * 处理 SuiteTicket 事件，并且存储到数据库中
     *
     * @param event 钉钉事件
     */
    private void processSuiteTicket(GenericOpenDingTalkEvent event) {
        try {
            JSONObject data = event.getData();
            if (data == null) {
                log.error("SuiteTicket事件数据为空");
                return;
            }

            // 获取事件中的关键数据
            String suiteTicket = data.getString("suiteTicket");
            if (suiteTicket == null || suiteTicket.isEmpty()) {
                log.error("SuiteTicket数据为空");
                return;
            }
            

            // 从事件数据中尝试获取 authCorpId，如果不存在则使用默认值
            String corpId = event.getEventCorpId();

            log.info("处理SuiteTicket事件: corpId={}, suiteTicket={}", corpId, suiteTicket);
            if (corpId == null || corpId.isEmpty()) {
                log.warn("企业ID为空，无法处理SuiteTicket事件");
                return;
            }

            // 查询数据库中是否已存在该企业信息
            LambdaQueryWrapper<ThirdCropInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ThirdCropInfo::getThirdId, corpId)
                    .eq(ThirdCropInfo::getThirdType, "dingtalk")
                    .eq(ThirdCropInfo::getIsDel, 0);

            ThirdCropInfo thirdCropInfo = thirdCropInfoMapper.selectOne(queryWrapper);

            Date now = new Date();// 如果不存在，则创建新记录
            if (thirdCropInfo == null) {
                thirdCropInfo = new ThirdCropInfo();
                thirdCropInfo.setThirdId(corpId);
                thirdCropInfo.setThirdType("dingtalk");
                thirdCropInfo.setGmtCreate(now);
                thirdCropInfo.setCreatorId(0L); // 系统创建
                thirdCropInfo.setCreatorName("system");
                thirdCropInfo.setUpdaterId(0L);
                thirdCropInfo.setUpdaterName("system");
                thirdCropInfo.setGmtModified(now);
                thirdCropInfo.setIsDel(0);
                // 构建扩展属性JSON
                JSONObject attributes = new JSONObject();
                attributes.put("suiteTicket", suiteTicket);
                thirdCropInfo.setAttributes(attributes.toString());

                thirdCropInfoMapper.insert(thirdCropInfo);
                log.info("新增企业信息: corpId={}", corpId);
            }
            // 如果存在，则更新记录
            else {
                // 使用 JSON_SET 更新 suiteTicket 属性，不影响其他属性
                thirdCropInfoMapper.updateJsonProperty(
                    thirdCropInfo.getId(),
                    "$.suiteTicket",
                    suiteTicket,
                    0L,
                    "system"
                );
                log.info("更新企业信息: corpId={}", corpId);
            }
        } catch (Exception e) {
            log.error("处理SuiteTicket事件异常", e);
        }
    }
}
