package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.master.*;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.model.TenantResource.*;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenant> implements ClusterTenantService {

    private static final Cache<String, ActorRef> actorRefCache = CacheUtil.newFIFOCache(10);

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
        if (Objects.isNull(clusterTenant.getId())) {
            try {
                checkTenant(clusterTenant);
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }
        }
        TenantResource resource = new TenantResource();
        BeanUtil.copyProperties(clusterTenant, resource);
        List<TenantFrameResource> allFrameResource = CollUtil.unionAll(
                resource.getHdfsResourceList().stream().map(t -> (TenantFrameResource) t).peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList()),
                resource.getHbaseResourceList().stream().map(t -> (TenantFrameResource) t).peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList()),
                resource.getHiveResourceList().stream().map(t -> (TenantFrameResource) t).peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList()),
                resource.getKafkaResourceList().stream().map(t -> (TenantFrameResource) t).peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList()),
                resource.getYarnResourceList().stream().map(t -> (TenantFrameResource) t).peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList())
        );

        // 框架资源操作
        ActorRef tenantResourceDispatcherActor = getActorRef(TenantResourceDispatcherActor.class, "tenantResourceDispatcherActor");
        for (TenantFrameResource tenantFrameResource : allFrameResource) {
            tenantResourceDispatcherActor.tell(tenantFrameResource, ActorRef.noSender());
        }

        // ranger策略操作
        ActorRef tenantRangerActor = getActorRef(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(resource, ActorRef.noSender());

        this.saveOrUpdate(clusterTenant);

        return Result.success();
    }

    @Override
    public Result deleteTenantById(Integer id) {
        ClusterTenant tenant = this.getById(id);
        TenantRangerCommand command = new TenantRangerCommand();
        command.setClusterId(tenant.getClusterId());
        command.setTenantName(tenant.getTenantName());

        // 删除所有ranger相关策略及角色
        ActorRef tenantRangerActor = getActorRef(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(command, ActorRef.noSender());

        if (this.removeById(id)) {
            return Result.success();
        }
        return Result.error();
    }

    public ActorRef getActorRef(Class<?> actorClazz, String actorName) {
        if (actorRefCache.containsKey(actorName)) {
            return actorRefCache.get(actorName);
        } else {
            ActorRef actorRef = ActorUtils.actorSystem.actorOf(
                    Props.create(actorClazz).withDispatcher("my-forkjoin-dispatcher"),
                    actorName
            );
            actorRefCache.put(actorName, actorRef);
            return actorRef;
        }
    }

    private void checkTenant(ClusterTenant clusterTenant) throws Exception {
        List<ClusterTenant> tenantList = this.list();

        // 校验名称
        List<String> exitsName = tenantList.stream().map(ClusterTenant::getTenantName).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(exitsName) && exitsName.contains(clusterTenant.getTenantName())) {
            throw new IllegalArgumentException("租户名称已存在");
        }

        // 校验hdfs资源
        if (CollUtil.isNotEmpty(clusterTenant.getHdfsResourceList())) {
            List<String> existHdfsPaths = tenantList.stream()
                    .map(ClusterTenant::getHdfsResourceList)
                    .map(this::convertToMap)
                    .flatMap(List::stream)
                    .map(t -> t.get("hdfsPath"))
                    .collect(Collectors.toList());
            for (com.datasophon.dao.entity.tenantResource.TenantHdfsResource hdfsResource : clusterTenant.getHdfsResourceList()) {
                if (!StrUtil.startWith(hdfsResource.getHdfsPath(), "/")) {
                    throw new IllegalArgumentException("hdfs路径不合法");
                }
                if (existHdfsPaths.contains(hdfsResource.getHdfsPath())) {
                    throw new IllegalArgumentException("hdfs路径已被添加过,请勿重复添加");
                }
                if (!StrUtil.isNumeric(hdfsResource.getHdfsSpaceQuota())) {
                    throw new IllegalArgumentException("hdfs配置不合法");
                }
            }
        }

        // 校验yarn
        if (CollUtil.isNotEmpty(clusterTenant.getYarnResourceList())) {
            List<String> existYarnQueue = tenantList.stream()
                    .map(ClusterTenant::getYarnResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("parentQueueName") + "." + t.get("queueName"))
                    .collect(Collectors.toList());
            for (com.datasophon.dao.entity.tenantResource.TenantYarnResource tenantYarnResource : clusterTenant.getYarnResourceList()) {
                String queueName = tenantYarnResource.getParentQueueName() + "." + tenantYarnResource.getQueueName();
                if (existYarnQueue.contains(queueName)) {
                    throw new IllegalArgumentException("yarn队列已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantYarnResource.getCapacityPercent())
                        ||
                        Convert.toInt(tenantYarnResource.getCapacityPercent()) < 0
                        ||
                        Convert.toInt(tenantYarnResource.getCapacityPercent()) > 100) {
                    throw new IllegalArgumentException("yarn队列配额不合法");
                }
            }
        }

        // 校验hive
        if (CollUtil.isNotEmpty(clusterTenant.getHiveResourceList())) {
            List<String> existHiveDbName = tenantList.stream()
                    .map(ClusterTenant::getHiveResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hiveDatabase"))
                    .collect(Collectors.toList());
            for (com.datasophon.dao.entity.tenantResource.TenantHiveResource tenantHiveResource : clusterTenant.getHiveResourceList()) {
                if (existHiveDbName.contains(tenantHiveResource.getHiveDatabase())) {
                    throw new IllegalArgumentException("hive数据库已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantHiveResource.getHiveDatabaseCapacity())) {
                    throw new IllegalArgumentException("hive配额设置不合法");
                }
            }
        }

        // 校验hbase
        if (CollUtil.isNotEmpty(clusterTenant.getHbaseResourceList())) {
            List<String> existHbaseNameSpace = tenantList.stream()
                    .map(ClusterTenant::getHbaseResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hbaseNamespace"))
                    .collect(Collectors.toList());
            for (com.datasophon.dao.entity.tenantResource.TenantHbaseResource tenantHbaseResource : clusterTenant.getHbaseResourceList()) {
                if (existHbaseNameSpace.contains(tenantHbaseResource.getHbaseNamespace())) {
                    throw new IllegalArgumentException("hbase namespace已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantHbaseResource.getHbaseCapacity()) || !StrUtil.isNumeric(tenantHbaseResource.getHbaseRegionServerNum())) {
                    throw new IllegalArgumentException("hbase配额设置不合法");
                }
            }
        }

        // 校验kafka
        if (CollUtil.isNotEmpty(clusterTenant.getKafkaResourceList())) {
            List<String> existKafkaTopic = tenantList.stream()
                    .map(ClusterTenant::getKafkaResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("kafkaTopicName"))
                    .collect(Collectors.toList());
            for (com.datasophon.dao.entity.tenantResource.TenantKafkaResource tenantKafkaResource : clusterTenant.getKafkaResourceList()) {
                if (existKafkaTopic.contains(tenantKafkaResource.getKafkaTopicName())) {
                    throw new IllegalArgumentException("kafka topic已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantKafkaResource.getKafkaReplicas()) || !StrUtil.isNumeric(tenantKafkaResource.getKafkaTopicCapacity())) {
                    throw new IllegalArgumentException("kafka配额设置不合法");
                }
            }
        }
    }

    public <T> List<LinkedHashMap<String, String>> convertToMap(List<T> list) {
        return Convert.convert(new TypeReference<List<LinkedHashMap<String, String>>>() {}, list);
    }

}