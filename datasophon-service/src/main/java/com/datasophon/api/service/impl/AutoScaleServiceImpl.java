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

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.converter.AutoScaleTaskConverter;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.AutoScaleService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.dto.AutoScaleTaskDTO;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.AutoScaleTaskEntity;
import com.datasophon.dao.mapper.AutoScaleTaskMapper;
import com.datasophon.kubernetes.util.KubernetesUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自动伸缩服务实现
 * 按照架构重构规范，迁移QueryChain到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service
public class AutoScaleServiceImpl extends ServiceImpl<AutoScaleTaskMapper, AutoScaleTaskEntity>
        implements AutoScaleService {

    private static final Logger logger = LoggerFactory.getLogger(AutoScaleServiceImpl.class);

    private static final String SEATUNNEL_SERVER_NAME = "seatunnel-seatunnelserver";
    private static final int DEFAULT_SCALE_UP_REPLICAS = 3;
    private static final int DEFAULT_SCALE_DOWN_REPLICAS = 1;

    @Autowired
    private AutoScaleTaskConverter autoScaleTaskConverter;

    private ClusterInfoService getClusterInfoService() {
        return SpringUtil.getBean(ClusterInfoService.class);
    }

    @Override
    public boolean isAutoScaleEnabled(Long clusterId) {
        if (clusterId == null) {
            return false;
        }
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enableAutoScale}"));
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void scaleUp() {
        Long clusterId = PropertyUtils.getLong("clusterId");
        if (BooleanUtil.isFalse(isAutoScaleEnabled(clusterId))) {
            return;
        }
        String kubeConfig = getClusterInfoService().getKubeConfigByClusterId(clusterId);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        KubernetesUtil.scaleStatefulSet(
                kubeConfig,
                namespace,
                SEATUNNEL_SERVER_NAME,
                DEFAULT_SCALE_UP_REPLICAS,
                "工作日早9点扩容");
        logger.info("执行工作日9点自动扩容，集群ID: {}", clusterId);
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void scaleDown() {
        Long clusterId = PropertyUtils.getLong("clusterId");
        if (BooleanUtil.isFalse(isAutoScaleEnabled(clusterId))) {
            return;
        }
        String kubeConfig = getClusterInfoService().getKubeConfigByClusterId(clusterId);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        KubernetesUtil.scaleStatefulSet(
                kubeConfig,
                namespace,
                SEATUNNEL_SERVER_NAME,
                DEFAULT_SCALE_DOWN_REPLICAS,
                "工作日晚6点缩容");
        logger.info("执行工作日18点自动缩容，集群ID: {}", clusterId);
    }

    @Override
    public AutoScaleTaskDTO createAutoScaleTask(AutoScaleTaskDTO taskDTO) {
        try {
            // 转换DTO为Entity
            AutoScaleTaskEntity entity = autoScaleTaskConverter.dtoToEntity(taskDTO);

            // 设置创建和更新时间
            Date now = new Date();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            // 保存到数据库
            boolean success = this.save(entity);
            if (!success) {
                throw new BusinessException(500, "创建自动伸缩任务失败");
            }

            // 更新全局配置（向后兼容）
            if (entity.getEnabled()) {
                saveAutoScaleConfig(entity.getClusterId(), "true");
            }

            logger.info("创建自动伸缩任务成功，任务ID: {}, 集群ID: {}", entity.getId(), entity.getClusterId());
            return autoScaleTaskConverter.entityToDto(entity);

        } catch (Exception e) {
            logger.error("创建自动伸缩任务失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "创建自动伸缩任务失败: " + e.getMessage());
        }
    }

    @Override
    public AutoScaleTaskDTO updateAutoScaleTask(AutoScaleTaskDTO taskDTO) {
        try {
            if (taskDTO.id() == null) {
                throw new BusinessException(400, "任务ID不能为空");
            }

            // 检查任务是否存在
            AutoScaleTaskEntity existingEntity = this.getById(taskDTO.id());
            if (existingEntity == null) {
                throw new BusinessException(404, "自动伸缩任务不存在");
            }

            // 转换DTO为Entity
            AutoScaleTaskEntity entity = autoScaleTaskConverter.dtoToEntity(taskDTO);
            entity.setUpdatedAt(new Date());

            // 更新到数据库
            boolean success = this.updateById(entity);
            if (!success) {
                throw new BusinessException(500, "更新自动伸缩任务失败");
            }

            // 更新全局配置（向后兼容）
            saveAutoScaleConfig(entity.getClusterId(), entity.getEnabled() ? "true" : "false");

            logger.info("更新自动伸缩任务成功，任务ID: {}", entity.getId());
            return autoScaleTaskConverter.entityToDto(entity);

        } catch (Exception e) {
            logger.error("更新自动伸缩任务失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "更新自动伸缩任务失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<AutoScaleTaskDTO> getAutoScaleTasks(Long clusterId, Integer page, Integer pageSize) {
        try {
            Page<AutoScaleTaskEntity> result = getMapper().selectPageByClusterId(clusterId, page, pageSize);

            List<AutoScaleTaskDTO> dtoList = autoScaleTaskConverter.entityListToDtoList(result.getRecords());
            return PageResult.of(dtoList, result.getTotalRow(), page, pageSize);

        } catch (Exception e) {
            logger.error("查询自动伸缩任务失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "查询自动伸缩任务失败: " + e.getMessage());
        }
    }

    @Override
    public List<AutoScaleTaskDTO> getEnabledTasksByClusterId(Long clusterId) {
        try {
            List<AutoScaleTaskEntity> entities = getMapper().selectEnabledByClusterId(clusterId);
            return autoScaleTaskConverter.entityListToDtoList(entities);

        } catch (Exception e) {
            logger.error("查询启用的自动伸缩任务失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "查询启用的自动伸缩任务失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteAutoScaleTask(Long taskId) {
        try {
            if (taskId == null) {
                throw new BusinessException(400, "任务ID不能为空");
            }

            // 检查任务是否存在
            AutoScaleTaskEntity entity = this.getById(taskId);
            if (entity == null) {
                throw new BusinessException(404, "自动伸缩任务不存在");
            }

            // 删除任务
            boolean success = this.removeById(taskId);
            if (success) {
                logger.info("删除自动伸缩任务成功，任务ID: {}", taskId);

                // 检查该集群是否还有其他启用的任务，如果没有则关闭全局配置
                List<AutoScaleTaskDTO> enabledTasks = getEnabledTasksByClusterId(entity.getClusterId());
                if (enabledTasks.isEmpty()) {
                    saveAutoScaleConfig(entity.getClusterId(), "false");
                }
            }

            return success;

        } catch (Exception e) {
            logger.error("删除自动伸缩任务失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "删除自动伸缩任务失败: " + e.getMessage());
        }
    }

    @Override
    public AutoScaleTaskDTO toggleAutoScaleTask(Long taskId, Boolean enabled) {
        try {
            if (taskId == null) {
                throw new BusinessException(400, "任务ID不能为空");
            }

            AutoScaleTaskEntity entity = this.getById(taskId);
            if (entity == null) {
                throw new BusinessException(404, "自动伸缩任务不存在");
            }

            entity.setEnabled(enabled);
            entity.setUpdatedAt(new Date());

            boolean success = this.updateById(entity);
            if (!success) {
                throw new BusinessException(500, "更新任务状态失败");
            }

            // 更新全局配置
            List<AutoScaleTaskDTO> enabledTasks = getEnabledTasksByClusterId(entity.getClusterId());
            saveAutoScaleConfig(entity.getClusterId(), enabledTasks.isEmpty() ? "false" : "true");

            logger.info("切换自动伸缩任务状态成功，任务ID: {}, 状态: {}", taskId, enabled);
            return autoScaleTaskConverter.entityToDto(entity);

        } catch (Exception e) {
            logger.error("切换自动伸缩任务状态失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "切换自动伸缩任务状态失败: " + e.getMessage());
        }
    }

    private void saveAutoScaleConfig(Long clusterId, String scaleType) {
        try {
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);
            SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${enableAutoScale}", scaleType);
            logger.debug("更新集群 {} 的自动伸缩配置为: {}", clusterId, scaleType);
        } catch (Exception e) {
            logger.warn("更新集群自动伸缩配置失败: {}", e.getMessage());
        }
    }
}