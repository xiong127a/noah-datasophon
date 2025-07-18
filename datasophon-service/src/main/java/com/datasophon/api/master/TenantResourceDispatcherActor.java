package com.datasophon.api.master;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHiveResource;
import com.datasophon.common.model.TenantResource.TenantKafkaResource;
import com.datasophon.common.model.TenantResource.TenantYarnResource;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TenantResourceDispatcherActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(TenantResourceDispatcherActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TenantFrameResource.class, this::handleTenantFrameResource)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleTenantFrameResource(TenantFrameResource tenantFrameResource) {
        try {
            Map<String, String> roleHostMap = getRoleHostMap(tenantFrameResource.getClusterId());

            if (tenantFrameResource.getServiceName().equals("YARN")) {
                TenantYarnResource tenantYarnResource = (TenantYarnResource) tenantFrameResource;
                tenantYarnResource.setClusterId(tenantFrameResource.getClusterId());
                ActorRef resourceActorRef = ActorUtils.getLocalActor(YarnQueueActor.class, "yarnQueueActor");
                resourceActorRef.tell(tenantYarnResource, ActorRef.noSender());
            } else {
                String serviceMasterRoleName = getServiceMasterRoleName(tenantFrameResource.getServiceName());
                if (tenantFrameResource.getServiceName().equals("KAFKA")) {
                    String zkAddr = GlobalVariables.get(tenantFrameResource.getClusterId()).get("${kafkaZkAddr}");
                    TenantKafkaResource kafkaResource = (TenantKafkaResource) tenantFrameResource;
                    kafkaResource.setKafkaZkAddr(zkAddr);
                }
                if (tenantFrameResource.getServiceName().equals("HIVE")) {
                    String hiveMetastoreDir = GlobalVariables.get(tenantFrameResource.getClusterId())
                            .get("${hive.metastore.warehouse.dir}");
                    TenantHiveResource hiveResource = (TenantHiveResource) tenantFrameResource;
                    hiveResource.setHiveMetastoreDir(hiveMetastoreDir);
                }
                ActorRef resourceActorRef = ActorUtils.getRemoteActor(roleHostMap.get(serviceMasterRoleName),
                        "tenantResourceActor");
                resourceActorRef.tell(tenantFrameResource, ActorRef.noSender());
            }
        } catch (Exception e) {
            logger.error("Error handling TenantFrameResource", e);
        }
    }

    private Map<String, String> getRoleHostMap(Integer clusterId) {
        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        List<ClusterServiceRoleInstanceEntity> nameNodeInstance = clusterServiceRoleInstanceService.list(
                new QueryWrapper<ClusterServiceRoleInstanceEntity>().eq(Constants.CLUSTER_ID, clusterId));
        return nameNodeInstance.stream().collect(Collectors.toMap(
                ClusterServiceRoleInstanceEntity::getServiceRoleName,
                ClusterServiceRoleInstanceEntity::getHostname,
                (a, b) -> a,
                HashMap::new));
    }

    private String getServiceMasterRoleName(String serviceName) {
        switch (serviceName) {
            case "HDFS":
                return "NameNode";
            case "HIVE":
                return "HiveServer2";
            case "KAFKA":
                return "KafkaBroker";
            case "HBASE":
                return "HbaseMaster";
            default:
                return "";
        }
    }
}
