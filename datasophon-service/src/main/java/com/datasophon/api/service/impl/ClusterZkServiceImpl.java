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

import com.datasophon.api.service.ClusterZkService;
import com.datasophon.dao.entity.ClusterZk;
import com.datasophon.dao.mapper.ClusterZkMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群ZooKeeper服务实现
 * 按照架构重构规范，迁移QueryChain到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterZkService")
public class ClusterZkServiceImpl extends ServiceImpl<ClusterZkMapper, ClusterZk> implements ClusterZkService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterZkServiceImpl.class);

    @Override
    public Integer getMaxMyId(Integer clusterId) {
        return getMapper().getMaxMyId(clusterId);
    }

    @Override
    public List<ClusterZk> getAllZkServer(Integer clusterId) {
        return getMapper().selectByClusterId(clusterId);
    }
}
