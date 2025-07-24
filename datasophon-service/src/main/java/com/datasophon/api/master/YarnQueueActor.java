package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.mybatisflex.core.query.QueryChain;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantYarnResource;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.entity.ClusterYarnScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class YarnQueueActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(YarnQueueActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TenantFrameResource.class, this::handleTenantFrameResource)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleTenantFrameResource(TenantFrameResource message) {
        try {
            if (message instanceof TenantYarnResource tenantYarnResource) {
                ClusterYarnSchedulerService clusterYarnSchedulerService = SpringUtil
                        .getBean(ClusterYarnSchedulerService.class);
                ClusterYarnScheduler scheduler = clusterYarnSchedulerService
                        .getScheduler(tenantYarnResource.getClusterId());
                if ("capacity".equals(scheduler.getScheduler())) {
                    operateCapacityQueue(tenantYarnResource);
                } else {
                    unhandled(message);
                }
            } else {
                unhandled(message);
            }
        } catch (Exception e) {
            logger.error("Error handling TenantFrameResource", e);
        }
    }

    private void operateCapacityQueue(TenantYarnResource tenantYarnResource) throws Exception {
        TROperateType trOperateType = TROperateType.valueOf(tenantYarnResource.getType());
        switch (trOperateType) {
            case ADD:
                createCapacityYarnQueue(tenantYarnResource, tenantYarnResource.getClusterId());
                break;
            case UPDATE:
                updateCapacityYarnQueue(tenantYarnResource, tenantYarnResource.getClusterId());
                break;
            case DELETE:
                deleteCapacityYarnQueue(tenantYarnResource, tenantYarnResource.getClusterId());
                break;
        }
    }

    private void createCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);

        List<ClusterQueueCapacity> list = QueryChain.of(ClusterQueueCapacity.class)
                .where(ClusterQueueCapacity::getClusterId).eq(clusterId)
                .and(ClusterQueueCapacity::getParent).eq(yarnResource.getParentQueueName())
                .and(ClusterQueueCapacity::getQueueName).eq(yarnResource.getQueueName())
                .list();

        if (CollUtil.isNotEmpty(list)) {
            logger.error("当前队列已经存在");
            return;
        }

        ClusterQueueCapacity clusterQueueCapacity = new ClusterQueueCapacity();
        clusterQueueCapacity.setQueueName(yarnResource.getQueueName());
        clusterQueueCapacity.setClusterId(clusterId);
        clusterQueueCapacity.setParent(yarnResource.getParentQueueName());
        clusterQueueCapacity.setCapacity(yarnResource.getCapacityPercent());
        clusterQueueCapacity.setNodeLabel(yarnResource.getNodeLabel());
        clusterQueueCapacity.setAclUsers("");

        clusterQueueCapacityService.save(clusterQueueCapacity);
        clusterQueueCapacityService.refreshToYarn(clusterId);
        logger.info("创建yarn队列 {} 成功,请手动去yarn资源页面配置队列", yarnResource.getQueueName());
    }

    private void updateCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);
        ClusterQueueCapacity queue = QueryChain.of(ClusterQueueCapacity.class)
                .where(ClusterQueueCapacity::getClusterId).eq(clusterId)
                .and(ClusterQueueCapacity::getParent).eq(yarnResource.getParentQueueName())
                .and(ClusterQueueCapacity::getQueueName).eq(yarnResource.getQueueName())
                .one();

        queue.setCapacity(yarnResource.getCapacityPercent());
        queue.setNodeLabel(yarnResource.getNodeLabel());

        clusterQueueCapacityService.saveOrUpdate(queue);
        Result result = clusterQueueCapacityService.refreshToYarn(clusterId);
        if (result.isSuccess()) {
            logger.info("update yarn queue {} success", yarnResource.getQueueName());
        } else {
            logger.error("update yarn queue {} failed --> {}", yarnResource.getQueueName(), result.getMsg());
        }
    }

    private void deleteCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);
        ClusterQueueCapacity queue = QueryChain.of(ClusterQueueCapacity.class)
                .where(ClusterQueueCapacity::getClusterId).eq(clusterId)
                .and(ClusterQueueCapacity::getParent).eq(yarnResource.getParentQueueName())
                .and(ClusterQueueCapacity::getQueueName).eq(yarnResource.getQueueName())
                .one();

        clusterQueueCapacityService.removeById(queue.getId());
        clusterQueueCapacityService.refreshToYarn(clusterId);
        logger.info("delete yarn queue {} success , please restart yarn", yarnResource.getQueueName());
    }

}
