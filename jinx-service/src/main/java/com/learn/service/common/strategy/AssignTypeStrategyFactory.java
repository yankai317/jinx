package com.learn.service.common.strategy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指派类型策略工厂
 * 根据指派类型返回对应的处理策略
 */
@Component
public class AssignTypeStrategyFactory {
    
    @Autowired
    private List<AssignTypeStrategy> strategies;
    
    private final Map<String, AssignTypeStrategy> strategyMap = new HashMap<>();
    
    /**
     * 初始化策略映射
     */
    @PostConstruct
    public void init() {
        for (AssignTypeStrategy strategy : strategies) {
            strategyMap.put(strategy.getAssignTypeCode(), strategy);
        }
    }
    
    /**
     * 根据指派类型获取对应的策略
     * 
     * @param assignType 指派类型
     * @return 对应的策略实现
     */
    public AssignTypeStrategy getStrategy(String assignType) {
        AssignTypeStrategy strategy = strategyMap.get(assignType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的指派类型: " + assignType);
        }
        return strategy;
    }
} 