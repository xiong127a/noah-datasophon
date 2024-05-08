package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.*;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.TenantResource.*;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.datasophon.common.enums.RangerOpType.DELETE_TENANT;

@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenant> implements ClusterTenantService {

//    private static final Cache<String, ActorRef> actorRefCache = CacheUtil.newFIFOCache(10);

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    @Autowired
    private ClusterUserService clusterUserService;

    @Override
    public Result listTenant(Integer clusterId, Integer page, Integer size, String tenantName) {
        int offset = (page - 1) * size;
        QueryWrapper<ClusterTenant> queryWrapper = new QueryWrapper<ClusterTenant>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .like(StrUtil.isNotBlank(tenantName), "tenant_name", tenantName)
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

        filterDeleteResource(clusterTenant);

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
        Map<String, String> globalVariables = GlobalVariables.get(clusterTenant.getClusterId());
        ActorRef tenantResourceDispatcherActor = ActorUtils.getLocalActor(TenantResourceDispatcherActor.class, "tenantResourceDispatcherActor");
        for (TenantFrameResource tenantFrameResource : allFrameResource) {
            String enableKerberos = globalVariables.get("${enable" + tenantFrameResource.getServiceName() + "Kerberos}");
            tenantFrameResource.setEnableKerberos(StrUtil.isNotEmpty(enableKerberos) && "true".equals(enableKerberos));
            tenantResourceDispatcherActor.tell(tenantFrameResource, ActorRef.noSender());
        }

        // ranger策略操作
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(resource, ActorRef.noSender());

        this.saveOrUpdate(clusterTenant);

        return Result.success();
    }

    private void filterDeleteResource(ClusterTenant clusterTenant) {
        clusterTenant.getHdfsResourceList().removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getYarnResourceList().removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getKafkaResourceList().removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getHiveResourceList().removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getHbaseResourceList().removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
    }

    @Override
    public Result deleteTenantById(Integer id) {
        // 是否授权授权用户校验
        List<ClusterUserTenant> userTenantList = clusterUserTenantService.lambdaQuery()
                .eq(ClusterUserTenant::getTenantId, id)
                .list();
        if (CollUtil.isNotEmpty(userTenantList)) {
            List<Integer> userIds = userTenantList.stream().map(ClusterUserTenant::getUserId).collect(Collectors.toList());
            List<String> usernames = clusterUserService.lambdaQuery()
                    .in(ClusterUser::getId, userIds)
                    .list()
                    .stream()
                    .map(ClusterUser::getUsername)
                    .collect(Collectors.toList());
            return Result.error("当前租户已经授权给用户：" + usernames + ", 请先取消授权");
        }

        ClusterTenant tenant = this.getById(id);
        TenantRangerCommand command = new TenantRangerCommand();
        command.setClusterId(tenant.getClusterId());
        command.setTenantName(tenant.getTenantName());
        command.setOperateType(DELETE_TENANT);

        // 删除所有ranger相关策略及角色
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(command, ActorRef.noSender());

        if (this.removeById(id)) {
            return Result.success();
        }
        return Result.error();
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
        return Convert.convert(new TypeReference<List<LinkedHashMap<String, String>>>() {
        }, list);
    }

}