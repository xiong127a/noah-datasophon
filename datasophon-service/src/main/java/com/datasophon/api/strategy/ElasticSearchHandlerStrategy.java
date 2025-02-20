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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.common.Constants.DATASOPHON;
import static com.datasophon.common.utils.HostUtils.generateHosts;

public class ElasticSearchHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String depMode = getDepMode(clusterId);
        if (!Constants.PVM_MODE.equals(depMode)) {
            hosts = generateHosts(hosts, "elasticsearch-elasticsearch");
        }
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${initMasterNodes}", String.join(",", hosts));
        String seedHosts = hosts.stream()
                .map(host -> host + ":9300")
                .collect(Collectors.joining(","));
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${seedHosts}", seedHosts);
        if (CollUtil.isNotEmpty(hosts)) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${esSingleHost}", hosts.get(0));
        }

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig config : list) {
            if ("http.port".equals(config.getName())) {
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${esHttpPort}", Convert.toStr(config.getValue()));
            }
        }
    }

}
