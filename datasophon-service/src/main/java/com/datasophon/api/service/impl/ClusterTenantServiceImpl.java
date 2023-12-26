package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.TenantResource;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterYarnQueue;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
    public Result saveOrUpdateTenant(ClusterTenant clusterTenant) throws Exception {

        TenantResource resource = new TenantResource();
        BeanUtil.copyProperties(clusterTenant, resource);

        if (StrUtil.isNotBlank(clusterTenant.getHdfsPath())) {
            resource.setServiceName("HDFS");
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "NameNode"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getKafkaTopicsConfig())) {
            String zkAddr = GlobalVariables.get(clusterTenant.getClusterId()).get("${kafkaZkAddr}");
            resource.setServiceName("KAFKA");
            resource.setKafkaZkAddr(zkAddr);
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "KafkaBroker"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getHbaseNamespace())) {
            resource.setServiceName("HBASE");
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "HbaseMaster"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getHiveDatabase())) {
            String hiveMetastoreDir = GlobalVariables.get(clusterTenant.getClusterId()).get("${hive.metastore.warehouse.dir}");
            resource.setServiceName("HIVE");
            resource.setHiveMetastoreDir(hiveMetastoreDir);
            tellTenantActor(getRoleHostName(clusterTenant.getClusterId(), "HiveServer2"), resource);
        }

        if (StrUtil.isNotBlank(clusterTenant.getYarnMemory()) && Objects.isNull(clusterTenant.getId())) {
            createTenantYarnResource(clusterTenant);
        } else if (StrUtil.isNotBlank(clusterTenant.getYarnMemory()) && Objects.nonNull(clusterTenant.getId())) {
            updateTenantYarnResource(clusterTenant);
        }

        // 创建ranger相关策略
        if (Objects.isNull(clusterTenant.getId())) {
            ActorRef tenantActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
            tenantActor.tell(clusterTenant, ActorRef.noSender());
        }

        this.saveOrUpdateTenant(clusterTenant);

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
        clusterYarnQueue.setSchedulePolicy("fifo");
        clusterYarnQueue.setWeight(1);
        clusterYarnQueueService.saveQueue(clusterYarnQueue);
        clusterYarnQueueService.refreshQueues(clusterTenant.getClusterId());
    }

    /**
     * 更新yarn队列配置
     */
    private void updateTenantYarnResource(ClusterTenant clusterTenant) throws Exception {
        ClusterYarnQueue clusterYarnQueue = clusterYarnQueueService.getQueueByName(clusterTenant.getClusterId(), clusterTenant.getTenantName());
        clusterYarnQueue.setClusterId(clusterTenant.getClusterId());
        clusterYarnQueue.setMaxCore(Integer.valueOf(clusterTenant.getYarnCpu()));
        clusterYarnQueue.setMaxMem(Integer.valueOf(clusterTenant.getYarnMemory()));
        clusterYarnQueue.setQueueName(clusterTenant.getTenantName());
        clusterYarnQueueService.updateById(clusterYarnQueue);
        clusterYarnQueueService.refreshQueues(clusterYarnQueue.getClusterId());
    }

    private String getRoleHostName(Integer clusterId, String roleName) {
        List<ClusterServiceRoleInstanceEntity> nameNodeInstance = clusterServiceRoleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId).eq(Constants.SERVICE_ROLE_NAME, roleName));
        return nameNodeInstance.get(0).getHostname();
    }

}