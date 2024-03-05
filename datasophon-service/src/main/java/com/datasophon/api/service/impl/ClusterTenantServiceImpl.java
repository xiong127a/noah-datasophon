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
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
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

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterYarnQueueService clusterYarnQueueService;

    private final Cache<String, ActorRef> resourceActorCache = CacheUtil.newFIFOCache(100);

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
                resource.getHdfsResourceList(),
                resource.getHbaseResourceList(),
                resource.getHiveResourceList(),
                resource.getKafkaResourceList()
        );

        // 框架资源操作
        for (TenantFrameResource tenantFrameResource : allFrameResource) {
            if (tenantFrameResource.getServiceName().equals("YARN")) {
                operateYarnResource((TenantYarnResource) tenantFrameResource, clusterTenant.getClusterId());
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
            ExecResult execResult = tellResourceActor(roleHostMap.get(serviceMasterRoleName), tenantFrameResource);
            if (!execResult.getExecResult()) {
                log.error(execResult.getExecErrOut());
                return Result.error(execResult.getExecErrOut());
            }
        }

        // ranger策略操作
        if (Objects.isNull(clusterTenant.getId())) {
            ActorRef tenantActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
            tenantActor.tell(clusterTenant, ActorRef.noSender());
        }

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

    private ExecResult tellResourceActor(String hostname, TenantFrameResource resource) throws Exception {
        ActorRef resourceActorRef = resourceActorCache.get(hostname);
        if (resourceActorRef == null) {
            resourceActorRef = ActorUtils.getRemoteActor(hostname, "tenantResourceActor");
            resourceActorCache.put(hostname, resourceActorRef);
        }

        // 使用 ask 模式发送消息给 actor，并等待返回结果
        Timeout timeout = new Timeout(Duration.create(20, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(resourceActorRef, resource, timeout);
        return (ExecResult) Await.result(execFuture, timeout.duration());
    }

    private void operateYarnResource(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        TROperateType trOperateType = TROperateType.valueOf(yarnResource.getType());
        switch (trOperateType) {
            case ADD:
                createTenantYarnResource(yarnResource, clusterId);
                break;
            case UPDATE:
                updateTenantYarnResource(yarnResource, clusterId);
                break;
            case DELETE:
                deleteTenantYarnResource(yarnResource, clusterId);
                break;
        }
    }

    /**
     * 创建yarn队列及设置限额
     */
    private void createTenantYarnResource(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterYarnQueue clusterYarnQueue = new ClusterYarnQueue();
        clusterYarnQueue.setAllowPreemption(1);
        clusterYarnQueue.setAmShare("0.1");
        clusterYarnQueue.setAppNum(100);
        clusterYarnQueue.setClusterId(clusterId);
        clusterYarnQueue.setMaxCore(Integer.valueOf(yarnResource.getYarnCpu()));
        clusterYarnQueue.setMinCore(1);
        clusterYarnQueue.setMinMem(1);
        clusterYarnQueue.setMaxMem(Integer.valueOf(yarnResource.getYarnMemory()));
        clusterYarnQueue.setQueueName(yarnResource.getYarnQueueName());
        clusterYarnQueue.setSchedulePolicy("fifo");
        clusterYarnQueue.setWeight(1);
        clusterYarnQueueService.saveQueue(clusterYarnQueue);
        clusterYarnQueueService.refreshQueues(clusterId);
    }

    /**
     * 更新yarn队列配置
     */
    private void updateTenantYarnResource(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterYarnQueue clusterYarnQueue = clusterYarnQueueService.getQueueByName(clusterId, yarnResource.getYarnQueueName());
        clusterYarnQueue.setClusterId(clusterId);
        clusterYarnQueue.setMaxCore(Integer.valueOf(yarnResource.getYarnCpu()));
        clusterYarnQueue.setMaxMem(Integer.valueOf(yarnResource.getYarnMemory()));
        clusterYarnQueue.setQueueName(yarnResource.getYarnQueueName());
        clusterYarnQueueService.updateById(clusterYarnQueue);
        clusterYarnQueueService.refreshQueues(clusterYarnQueue.getClusterId());
    }

    private void deleteTenantYarnResource(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterYarnQueue clusterYarnQueue = clusterYarnQueueService.getQueueByName(clusterId, yarnResource.getYarnQueueName());
        clusterYarnQueueService.removeById(clusterYarnQueue.getId());
        clusterYarnQueueService.refreshQueues(clusterYarnQueue.getClusterId());
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