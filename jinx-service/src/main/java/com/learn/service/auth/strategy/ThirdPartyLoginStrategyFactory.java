package com.learn.service.auth.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方登录策略工厂
 * 管理所有第三方登录策略，根据类型返回对应的策略实例
 */
@Component
public class ThirdPartyLoginStrategyFactory {
    
    @Autowired
    private List<ThirdPartyLoginStrategy> strategies;
    
    private Map<String, ThirdPartyLoginStrategy> strategyMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (ThirdPartyLoginStrategy strategy : strategies) {
            strategyMap.put(strategy.getThirdPartyType(), strategy);
        }
    }
    
    /**
     * 获取指定类型的登录策略
     * @param thirdPartyType 第三方平台类型
     * @return 登录策略实例，如果类型不支持则返回null
     */
    public ThirdPartyLoginStrategy getStrategy(String thirdPartyType) {
        return strategyMap.get(thirdPartyType);
    }
    
    /**
     * 检查是否支持指定类型
     * @param thirdPartyType 第三方平台类型
     * @return 是否支持
     */
    public boolean supportsType(String thirdPartyType) {
        return strategyMap.containsKey(thirdPartyType);
    }
} 
