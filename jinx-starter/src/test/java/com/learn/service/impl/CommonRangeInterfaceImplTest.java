package com.learn.service.impl;

import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.infrastructure.repository.mapper.CommonRangeMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.*;
import com.learn.StarterApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = StarterApplication.class)
//@Transactional
public class CommonRangeInterfaceImplTest {

    @Autowired
    private CommonRangeMapper commonRangeMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    private CommonRangeCreateRequest createRequest;
    private CommonRangeQueryRequest queryRequest;

    @BeforeEach
    public void setUp() {
        // 初始化创建请求对象
        createRequest = new CommonRangeCreateRequest();
        createRequest.setModelType("visibility");
        createRequest.setType(BizType.COURSE);
        createRequest.setTypeId(123L);
        
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        targetTypeAndIds.put("department", Arrays.asList(1L, 2L));
        targetTypeAndIds.put("user", Arrays.asList(100L, 101L));
        createRequest.setTargetTypeAndIds(targetTypeAndIds);
        
        // 初始化查询请求对象
        queryRequest = new CommonRangeQueryRequest();
        queryRequest.setModelType("visibility");
        queryRequest.setType(BizType.COURSE);
        queryRequest.setTypeId(123L);
    }

    @Test
    public void testBatchCreateRange() {
        // 执行测试 - 实际插入数据库
        CommonRangeCreateResponse response = commonRangeInterface.batchCreateRange(createRequest);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertFalse(response.getRangeIds().isEmpty());
        
        // 从数据库中验证数据是否已成功插入
        List<CommonRange> insertedRanges = commonRangeMapper.queryRangeByBusinessId(
                createRequest.getModelType(), createRequest.getType(), createRequest.getTypeId());
        assertFalse(insertedRanges.isEmpty());
        assertEquals(2, insertedRanges.size());
    }

    @Test
    public void testBatchCreateRangeWithEmptyTargets() {
        // 使用空的目标类型和ID映射
        createRequest.setTargetTypeAndIds(new HashMap<>());
        
        // 执行测试
        CommonRangeCreateResponse response = commonRangeInterface.batchCreateRange(createRequest);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertTrue(response.getRangeIds().isEmpty());
    }

    @Test
    public void testQueryBusinessIdsByTargets() {
        // 首先插入测试数据
        commonRangeInterface.batchCreateRange(createRequest);
        
        // 设置查询参数
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        targetTypeAndIds.put("department", Arrays.asList(1L, 2L));
        queryRequest.setTargetTypeAndIds(targetTypeAndIds);
        
        // 执行测试
        List<CommonRangeQueryResponse> responseList = commonRangeInterface.queryBusinessIdsByTargets(queryRequest);
        
        // 验证结果
        assertNotNull(responseList);
        assertFalse(responseList.isEmpty());
        assertEquals(1, responseList.size());
        assertEquals(123L, responseList.get(0).getTypeId());
    }

    @Test
    public void testQueryRangeConfigByBusinessId() {
        // 首先插入测试数据
        commonRangeInterface.batchCreateRange(createRequest);
        
        // 执行测试
        CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(queryRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(123L, response.getTypeId());
        assertNotNull(response.getDepartmentIds());
        assertNotNull(response.getUserIds());
        assertTrue(response.getSuccess());
        
        // 验证部门IDs
        assertEquals(2, response.getDepartmentIds().size());
        assertTrue(response.getDepartmentIds().contains(1L));
        assertTrue(response.getDepartmentIds().contains(2L));
        
        // 验证用户IDs
        assertEquals(2, response.getUserIds().size());
        assertTrue(response.getUserIds().contains(100L));
        assertTrue(response.getUserIds().contains(101L));
    }

    @Test
    public void testDeleteRangeByBusinessId() {
        // 首先插入测试数据
        commonRangeInterface.batchCreateRange(createRequest);
        
        // 创建删除请求
        CommonRangeDeleteRequest deleteRequest = new CommonRangeDeleteRequest();
        deleteRequest.setModelType("visibility");
        deleteRequest.setType(BizType.COURSE);
        deleteRequest.setTypeId(123L);
        
        // 执行测试
        Boolean result = commonRangeInterface.deleteRangeByBusinessId(deleteRequest);
        
        // 验证结果
        assertTrue(result);
        
        // 验证数据已被删除
        List<CommonRange> ranges = commonRangeMapper.queryRangeByBusinessId(
                deleteRequest.getModelType(), deleteRequest.getType(), deleteRequest.getTypeId());
        assertTrue(ranges.isEmpty());
    }
    
    @Test
    public void testQueryPreloadedData() {
        // 测试预加载的数据
        
        // 查询课程可见范围
        CommonRangeQueryRequest visibilityRequest = new CommonRangeQueryRequest();
        visibilityRequest.setModelType("visibility");
        visibilityRequest.setType(BizType.COURSE);
        visibilityRequest.setTypeId(100L);
        
        CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(visibilityRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(100L, response.getTypeId());
        assertEquals(3, response.getDepartmentIds().size());
        assertTrue(response.getDepartmentIds().contains(10L));
        assertTrue(response.getDepartmentIds().contains(11L));
        assertTrue(response.getDepartmentIds().contains(12L));
        
        // 查询培训可见范围
        visibilityRequest.setType(BizType.TRAIN);
        visibilityRequest.setTypeId(200L);
        
        response = commonRangeInterface.queryRangeConfigByBusinessId(visibilityRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(200L, response.getTypeId());
        assertEquals(2, response.getRoleIds().size());
        assertTrue(response.getRoleIds().contains(5L));
        assertTrue(response.getRoleIds().contains(6L));
    }
    
    @Test
    public void testQueryByMultipleTargetTypes() {
        // 设置查询参数 - 包含多种目标类型
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        targetTypeAndIds.put("department", Arrays.asList(10L, 11L));
        targetTypeAndIds.put("role", Arrays.asList(5L, 6L));
        
        CommonRangeQueryRequest multiRequest = new CommonRangeQueryRequest();
        multiRequest.setModelType("visibility");
        multiRequest.setType(BizType.COURSE);
        multiRequest.setTargetTypeAndIds(targetTypeAndIds);
        
        // 执行测试
        List<CommonRangeQueryResponse> responseList = commonRangeInterface.queryBusinessIdsByTargets(multiRequest);
        
        // 验证结果
        assertNotNull(responseList);
        assertFalse(responseList.isEmpty());
        
        // 应该包含课程ID 100 (department类型匹配) 和 200 (role类型匹配)
        boolean foundCourse100 = false;
        boolean foundTrain200 = false;
        
        for (CommonRangeQueryResponse res : responseList) {
            if (res.getTypeId().equals(100L)) {
                foundCourse100 = true;
            } else if (res.getTypeId().equals(200L)) {
                foundTrain200 = true;
            }
        }
        
        assertTrue(foundCourse100 || foundTrain200);
    }
} 
