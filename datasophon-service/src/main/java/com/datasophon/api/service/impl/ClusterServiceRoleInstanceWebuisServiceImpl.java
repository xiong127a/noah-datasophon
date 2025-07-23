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

import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceWebuisMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("clusterServiceRoleInstanceWebuisService")
public class ClusterServiceRoleInstanceWebuisServiceImpl
        extends
        ServiceImpl<ClusterServiceRoleInstanceWebuisMapper, ClusterServiceRoleInstanceWebuis>
        implements
        ClusterServiceRoleInstanceWebuisService {

    private static final String ACTIVE = "(Active)";

    private static final String STANDBY = "(Standby)";

    @Override
    public Result getWebUis(Integer serviceInstanceId) {
        List<ClusterServiceRoleInstanceWebuis> list = QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceInstanceId).eq(serviceInstanceId)
                .list();
        return Result.success(list);
    }

    @Override
    public void removeByServiceInsId(Integer serviceInstanceId) {
        this.remove(QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceInstanceId).eq(serviceInstanceId));
    }

    @Override
    public void updateWebUiToActive(Integer roleInstanceId) {
        updateWebUiName(roleInstanceId, ACTIVE);
    }

    @Override
    public ClusterServiceRoleInstanceWebuis getRoleInstanceWebUi(Integer roleInstanceId) {
        return QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceRoleInstanceId).eq(roleInstanceId)
                .one();
    }

    @Override
    public void removeByRoleInsIds(ArrayList<Integer> needRemoveList) {
        this.remove(QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceRoleInstanceId).in(needRemoveList));
    }

    @Override
    public void updateWebUiToStandby(Integer roleInstanceId) {
        updateWebUiName(roleInstanceId, STANDBY);
    }

    @Override
    public List<ClusterServiceRoleInstanceWebuis> listWebUisByServiceInstanceId(Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceInstanceId).eq(serviceInstanceId)
                .list();
    }

    private void updateWebUiName(Integer roleInstanceId, String state) {
        List<ClusterServiceRoleInstanceWebuis> webuisList = QueryChain.of(ClusterServiceRoleInstanceWebuis.class)
                .where(ClusterServiceRoleInstanceWebuis::getServiceRoleInstanceId).eq(roleInstanceId)
                .list();

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
