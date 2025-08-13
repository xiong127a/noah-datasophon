/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service.impl;

import com.datasophon.api.converter.ClusterVariableConverter;
import com.datasophon.api.service.ClusterVariableManagementService;
import com.datasophon.api.service.ClusterVariableService;
import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.dao.entity.ClusterVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import static com.datasophon.api.utils.CacheOperateUtils.putRemoteVariableCache;

/**
 * 集群变量管理服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service
public class ClusterVariableManagementServiceImpl implements ClusterVariableManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterVariableManagementServiceImpl.class);

    @Autowired
    private ClusterVariableService variableService;

    @Autowired
    private ClusterVariableConverter variableConverter;

    @Override
    public void generateClusterVariable(Map<String, String> globalVariables, Long clusterId,
            String variableName, String value) {
        ClusterVariableDTO clusterVariableDTO = variableService.getVariableByVariableName(variableName, clusterId);
        if (Objects.nonNull(clusterVariableDTO)) {
            logger.info("update variable {} value {} to {}", variableName, clusterVariableDTO.variableValue(), value);
            ClusterVariableEntity clusterVariableEntity = variableConverter.dtoToEntity(clusterVariableDTO);
            clusterVariableEntity.setVariableValue(value);
            variableService.updateById(clusterVariableEntity);
        } else {
            ClusterVariableEntity newClusterVariableEntity = new ClusterVariableEntity();
            newClusterVariableEntity.setClusterId(clusterId);
            newClusterVariableEntity.setVariableName(variableName);
            newClusterVariableEntity.setVariableValue(value);
            variableService.save(newClusterVariableEntity);
        }
        globalVariables.put(variableName, value);
        putRemoteVariableCache(variableName, value, clusterId);
    }
}