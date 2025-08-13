package com.datasophon.api.strategy;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NoahSyncHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(NoahSyncHandlerStrategy.class);


    @Override
    public void getConfig(Long clusterId, List<ServiceConfig> list) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        String hostMapKey = clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING;
        Map<String, List<String>> hostMap = CacheOperateUtils.getGeneric(hostMapKey, TypeRefs.MAP_STRING_LIST_STRING);

        if (Objects.nonNull(hostMap)) {
            List<String> noahSyncServers = hostMap.get("NoahSyncServer");
            Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
            ServiceConfig hosts = map.get("hosts");
            hosts.setName("hosts");
            hosts.setLabel("集群节点ip");
            hosts.setDescription("集群节点ip");
            hosts.setValue(noahSyncServers);
            hosts.setHidden(false);
            hosts.setRequired(true);
            hosts.setType("multiple");
            hosts.setSeparator(",");
            hosts.setDefaultValue(noahSyncServers);
            hosts.setConfigType("zkserver");
            hosts.setConfigurableInWizard(true);
        }
    }

}
