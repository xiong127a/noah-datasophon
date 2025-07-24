package com.datasophon.api.strategy;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AlluxioHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

        ServiceRoleStrategy.super.handlerConfig(clusterId, list);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(hostMapKey, new TypeReference<>() {
        });

        if (Objects.nonNull(hostMap)) {
            List<String> masterHosts = hostMap.get("AlluxioMaster");
            List<String> workerHosts = hostMap.get("AlluxioWorker");
            for (ServiceConfig serviceConfig : list) {
                if ("alluxio.master.embedded.journal.addresses".equals(serviceConfig.getName())) {
                    String masterAddr = masterHosts.stream().map(t -> t + ":19200").collect(Collectors.joining(","));
                    serviceConfig.setValue(masterAddr);
                }
                if ("masters".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(masterHosts);
                }
                if ("workers".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(workerHosts);
                }
            }
        }
    }

}
