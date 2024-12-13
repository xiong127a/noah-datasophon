package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RedisSentinelHandlerStrategy implements ServiceRoleStrategy{
    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String redisSentinelMasterHost = globalVariables.get("${redisSentinelMasterHost}");
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        ServiceConfig redisMasterHostConfig = map.get("redisSentinelMasterHost");

        if (Objects.nonNull(redisSentinelMasterHost)) {
            redisMasterHostConfig.setValue(redisSentinelMasterHost);

        }
    }

}
