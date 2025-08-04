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

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceWebuisMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群服务角色实例WebUI服务实现
 * 按照架构重构规范，ServiceImpl返回DTO/Entity，不返回Result
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterServiceRoleInstanceWebuisService")
public class ClusterServiceRoleInstanceWebuisServiceImpl
        extends ServiceImpl<ClusterServiceRoleInstanceWebuisMapper, ClusterServiceRoleInstanceWebuis>
        implements ClusterServiceRoleInstanceWebuisService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceRoleInstanceWebuisServiceImpl.class);

    private static final String ACTIVE = "(Active)";

    private static final String STANDBY = "(Standby)";

    @Override
    public List<ClusterServiceRoleInstanceWebuis> getWebUis(Integer serviceInstanceId) {
        return getMapper().selectByServiceInstanceId(serviceInstanceId);
    }

    @Override
    public void removeByServiceInsId(Integer serviceInstanceId) {
        getMapper().deleteByServiceInstanceId(serviceInstanceId);
    }

    @Override
    public void updateWebUiToActive(Integer roleInstanceId) {
        updateWebUiName(roleInstanceId, ACTIVE);
    }

    @Override
    public ClusterServiceRoleInstanceWebuis getRoleInstanceWebUi(Integer roleInstanceId) {
        return getMapper().selectByServiceRoleInstanceId(roleInstanceId);
    }

    @Override
    public void removeByRoleInsIds(ArrayList<Integer> needRemoveList) {
        getMapper().deleteByServiceRoleInstanceIds(needRemoveList);
    }

    @Override
    public void updateWebUiToStandby(Integer roleInstanceId) {
        updateWebUiName(roleInstanceId, STANDBY);
    }

    @Override
    public List<ClusterServiceRoleInstanceWebuis> listWebUisByServiceInstanceId(Integer serviceInstanceId) {
        return getMapper().selectByServiceInstanceId(serviceInstanceId);
    }

    private void updateWebUiName(Integer roleInstanceId, String state) {
        List<ClusterServiceRoleInstanceWebuis> webuisList = getMapper().selectListByServiceRoleInstanceId(roleInstanceId);

        if (webuisList.isEmpty()) {
            return;
        }

        for (ClusterServiceRoleInstanceWebuis webuis : webuisList) {
            String webuiName = webuis.getName();
            boolean needUpdate = false;
            
            if (webuiName.contains(ACTIVE) && STANDBY.equals(state)) {
                webuiName = webuiName.replace(ACTIVE, STANDBY);
                needUpdate = true;
            }
            
            if (webuiName.contains(STANDBY) && ACTIVE.equals(state)) {
                webuiName = webuiName.replace(STANDBY, ACTIVE);
                needUpdate = true;
            }
            
            webuis.setName(webuiName);
            
            if (!webuiName.contains(ACTIVE) && !webuiName.contains(STANDBY)) {
                webuis.setName(webuis.getName() + state);
                needUpdate = true;
            }
            
            if (needUpdate) {
                this.updateById(webuis);
            }
        }
    }
}
