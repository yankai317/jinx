package com.learn.service.impl;

import com.dingtalk.open.app.api.GenericEventListener;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import com.google.common.collect.Lists;
import com.learn.StarterApplication;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.service.dingtalk.common.DingTalkApiClient;
import com.learn.service.dingtalk.impl.DingTalkDepartmentSyncServiceImpl;
import com.learn.service.dingtalk.impl.DingTalkRoleSyncServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import shade.com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = StarterApplication.class)
class DingTalkDepartmentSyncServiceImplTest {

    @Autowired
    private DingTalkDepartmentSyncServiceImpl dingTalkSyncService;

    @Autowired
    private DingTalkRoleSyncServiceImpl dingTalkRoleSyncService;

    @Resource
    private DingTalkApiClient dingTalkApiClient;
    @Resource
    private DepartmentMapper departmentMapper;

    @Test
    void testBatchInsert() {
        List<Department> toInsertList = new ArrayList<>();
        toInsertList.add(new Department()
        .setDepartmentName("不隐测试部门")
                .setParentId(1L)
                .setDepartmentLevel(1)
                .setSortOrder(1)
                .setExternalId("12344441")
                .setExternalSource("dingtalk")
                .setIsDel(0));

        toInsertList.add(new Department()
                .setDepartmentName("不隐测试部门2")
                .setParentId(1L)
                .setDepartmentLevel(1)
                .setSortOrder(1)
                .setExternalId("1111111")
                .setExternalSource("dingtalk")
                .setIsDel(0));
        int i = departmentMapper.batchInsert(toInsertList);
        System.out.println(JSONObject.toJSONString(toInsertList));
    }


    @Test
    void syncDepartments() {
        dingTalkSyncService.syncDepartments();
    }

    @Test
    void syncDepartmentUsers() {
        dingTalkSyncService.syncDepartmentUsers();
    }

    @Test
    void syncRoles() {
        dingTalkRoleSyncService.syncRoles();
    }

    @Test
    void syncRoleUsers() {
        dingTalkRoleSyncService.syncRoleUsers();
    }

//    @Test
//    void deliverSingleChatCard() {
//        Map<String, String> cardData = new HashMap<>();
//        cardData.put("title", "不隐测试卡片");
//        cardData.put("url", "https://middleware-file-center.oss-cn-hangzhou.aliyuncs.com/1718704367532_Textured%20product.png");
//
//        dingTalkApiClient.deliverSingleChatCard("fb903641-59c6-43d8-95e5-a74102f09949.schema", cardData, Lists.newArrayList("0M9v8XYjiPl6EHiPJFXagcNwiEiE"),"ding5c0021bff7799aad35c2f4657eb6378f");
//    }

}
