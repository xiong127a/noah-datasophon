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

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.mapper.FrameServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.mybatisflex.core.query.QueryChain;

@Service("frameServiceService")
public class FrameServiceServiceImpl extends ServiceImpl<FrameServiceMapper, FrameServiceEntity>
        implements
        FrameServiceService {

    ClusterInfoService clusterInfoService;

    FrameInfoMapper frameInfoMapper;

    final
    ClusterServiceInstanceService serviceInstanceService;

    private final static List<String> CUSTOM_REQUIRED_SERVICE = Arrays.asList(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER");

    private final static List<String> DATALAKE_REQUIRED_SERVICE = Arrays.asList(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER", "HDFS", "YARN", "HUDI", "HIVE", "ICEBERG",
            "SPARK3", "FLINK");

    public FrameServiceServiceImpl(ClusterServiceInstanceService serviceInstanceService) {
        this.serviceInstanceService = serviceInstanceService;
    }

    @Autowired
    public FrameServiceServiceImpl(FrameInfoMapper frameInfoMapper) {
        this.frameInfoMapper = frameInfoMapper;
    }

    @Autowired
    public FrameServiceServiceImpl(ClusterInfoService clusterInfoService) {
        this.clusterInfoService = clusterInfoService;
    }

    @Override
    public Result getAllFrameService(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());

        List<FrameServiceEntity> list = QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameId).eq(frameInfo.getId())
                .orderBy(FrameServiceEntity::getSortNum).asc()
                .list();

        setInstalled(clusterId, list);
        return Result.success(list);
    }

    @Override
    public Result getAllFrameServiceWithRequired(Integer clusterId, String type) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());

        List<FrameServiceEntity> list = QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameId).eq(frameInfo.getId())
                .orderBy(FrameServiceEntity::getSortNum).asc()
                .list();

        setInstalled(clusterId, list);
        setRequired(list, type);
        return Result.success(list);
    }

    private void setRequired(List<FrameServiceEntity> list, String type) {
        List<String> requireService = type.equals("custom") ? CUSTOM_REQUIRED_SERVICE : DATALAKE_REQUIRED_SERVICE;
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
    public Result getServiceListByServiceIds(List<Integer> serviceIds) {
        Collection<FrameServiceEntity> list = this.listByIds(serviceIds);
        return Result.success(list);
    }

    @Override
    public FrameServiceEntity getServiceByFrameIdAndServiceName(Integer frameId, String serviceName) {
        return QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameId).eq(frameId)
                .and(FrameServiceEntity::getServiceName).eq(serviceName)
                .one();
    }

    @Override
    public FrameServiceEntity getServiceByFrameCodeAndServiceName(String clusterFrame, String serviceName) {
        return QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameCode).eq(clusterFrame)
                .and(FrameServiceEntity::getServiceName).eq(serviceName)
                .one();
    }

    @Override
    public List<FrameServiceEntity> getAllFrameServiceByFrameCode(String clusterFrame) {
        return QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameCode).eq(clusterFrame)
                .list();
    }

    @Override
    public List<FrameServiceEntity> listServices(String serviceIds) {
        List<String> ids = Arrays.stream(serviceIds.split(",")).collect(Collectors.toList());
        return QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getId).in(ids)
                .list();
    }

}
