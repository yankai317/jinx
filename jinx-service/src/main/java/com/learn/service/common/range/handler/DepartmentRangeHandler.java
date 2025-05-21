package com.learn.service.common.range.handler;

import com.learn.dto.common.RangeQueryResponse;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.service.dto.CommonRangeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门范围处理器
 * 责任链模式中处理部门信息的具体处理器
 */
@Component
@Slf4j
public class DepartmentRangeHandler extends RangeHandler {
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Override
    public Object handle(CommonRangeQueryResponse response) {
        log.info("处理部门范围信息");
        
        // 获取当前处理结果，如果为空则创建新的响应对象
        RangeQueryResponse result = (RangeQueryResponse) (nextHandler != null ? nextHandler.handle(response) : new RangeQueryResponse());
        
        // 处理部门信息
        List<RangeQueryResponse.RangeDepartment> departmentInfos = new ArrayList<>();
        
        // 如果有部门ID，则查询部门详细信息
        if (!CollectionUtils.isEmpty(response.getDepartmentIds())) {

            // 查询部门信息
            List<Department> departments = departmentMapper.selectBatchIds(response.getDepartmentIds());

            // 转换为RangeDepartment对象
            if (!CollectionUtils.isEmpty(departments)) {
                departmentInfos = departments.stream()
                        .map(dept -> {
                            RangeQueryResponse.RangeDepartment rangeDepartment = new RangeQueryResponse.RangeDepartment();
                            rangeDepartment.setDepartmentId(dept.getId());
                            rangeDepartment.setDepartmentName(dept.getDepartmentName());
                            return rangeDepartment;
                        })
                        .collect(Collectors.toList());
            }
        }
        
        // 设置部门信息
        result.setDepartmentInfos(departmentInfos);
        
        return result;
    }
}
