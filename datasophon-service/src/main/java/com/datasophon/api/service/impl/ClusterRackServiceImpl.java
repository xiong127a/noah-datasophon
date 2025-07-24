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

import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.string.validator.GeneralValidator;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterRack;
import com.datasophon.dao.mapper.ClusterRackMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("clusterRackService")
public class ClusterRackServiceImpl extends ServiceImpl<ClusterRackMapper, ClusterRack> implements ClusterRackService {

    private final ClusterHostService hostService;
    @Autowired
    public ClusterRackServiceImpl(ClusterHostService hostService) {
        this.hostService = hostService;
    }

    @Override
    public List<ClusterRack> queryClusterRack(Integer clusterId) {
        return QueryChain.of(ClusterRack.class)
                .where(ClusterRack::getClusterId).eq(clusterId)
                .list();
    }

    @Override
    public Result saveRack(Integer clusterId, String rack) {
        // 机架名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        GeneralValidator generalValidator = new GeneralValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(generalValidator);
        generalValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(rack);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }

        // 重复校验
        boolean exists = QueryChain.of(ClusterRack.class)
                .where(ClusterRack::getClusterId).eq(clusterId)
                .and(ClusterRack::getRack).eq(rack)
                .exists();

        if (exists) {
            return Result.error("机架名称重复");
        }

        ClusterRack clusterRack = new ClusterRack();
        clusterRack.setRack(rack);
        clusterRack.setClusterId(clusterId);
        this.save(clusterRack);
        return Result.success();
    }

    @Override
    public Result deleteRack(Integer rackId) {
        ClusterRack clusterRack = this.getById(rackId);
        if (rackInUse(clusterRack)) {
            return Result.error(Status.RACK_IS_USING.getMsg());
        }
        this.removeById(rackId);
        return Result.success();
    }

    @Override
    public void createDefaultRack(Integer clusterId) {
        ClusterRack clusterRack = new ClusterRack();
        clusterRack.setRack("/default-rack");
        clusterRack.setClusterId(clusterId);
        this.save(clusterRack);
    }

    private boolean rackInUse(ClusterRack clusterRack) {
        List<ClusterHostDO> list = hostService.getClusterHostByRack(clusterRack.getClusterId(), clusterRack.getRack());
        return !list.isEmpty();
    }
}
