package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;

import java.util.List;
import java.util.Map;

public class HttpFsHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        if (CollUtil.isNotEmpty(hosts)) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${httpFs}", hosts.getFirst());
        }
    }

}
