package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClickhouseHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        HashMap<String, List<String>> hostMap = CacheOperateUtils.getWithType(hostMapKey, new TypeReference<HashMap<String, List<String>>>() {});

        if (Objects.nonNull(hostMap)) {
            List<String> hostList = hostMap.get("ClickHouse");
            for (ServiceConfig serviceConfig : list) {
                if ("ckShardAddress".equals(serviceConfig.getName())) {
                    serviceConfig.setValue(hostList.stream().map(t -> t + ":9010").collect(Collectors.toList()));
                }
                if ("ckZkAddress".equals(serviceConfig.getName())) {
                    Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                    String zkUrls = globalVariables.get("${zkUrls}");
                    List<String> zkUrlList = StrUtil.splitTrim(zkUrls, ",");
                    serviceConfig.setValue(zkUrlList);
                }
            }
        }
    }

}
