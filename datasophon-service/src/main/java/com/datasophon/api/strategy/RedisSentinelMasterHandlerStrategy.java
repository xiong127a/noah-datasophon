package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;

import java.util.List;
import java.util.Map;

public class RedisSentinelMasterHandlerStrategy implements ServiceRoleStrategy{
    @Override
    public void handler(Long clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (CollUtil.isNotEmpty(hosts)) {
            SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${redisSentinelMasterHost}", hosts.getFirst());
        }
    }


}
