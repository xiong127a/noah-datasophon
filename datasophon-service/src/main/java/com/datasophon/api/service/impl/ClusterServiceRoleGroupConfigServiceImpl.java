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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.mapper.ClusterServiceRoleGroupConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("clusterServiceRoleGroupConfigService")
public class ClusterServiceRoleGroupConfigServiceImpl
        extends
        ServiceImpl<ClusterServiceRoleGroupConfigMapper, ClusterServiceRoleGroupConfig>
        implements
        ClusterServiceRoleGroupConfigService {

    @Override
    public ClusterServiceRoleGroupConfig getConfigByRoleGroupId(Integer roleGroupId) {
        LambdaQueryWrapper<ClusterServiceRoleGroupConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClusterServiceRoleGroupConfig::getRoleGroupId, roleGroupId)
                .orderByDesc(ClusterServiceRoleGroupConfig::getConfigVersion);
        return this.getOne(queryWrapper,false);
    }

    @Override
    public ClusterServiceRoleGroupConfig getConfigByRoleGroupIdAndVersion(Integer roleGroupId, Integer version) {
        LambdaQueryWrapper<ClusterServiceRoleGroupConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClusterServiceRoleGroupConfig::getRoleGroupId, roleGroupId)
                .eq(ClusterServiceRoleGroupConfig::getConfigVersion, version);
        return this.getOne(queryWrapper);
    }

    @Override
    public void removeAllByRoleGroupId(Integer roleGroupId) {
        LambdaQueryWrapper<ClusterServiceRoleGroupConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClusterServiceRoleGroupConfig::getRoleGroupId, roleGroupId);
        this.remove(queryWrapper);
    }

    @Override
    public List<ClusterServiceRoleGroupConfig> listRoleGroupConfigsByRoleGroupIds(List<Integer> roleGroupIds) {
        LambdaQueryWrapper<ClusterServiceRoleGroupConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ClusterServiceRoleGroupConfig::getRoleGroupId, roleGroupIds);
        return this.list(queryWrapper);
    }
}
