package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KibanaSearchHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig config : list) {
            if ("server.port".equals(config.getName())) {
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${kibanaPort}", Convert.toStr(config.getValue()));
            }
        }
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String initMasterNodes = globalVariables.get("${initMasterNodes}");
        String esHttpPort = globalVariables.get("${esHttpPort}");
        List<String> esHosts = StrUtil.split(initMasterNodes, ",");
        if (CollUtil.isNotEmpty(esHosts)) {
            esHosts = esHosts.stream()
                    .map(host -> "http://" + host + ":" + esHttpPort)
                    .collect(Collectors.toList());
            Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
            ServiceConfig hosts = map.get("elasticsearch.hosts");
            hosts.setValue(esHosts);
            hosts.setDefaultValue(esHosts);
        }
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        String kibanaPort = GlobalVariables.get(roleInstanceEntity.getClusterId()).get("${kibanaPort}");
        String url = "http://" + roleInstanceEntity.getHostname() + ":" + kibanaPort;
        try {
            HttpUtil.get(url);
            ProcessUtils.recoverAlert(roleInstanceEntity);
        } catch (Exception e) {
            // save alert
            String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
            ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }
    }


}
