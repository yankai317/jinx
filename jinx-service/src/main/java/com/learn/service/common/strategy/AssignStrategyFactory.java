package com.learn.service.common.strategy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指派策略工厂
 * 根据业务类型返回对应的指派策略
 */
@Component
public class AssignStrategyFactory {
    
    @Autowired
    private List<AssignStrategy> strategies;
    
    private final Map<String, AssignStrategy> strategyMap = new HashMap<>();
    
    /**
     * 初始化策略映射
     */
    @PostConstruct
    public void init() {
        for (AssignStrategy strategy : strategies) {
            strategyMap.put(strategy.getBizType(), strategy);
        }
    }
    
    /**
     * 根据业务类型获取对应的策略
     * 
     * @param bizType 业务类型
     * @return 对应的策略实现
     */
    public AssignStrategy getStrategy(String bizType) {
        AssignStrategy strategy = strategyMap.get(bizType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的业务类型: " + bizType);
        }
        return strategy;
    }
}
