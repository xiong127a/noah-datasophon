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

import com.datasophon.api.service.HdfsEcService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.command.HdfsEcCommand;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * HDFS纠删码服务实现
 * 替代HdfsECActor，用于管理HDFS容量扩容和缩容
 */
@Service
public class HdfsEcServiceImpl implements HdfsEcService {

    private static final Logger logger = LoggerFactory.getLogger(HdfsEcServiceImpl.class);

    @Override
    // @Async removed - 改为同步执行，避免Spring线程池卡死问题
    public void handleHdfsEcCommand(HdfsEcCommand hdfsEcCommand) {
        try {
            // 查询DataNode列表
            List<ClusterServiceRoleInstanceEntity> datanodes = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(hdfsEcCommand.getServiceInstanceId())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq("DataNode")
                    .list();

            TreeSet<String> list = datanodes.stream()
                    .map(ClusterServiceRoleInstanceEntity::getHostname)
                    .collect(Collectors.toCollection(TreeSet::new));

            ProcessUtils.hdfsEcMethond(hdfsEcCommand.getServiceInstanceId(), list, "whitelist", "NameNode");
            
            logger.info("HDFS纠删码配置成功，serviceInstanceId: {}", hdfsEcCommand.getServiceInstanceId());
        } catch (Exception e) {
            logger.error("处理HdfsEcCommand时出错", e);
        }
    }
}

