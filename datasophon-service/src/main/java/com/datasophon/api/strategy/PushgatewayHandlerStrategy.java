package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;

import java.util.List;
import java.util.Map;

public class PushgatewayHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        if (hosts.size() == 1) {
            Map<String, String> variables = GlobalVariables.get(clusterId);
            SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
            simpleClusterVariableService.generateClusterVariable(variables, clusterId, "${pushgatewayHost}", hosts.getFirst());
        }
    }

}
