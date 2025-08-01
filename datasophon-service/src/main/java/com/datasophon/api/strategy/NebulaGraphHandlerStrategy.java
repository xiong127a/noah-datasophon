package com.datasophon.api.strategy;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NebulaGraphHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (!hosts.isEmpty()) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nebulaGraphHost}", hosts.getFirst());
        }
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        Map<String, List<String>> hostMap = CacheOperateUtils.getGeneric(hostMapKey, TypeRefs.MAP_STRING_LIST_STRING);
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

}
