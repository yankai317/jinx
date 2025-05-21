package com.learn.service.common.range.handler;

import com.learn.dto.common.RangeQueryResponse;
import com.learn.infrastructure.repository.entity.FunctionRole;
import com.learn.infrastructure.repository.entity.OrgRole;
import com.learn.infrastructure.repository.mapper.FunctionRoleMapper;
import com.learn.infrastructure.repository.mapper.OrgRoleMapper;
import com.learn.service.dto.CommonRangeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色范围处理器
 * 责任链模式中处理角色信息的具体处理器
 */
@Component
@Slf4j
public class RoleRangeHandler extends RangeHandler {
    
    @Autowired
    private OrgRoleMapper orgRoleMapper;
    
    @Override
    public Object handle(CommonRangeQueryResponse response) {
        log.info("处理角色范围信息");
        
        // 获取当前处理结果，如果为空则创建新的响应对象
        RangeQueryResponse result = (RangeQueryResponse) (nextHandler != null ? nextHandler.handle(response) : new RangeQueryResponse());
        
        // 处理角色信息
        List<RangeQueryResponse.RangeRole> roleInfos = new ArrayList<>();
        
        // 如果有角色ID，则查询角色详细信息
        if (!CollectionUtils.isEmpty(response.getRoleIds())) {
            // 查询角色信息
            List<OrgRole> roles = orgRoleMapper.selectBatchIds(response.getRoleIds());

            // 转换为RangeRole对象
            if (!CollectionUtils.isEmpty(roles)) {
                roleInfos = roles.stream()
                        .map(role -> {
                            RangeQueryResponse.RangeRole rangeRole = new RangeQueryResponse.RangeRole();
                            rangeRole.setRoleId(role.getId());
                            rangeRole.setRoleName(role.getRoleName());
                            return rangeRole;
                        })
                        .collect(Collectors.toList());
            }
        }
        
        // 设置角色信息
        result.setRoleInfos(roleInfos);
        
        return result;
    }
}
