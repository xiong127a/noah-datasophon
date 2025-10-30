/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.tenant.resource.TenantYarnResource;
import com.datasophon.dao.entity.ClusterQueueCapacityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Yarn队列管理服务实现
 * 替代YarnQueueActor，处理Yarn队列的增删改操作
 */
@Service
public class YarnQueueServiceImpl implements YarnQueueService {

    private static final Logger logger = LoggerFactory.getLogger(YarnQueueServiceImpl.class);

    @Autowired
    private ClusterYarnSchedulerService clusterYarnSchedulerService;

    @Autowired
    private ClusterQueueCapacityService clusterQueueCapacityService;

    @Override
    @Async("taskExecutor")
    public void handleTenantYarnResource(TenantYarnResource tenantYarnResource) {
        try {
            ClusterYarnSchedulerDTO scheduler = clusterYarnSchedulerService
                    .getScheduler(tenantYarnResource.getClusterId());
            if (scheduler != null && "capacity".equals(scheduler.scheduler())) {
                operateCapacityQueue(tenantYarnResource);
            } else {
                logger.warn("不支持的调度器类型或调度器未配置");
            }
        } catch (Exception e) {
            logger.error("处理TenantYarnResource时出错", e);
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

    private void createCapacityYarnQueue(TenantYarnResource yarnResource, Long clusterId) throws Exception {
        try {
            ClusterQueueCapacityEntity existingQueue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                    clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());
            if (existingQueue != null) {
                logger.error("当前队列已经存在");
                return;
            }
        } catch (Exception e) {
            logger.warn("检查队列是否存在时出错", e);
        }

        ClusterQueueCapacityEntity queueCapacityEntity = new ClusterQueueCapacityEntity();
        queueCapacityEntity.setClusterId(clusterId);
        queueCapacityEntity.setQueueName(yarnResource.getQueueName());
        queueCapacityEntity.setParentQueueName(yarnResource.getParentQueueName());
        queueCapacityEntity.setCapacity(yarnResource.getCapacity());
        queueCapacityEntity.setMaxCapacity(yarnResource.getMaxCapacity());

        clusterQueueCapacityService.save(queueCapacityEntity);
        logger.info("成功创建Yarn队列: {}", yarnResource.getQueueName());
    }

    private void updateCapacityYarnQueue(TenantYarnResource yarnResource, Long clusterId) throws Exception {
        ClusterQueueCapacityEntity queue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());

        if (queue == null) {
            logger.error("队列不存在，无法更新: {}", yarnResource.getQueueName());
            return;
        }

        queue.setCapacity(yarnResource.getCapacity());
        queue.setMaxCapacity(yarnResource.getMaxCapacity());

        clusterQueueCapacityService.updateById(queue);
        logger.info("成功更新Yarn队列: {}", yarnResource.getQueueName());
    }

    private void deleteCapacityYarnQueue(TenantYarnResource yarnResource, Long clusterId) throws Exception {
        ClusterQueueCapacityEntity queue = clusterQueueCapacityService.getByClusterIdAndQueueName(
                clusterId, yarnResource.getQueueName(), yarnResource.getParentQueueName());

        if (queue == null) {
            logger.error("队列不存在，无法删除: {}", yarnResource.getQueueName());
            return;
        }

        clusterQueueCapacityService.removeById(queue.getId());
        logger.info("成功删除Yarn队列: {}", yarnResource.getQueueName());
    }
}

