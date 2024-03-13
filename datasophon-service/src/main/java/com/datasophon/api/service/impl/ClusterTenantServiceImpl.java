package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.master.YarnQueueActor;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.TenantResource.*;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenant> implements ClusterTenantService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterTenantServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterYarnQueueService clusterYarnQueueService;

//    private final Cache<String, ActorRef> resourceActorCache = CacheUtil.newFIFOCache(100);

    @Override
    public Result listTenant(Integer clusterId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        QueryWrapper<ClusterTenant> queryWrapper = new QueryWrapper<ClusterTenant>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .last("limit " + offset + "," + size);
        List<ClusterTenant> list = this.list(queryWrapper);
        int total = this.count(new QueryWrapper<ClusterTenant>()
                .eq(Constants.CLUSTER_ID, clusterId));
        return Result.success(list).put(Constants.TOTAL, total);
    }

    @Override
    public Result saveOrUpdateTenant(ClusterTenant clusterTenant) throws Exception {
        TenantResource resource = new TenantResource();
        BeanUtil.copyProperties(clusterTenant, resource);
        Map<String, String> roleHostMap = getRoleHostMap(clusterTenant.getClusterId());
        List<TenantFrameResource> allFrameResource = CollUtil.unionAll(
                resource.getHdfsResourceList().stream().map(t -> (TenantFrameResource) t).collect(Collectors.toList()),
                resource.getHbaseResourceList().stream().map(t -> (TenantFrameResource) t).collect(Collectors.toList()),
                resource.getHiveResourceList().stream().map(t -> (TenantFrameResource) t).collect(Collectors.toList()),
                resource.getKafkaResourceList().stream().map(t -> (TenantFrameResource) t).collect(Collectors.toList()),
                resource.getYarnResourceList().stream().map(t -> (TenantFrameResource) t).collect(Collectors.toList())
        );

        // 框架资源操作
        for (TenantFrameResource tenantFrameResource : allFrameResource) {
            if (tenantFrameResource.getServiceName().equals("YARN")) {
                TenantYarnResource tenantYarnResource = (TenantYarnResource) tenantFrameResource;
                tenantYarnResource.setClusterId(clusterTenant.getClusterId());
                ActorRef resourceActorRef = ActorUtils.getLocalActor(YarnQueueActor.class, "yarnQueueActor");
                resourceActorRef.tell(tenantYarnResource, ActorRef.noSender());
                continue;
            }

            String serviceMasterRoleName = getServiceMasterRoleName(tenantFrameResource.getServiceName());
            if (tenantFrameResource.getServiceName().equals("KAFKA")) {
                String zkAddr = GlobalVariables.get(clusterTenant.getClusterId()).get("${kafkaZkAddr}");
                TenantKafkaResource kafkaResource = (TenantKafkaResource) tenantFrameResource;
                kafkaResource.setKafkaZkAddr(zkAddr);
            }
            if (tenantFrameResource.getServiceName().equals("HIVE")) {
                String hiveMetastoreDir = GlobalVariables.get(clusterTenant.getClusterId()).get("${hive.metastore.warehouse.dir}");
                TenantHiveResource hiveResource = (TenantHiveResource) tenantFrameResource;
                hiveResource.setHiveMetastoreDir(hiveMetastoreDir);
            }
            ActorRef resourceActorRef = ActorUtils.getRemoteActor(roleHostMap.get(serviceMasterRoleName), "tenantResourceActor");
            resourceActorRef.tell(tenantFrameResource, ActorRef.noSender());
        }

        // ranger策略操作
        ActorRef tenantActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        tenantActor.tell(resource, ActorRef.noSender());

        this.saveOrUpdate(clusterTenant);

        return Result.success();
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

    private Map<String, String> getRoleHostMap(Integer clusterId) {
        List<ClusterServiceRoleInstanceEntity> nameNodeInstance = clusterServiceRoleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId));
        return nameNodeInstance.stream().collect(Collectors.toMap(
                ClusterServiceRoleInstanceEntity::getServiceRoleName,
                ClusterServiceRoleInstanceEntity::getHostname,
                (a, b) -> a,
                HashMap::new
        ));
    }

}