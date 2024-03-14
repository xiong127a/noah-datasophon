package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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

import java.util.List;
import java.util.stream.Collectors;

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

}