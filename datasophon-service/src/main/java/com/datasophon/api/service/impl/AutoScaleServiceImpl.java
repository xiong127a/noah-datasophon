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
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.AutoScaleService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.AutoScaleTaskVO;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.kubernetes.util.KubernetesUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 自动伸缩服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service
public class AutoScaleServiceImpl implements AutoScaleService {

    private static final String SEATUNNEL_SERVER_NAME = "seatunnel-seatunnelserver";
    private static final int DEFAULT_SCALE_UP_REPLICAS = 3;
    private static final int DEFAULT_SCALE_DOWN_REPLICAS = 1;

    private ClusterInfoService getClusterInfoService() {
        return SpringUtil.getBean(ClusterInfoService.class);
    }

    private boolean isAutoScaleEnabled(int clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enableAutoScale}"));
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void scaleUp() {
        int clusterId = PropertyUtils.getInt("clusterId");
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

    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void scaleDown() {
        int clusterId = PropertyUtils.getInt("clusterId");
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

    }

    @Override
    public boolean createAutoScaleTask(AutoScaleTaskVO taskVO) {
        try {
            // saveAutoScaleConfig(taskVO.getClusterId(), taskVO.getScaleType());
            saveAutoScaleConfig(taskVO.getClusterId(), "true");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auto scale task", e);
        }
    }

    @Override
    public boolean updateAutoScaleTask(AutoScaleTaskVO taskVO) {
        try {
            // saveAutoScaleConfig(taskVO.getClusterId(), taskVO.getScaleType());
            saveAutoScaleConfig(taskVO.getClusterId(), "false");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update auto scale task", e);
        }
    }

    private void saveAutoScaleConfig(Integer clusterId, String scaleType) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableAutoScale}", scaleType);
    }

    @Override
    public String getAutoScaleTasks(AutoScaleTaskVO taskVO) {
        Map<String, String> globalVariables = GlobalVariables.get(taskVO.getClusterId());
        return globalVariables.get("${enableAutoScale}") != null ? globalVariables.get("${enableAutoScale}") : "false";
    }

    @Override
    public boolean deleteAutoScaleTask(AutoScaleTaskVO taskVO) {
        // TODO: 实现删除自动伸缩任务逻辑
        return false;
    }
}