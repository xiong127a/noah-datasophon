package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.model.ServiceRoleInfo;

import java.util.List;
import java.util.Map;

public class PostgresqlHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        if (hosts.size() == 1) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${PostgresqlMaster}",
                    hosts.getFirst());
        }
    }


    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String postgresqlMaster = globalVariables.get("${PostgresqlMaster}");
        serviceRoleInfo.setMasterHost(postgresqlMaster);
    }

}
