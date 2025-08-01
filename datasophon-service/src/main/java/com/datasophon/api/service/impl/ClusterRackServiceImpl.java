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

import com.datasophon.common.enums.Status;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.string.validator.GeneralValidator;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterRack;
import com.datasophon.dao.mapper.ClusterRackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群机架服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("clusterRackService")
public class ClusterRackServiceImpl implements ClusterRackService {

    @Autowired
    private ClusterRackMapper clusterRackMapper;

    @Autowired
    private ClusterHostService hostService;

    @Override
    public List<ClusterRack> queryClusterRack(Integer clusterId) {
        return clusterRackMapper.selectByClusterId(clusterId);
    }

    @Override
    public ClusterRack saveRack(Integer clusterId, String rack) {
        // 机架名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        GeneralValidator generalValidator = new GeneralValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(generalValidator);
        generalValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(rack);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验
        boolean exists = clusterRackMapper.existsByClusterIdAndRack(clusterId, rack);

        if (exists) {
            throw new RuntimeException("机架名称重复");
        }

        ClusterRack clusterRack = new ClusterRack();
        clusterRack.setRack(rack);
        clusterRack.setClusterId(clusterId);
        clusterRackMapper.insert(clusterRack);
        return clusterRack;
    }

    @Override
    public boolean deleteRack(Integer rackId) {
        ClusterRack clusterRack = clusterRackMapper.selectById(rackId);
        if (clusterRack == null) {
            throw new RuntimeException("Rack not found with id: " + rackId);
        }
        if (rackInUse(clusterRack)) {
            throw new RuntimeException(Status.RACK_IS_USING.getMsg());
        }
        clusterRackMapper.removeById(rackId);
        return true;
    }

    @Override
    public void createDefaultRack(Integer clusterId) {
        ClusterRack clusterRack = new ClusterRack();
        clusterRack.setRack("/default-rack");
        clusterRack.setClusterId(clusterId);
        clusterRackMapper.insert(clusterRack);
    }

    private boolean rackInUse(ClusterRack clusterRack) {
        List<ClusterHostDO> list = hostService.getClusterHostByRack(clusterRack.getClusterId(), clusterRack.getRack());
        return !list.isEmpty();
    }

    // 标准CRUD方法实现
    @Override
    public ClusterRack getById(Integer id) {
        return clusterRackMapper.selectById(id);
    }

    @Override
    public ClusterRack save(ClusterRack entity) {
        clusterRackMapper.insert(entity);
        return entity;
    }

    @Override
    public ClusterRack updateById(ClusterRack entity) {
        clusterRackMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return clusterRackMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<ClusterRack> getAllRacks() {
        return clusterRackMapper.selectAll();
    }
}
