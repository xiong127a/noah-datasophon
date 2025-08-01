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

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.mapper.FrameServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 集群框架版本服务表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("frameServiceService")
public class FrameServiceServiceImpl implements FrameServiceService {

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private FrameInfoMapper frameInfoMapper;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private FrameServiceMapper frameServiceMapper;

    private static final List<String> CUSTOM_REQUIRED_SERVICE = Arrays.asList(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER");

    private static final List<String> DATALAKE_REQUIRED_SERVICE = Arrays.asList(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER", "HDFS", "YARN", "HUDI", "HIVE", "ICEBERG",
            "SPARK3", "FLINK");

    @Override
    public List<FrameServiceEntity> getAllFrameService(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (clusterInfo == null) {
            throw new RuntimeException("Cluster not found with id: " + clusterId);
        }
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());
        if (frameInfo == null) {
            throw new RuntimeException("Frame info not found for cluster frame: " + clusterInfo.getClusterFrame());
        }

        List<FrameServiceEntity> list = frameServiceMapper.selectByFrameIdOrderBySortNum(frameInfo.getId());
        setInstalled(clusterId, list);
        return list;
    }

    @Override
    public List<FrameServiceEntity> getAllFrameServiceWithRequired(Integer clusterId, String type) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (clusterInfo == null) {
            throw new RuntimeException("Cluster not found with id: " + clusterId);
        }
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());
        if (frameInfo == null) {
            throw new RuntimeException("Frame info not found for cluster frame: " + clusterInfo.getClusterFrame());
        }

        List<FrameServiceEntity> list = frameServiceMapper.selectByFrameIdOrderBySortNum(frameInfo.getId());
        setInstalled(clusterId, list);
        setRequired(list, type);
        return list;
    }

    private void setRequired(List<FrameServiceEntity> list, String type) {
        List<String> requireService = "custom".equals(type) ? CUSTOM_REQUIRED_SERVICE : DATALAKE_REQUIRED_SERVICE;
        for (FrameServiceEntity frameServiceEntity : list) {
            frameServiceEntity.setIsRequired(requireService.contains(frameServiceEntity.getServiceName()));
        }
    }

    private void setInstalled(Integer clusterId, List<FrameServiceEntity> list) {
        for (FrameServiceEntity serviceEntity : list) {
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService
                    .getServiceInstanceByClusterIdAndServiceName(clusterId, serviceEntity.getServiceName());
            serviceEntity.setInstalled(Objects.nonNull(serviceInstance)
                    && !serviceInstance.getServiceState().equals(ServiceState.WAIT_INSTALL));
        }
    }

    @Override
    public List<FrameServiceEntity> getServiceListByServiceIds(List<Integer> serviceIds) {
        return frameServiceMapper.selectByIds(serviceIds);
    }

    @Override
    public FrameServiceEntity getServiceByFrameIdAndServiceName(Integer frameId, String serviceName) {
        return frameServiceMapper.selectByFrameIdAndServiceName(frameId, serviceName);
    }

    @Override
    public FrameServiceEntity getServiceByFrameCodeAndServiceName(String clusterFrame, String serviceName) {
        return frameServiceMapper.selectByFrameCodeAndServiceName(clusterFrame, serviceName);
    }

    @Override
    public List<FrameServiceEntity> getAllFrameServiceByFrameCode(String clusterFrame) {
        return frameServiceMapper.selectByFrameCode(clusterFrame);
    }

    @Override
    public List<FrameServiceEntity> listServices(String serviceIds) {
        List<String> ids = Arrays.stream(serviceIds.split(",")).collect(java.util.stream.Collectors.toList());
        return frameServiceMapper.selectByStringIds(ids);
    }

    // 标准CRUD方法实现
    @Override
    public FrameServiceEntity getById(Integer id) {
        return frameServiceMapper.selectById(id);
    }

    @Override
    public FrameServiceEntity save(FrameServiceEntity entity) {
        frameServiceMapper.insert(entity);
        return entity;
    }

    @Override
    public FrameServiceEntity updateById(FrameServiceEntity entity) {
        frameServiceMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return frameServiceMapper.deleteByIds(ids) > 0;
    }

}
