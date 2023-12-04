package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.TenantResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterYarnQueue;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenant> implements ClusterTenantService {

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterYarnQueueService clusterYarnQueueService;

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
    public Result saveTenant(ClusterTenant clusterTenant) throws Exception {

        if (StrUtil.isNotBlank(clusterTenant.getHdfsPath())) {
            TenantResource resource = TenantResource.builder()
                    .hdfsPath(clusterTenant.getHdfsPath())
                    .hdfsSpaceQuota(clusterTenant.getHdfsSpaceQuota())
                    .hdfsQuota(clusterTenant.getHdfsQuota())
                    .build();
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "NameNode"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getKafkaTopicsConfig())) {
            TenantResource resource = TenantResource.builder()
                    .kafkaTopicsConfig(clusterTenant.getKafkaTopicsConfig())
                    .build();
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "KafkaBroker"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getHbaseNamespace())) {
            TenantResource resource = TenantResource.builder()
                    .hbaseNamespace(clusterTenant.getHbaseNamespace())
                    .hbaseCapacity(clusterTenant.getHbaseCapacity())
                    .hbaseRegionServerNum(clusterTenant.getHbaseRegionServerNum())
                    .build();
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "HbaseMaster"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getHiveDatabase())) {
            String hiveMetastoreDir = GlobalVariables.get(clusterTenant.getClusterId()).get("${hive.metastore.warehouse.dir}");
            TenantResource resource = TenantResource.builder()
                    .hiveDatabase(clusterTenant.getHiveDatabase())
                    .hiveDatabaseCapacity(clusterTenant.getHiveDatabaseCapacity())
                    .hiveMetastoreDir(hiveMetastoreDir)
                    .build();
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "HiveServer2"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getYarnMemory())) {
            createTenantYarnResource(clusterTenant);
        }

        return Result.success();
    }

    private void tellTenantActor(String hostname, TenantResource resource) {
        ActorRef remoteActor = ActorUtils.getRemoteActor(hostname, "tenantResourceActor");
        remoteActor.tell(resource, ActorRef.noSender());
    }

    /**
     * 创建yarn队列及设置限额
     */
    private void createTenantYarnResource(ClusterTenant clusterTenant) throws Exception {
        ClusterYarnQueue clusterYarnQueue = new ClusterYarnQueue();
        clusterYarnQueue.setAllowPreemption(1);
        clusterYarnQueue.setAmShare("0.1");
        clusterYarnQueue.setAppNum(100);
        clusterYarnQueue.setClusterId(clusterTenant.getClusterId());
        clusterYarnQueue.setMaxCore(Integer.valueOf(clusterTenant.getYarnCpu()));
        clusterYarnQueue.setMinCore(1);
        clusterYarnQueue.setMinMem(1);
        clusterYarnQueue.setMaxMem(Integer.valueOf(clusterTenant.getYarnMemory()));
        clusterYarnQueue.setQueueName(clusterTenant.getTenantName());
        clusterYarnQueue.setSchedulePolicy("fair");
        clusterYarnQueue.setWeight(1);
        clusterYarnQueueService.saveQueue(clusterYarnQueue);
        clusterYarnQueueService.refreshQueues(clusterTenant.getClusterId());
    }

    private String getRoleHostName(Integer clusterId, String roleName) {
        List<ClusterServiceRoleInstanceEntity> nameNodeInstance = clusterServiceRoleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId).eq(Constants.SERVICE_ROLE_NAME, roleName));
        return nameNodeInstance.get(0).getHostname();
    }

}