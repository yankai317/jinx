package com.learn.service.third.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.infrastructure.repository.entity.ThirdCropInfo;
import com.learn.infrastructure.repository.mapper.ThirdCropInfoMapper;
import com.learn.service.third.ThirdCropInfoService;
import com.learn.service.third.bo.ThirdCropInfoBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import shade.com.alibaba.fastjson2.JSONObject;
import shade.com.alibaba.fastjson2.TypeReference;

/**
 * 三方企业信息服务实现类
 */
@Service
@Slf4j
public class ThirdCropInfoServiceImpl implements ThirdCropInfoService {

    @Autowired
    private ThirdCropInfoMapper thirdCropInfoMapper;
    

    @Override
    public ThirdCropInfoBO queryByThirdId(String thirdId) {
        // 参数校验
        if (thirdId == null || thirdId.isEmpty()) {
            throw new IllegalArgumentException("查询三方企业信息失败，thirdId为空");
        }
        
        // 构建查询条件
        LambdaQueryWrapper<ThirdCropInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ThirdCropInfo::getThirdId, thirdId)
                   .eq(ThirdCropInfo::getIsDel, 0); // 只查询未删除的记录
        
        // 查询数据库
        ThirdCropInfo thirdCropInfo = thirdCropInfoMapper.selectOne(queryWrapper);
        if (thirdCropInfo == null) {
            log.info("未查询到三方企业信息，thirdId={}", thirdId);
            return null;
        }
        
        // 转换为业务对象
        return convertToBO(thirdCropInfo);
    }
    
    /**
     * 将实体对象转换为业务对象
     *
     * @param entity 实体对象
     * @return 业务对象
     */
    private ThirdCropInfoBO convertToBO(ThirdCropInfo entity) {
        if (entity == null) {
            return null;
        }
        
        ThirdCropInfoBO bo = new ThirdCropInfoBO();
        // 复制基本属性
        BeanUtils.copyProperties(entity, bo);
        
        // 解析attributes字段
        ThirdCropInfoBO.ThirdCropAttributes attributes = JSONObject.parseObject(
                entity.getAttributes(),
                new TypeReference<>() {
                });
        bo.setAttributes(attributes);
        return bo;
    }
}
