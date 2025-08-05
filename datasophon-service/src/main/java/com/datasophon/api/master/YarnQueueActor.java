package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantYarnResource;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                ClusterYarnSchedulerDTO scheduler = clusterYarnSchedulerService
                        .getScheduler(tenantYarnResource.getClusterId());
                if (scheduler != null && "capacity".equals(scheduler.scheduler())) {
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
            case NONE:
                logger.warn("收到NONE操作类型，跳过处理");
                break;
            default:
                logger.warn("未知的操作类型: {}", trOperateType);
                break;
        }
    }

    private void createCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);

        // 通过Service层检查队列是否已存在
        try {
            ClusterQueueCapacity existingQueue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                    clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());
            if (existingQueue != null) {
                logger.error("当前队列已经存在");
                return;
            }
        } catch (Exception e) {
            logger.debug("队列不存在，可以创建");
        }

        ClusterQueueCapacity clusterQueueCapacity = new ClusterQueueCapacity();
        clusterQueueCapacity.setQueueName(yarnResource.getQueueName());
        clusterQueueCapacity.setClusterId(clusterId);
        clusterQueueCapacity.setParent(yarnResource.getParentQueueName());
        clusterQueueCapacity.setCapacity(yarnResource.getCapacityPercent());
        clusterQueueCapacity.setNodeLabel(yarnResource.getNodeLabel());
        clusterQueueCapacity.setAclUsers("");

        clusterQueueCapacityService.save(clusterQueueCapacity);
        boolean refreshResult = clusterQueueCapacityService.refreshToYarn(clusterId);
        if (refreshResult) {
            logger.info("创建yarn队列 {} 成功,请手动去yarn资源页面配置队列", yarnResource.getQueueName());
        } else {
            logger.error("创建yarn队列 {} 后刷新到Yarn失败", yarnResource.getQueueName());
        }
    }

    private void updateCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);

        // 通过Service层获取队列
        ClusterQueueCapacity queue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());

        if (queue == null) {
            logger.error("要更新的yarn队列 {} 不存在", yarnResource.getQueueName());
            return;
        }

        queue.setCapacity(yarnResource.getCapacityPercent());
        queue.setNodeLabel(yarnResource.getNodeLabel());

        clusterQueueCapacityService.saveOrUpdate(queue);
        boolean refreshResult = clusterQueueCapacityService.refreshToYarn(clusterId);
        if (refreshResult) {
            logger.info("update yarn queue {} success", yarnResource.getQueueName());
        } else {
            logger.error("update yarn queue {} failed", yarnResource.getQueueName());
        }
    }

    private void deleteCapacityYarnQueue(TenantYarnResource yarnResource, Integer clusterId) throws Exception {
        ClusterQueueCapacityService clusterQueueCapacityService = SpringUtil.getBean(ClusterQueueCapacityService.class);

        // 通过Service层获取队列
        ClusterQueueCapacity queue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());

        if (queue == null) {
            logger.error("要删除的yarn队列 {} 不存在", yarnResource.getQueueName());
            return;
        }

        clusterQueueCapacityService.removeById(queue.getId());
        boolean refreshResult = clusterQueueCapacityService.refreshToYarn(clusterId);
        if (refreshResult) {
            logger.info("delete yarn queue {} success , please restart yarn", yarnResource.getQueueName());
        } else {
            logger.error("delete yarn queue {} 后刷新到Yarn失败", yarnResource.getQueueName());
        }
    }

}
