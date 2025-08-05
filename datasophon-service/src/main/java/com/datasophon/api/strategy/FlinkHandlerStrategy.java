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

package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlinkHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        boolean enableJM2HA = false;
        Map<String, ServiceConfig> map = list.stream()
                .collect(java.util.stream.Collectors.toMap(ServiceConfig::getName, config -> config));
        for (ServiceConfig config : list) {
            if ("enableJMHA".equals(config.getName())) {
                enableJM2HA = isEnableHA(clusterId, globalVariables, enableJM2HA, config, "FLINK");
            }
        }
        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "FLINK" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableJM2HA) {
            addConfigWithHA(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithHA(list, map, configs);
        }
        list.addAll(kbConfigs);
    }

}
