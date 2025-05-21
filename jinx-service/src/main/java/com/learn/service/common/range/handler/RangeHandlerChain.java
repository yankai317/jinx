package com.learn.service.common.range.handler;

import com.learn.dto.common.RangeQueryResponse;
import com.learn.service.dto.CommonRangeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.learn.constants.BizConstants.DEFAULT_USER_ID;

/**
 * 范围处理器链
 * 责任链模式的管理器，用于组装和管理责任链
 */
@Component
@Slf4j
public class RangeHandlerChain {
    
    @Autowired
    private DepartmentRangeHandler departmentRangeHandler;
    
    @Autowired
    private RoleRangeHandler roleRangeHandler;
    
    @Autowired
    private UserRangeHandler userRangeHandler;
    
    /**
     * 处理范围信息
     * 
     * @param response 范围查询响应
     * @return 处理后的对象
     */
    public RangeQueryResponse process(CommonRangeQueryResponse response) {
        log.info("开始处理范围信息");
        
        // 组装责任链
        departmentRangeHandler.setNext(roleRangeHandler).setNext(userRangeHandler);
        
        // 处理范围信息


        return (RangeQueryResponse) departmentRangeHandler.handle(response);
    }
}
