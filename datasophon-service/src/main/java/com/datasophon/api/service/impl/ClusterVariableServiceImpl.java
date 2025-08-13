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

import com.datasophon.api.converter.ClusterVariableConverter;
import com.datasophon.api.service.ClusterVariableService;
import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.dao.entity.ClusterVariableEntity;
import com.datasophon.dao.mapper.ClusterVariableMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 集群变量服务实现
 * 继承ServiceImpl提供基础CRUD操作，使用Converter进行对象转换
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service
public class ClusterVariableServiceImpl extends ServiceImpl<ClusterVariableMapper, ClusterVariableEntity>
        implements ClusterVariableService {

    @Autowired
    private ClusterVariableConverter clusterVariableConverter;

    @Override
    public ClusterVariableDTO getVariableByVariableName(String variableName, Long clusterId) {
        ClusterVariableEntity entity = getMapper().getVariableByVariableName(variableName, clusterId);
        return entity != null ? clusterVariableConverter.entityToDto(entity) : null;
    }
}