package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;

public class PostgresqlHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (hosts.size() == 1) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${PostgresqlMaster}",
                    hosts.get(0));
        }
    }


    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String postgresqlMaster = globalVariables.get("${PostgresqlMaster}");
        serviceRoleInfo.setMasterHost(postgresqlMaster);
    }

}
