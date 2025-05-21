package com.learn.service.common.range.handler;

import com.learn.service.dto.CommonRangeQueryResponse;

/**
 * 范围处理器抽象类
 *责任链模式的抽象处理器
 */
public abstract class RangeHandler {
    /**
     * 下一个处理器
     */
    protected RangeHandler nextHandler;
    
    /**
     * 设置下一个处理器
     * 
     * @param nextHandler 下一个处理器
     * @return 下一个处理器
     */
    public RangeHandler setNext(RangeHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler;
    }
    
    /**
     * 处理范围信息
     * 
     * @param response 范围查询响应
     * @return 处理后的对象
     */
    public abstract Object handle(CommonRangeQueryResponse response);
}
