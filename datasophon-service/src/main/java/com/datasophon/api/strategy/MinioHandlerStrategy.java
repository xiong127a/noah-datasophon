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

public class MinioHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {


    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(hostMapKey, new TypeReference<HashMap<String, List<String>>>() {});

        if (Objects.nonNull(hostMap)) {
            List<String> hostList = hostMap.get("MinioService");
            for (ServiceConfig serviceConfig : list) {
                if ("minioDataPaths".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(hostList.stream().map(t -> "http://" + t + ":9000/opt/datasophon/minio/data").collect(Collectors.toList()));
                }
            }
        }

    }

}
