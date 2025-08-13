package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.List;
import java.util.Map;

public class StarrocksHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
        getConfig(clusterId, list);
    }

    @Override
    public void getConfig(Long clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String priority_networks = globalVariables.get("${priority_networks}");
        for (ServiceConfig serviceConfig : list) {
            if (StrUtil.equals(serviceConfig.getName(), "priority_networks")) {
                serviceConfig.setValue(priority_networks);
            }
        }
    }

}
