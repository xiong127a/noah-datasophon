package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NebulaGraphHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (!hosts.isEmpty()) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nebulaGraphHost}", hosts.get(0));
        }
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = (HashMap<String, List<String>>) CacheOperateUtils.get(hostMapKey);
        if (Objects.nonNull(hostMap)) {
            List<String> hostList = hostMap.get("Meta");
            for (ServiceConfig serviceConfig : list) {
                if ("metaServerAddrs".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(hostList.stream()
                            .map(host -> host + ":9559")
                            .collect(Collectors.joining(",")));
                }
            }
        }
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }
}
