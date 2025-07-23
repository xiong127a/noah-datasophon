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

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterVariableService;
import com.datasophon.dao.entity.ClusterVariable;
import com.datasophon.dao.mapper.ClusterVariableMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("clusterVariableService")
public class ClusterVariableServiceImpl extends ServiceImpl<ClusterVariableMapper, ClusterVariable>
        implements
        ClusterVariableService {

    @Override
    public ClusterVariable getVariableByVariableName(String variableName, Integer clusterId) {
        List<ClusterVariable> list = QueryChain.of(ClusterVariable.class)
                .where(ClusterVariable::getVariableName).eq(variableName)
                .and(ClusterVariable::getClusterId).eq(clusterId)
                .list();

        if (CollUtil.isNotEmpty(list)) {
            return list.getFirst();
        }
        return null;
    }
}
