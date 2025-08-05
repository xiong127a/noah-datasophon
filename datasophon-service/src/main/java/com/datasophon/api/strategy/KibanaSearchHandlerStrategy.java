package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.dao.enums.AlertLevel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KibanaSearchHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        for (ServiceConfig config : list) {
            if ("server.port".equals(config.getName())) {
                simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${kibanaPort}", Convert.toStr(config.getValue()));
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
            Map<String, ServiceConfig> map = list.stream()
                    .collect(java.util.stream.Collectors.toMap(ServiceConfig::getName, config -> config));
            ServiceConfig hosts = map.get("elasticsearch.hosts");
            hosts.setValue(esHosts);
            hosts.setDefaultValue(esHosts);
        }
    }


    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto, Map<String, ClusterServiceRoleInstanceDTO> map) {
        String kibanaPort = GlobalVariables.get(roleInstanceDto.clusterId()).get("${kibanaPort}");
        String url = "http://" + roleInstanceDto.hostname() + ":" + kibanaPort;
        ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
        try {
            HttpUtil.get(url);
            serviceStateManagementService.recoverAlert(roleInstanceDto);
        } catch (Exception e) {
            // save alert
            String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
            serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }
    }


}
