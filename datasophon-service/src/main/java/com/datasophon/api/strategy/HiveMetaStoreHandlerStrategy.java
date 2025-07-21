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
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.ServiceRoleInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class HiveMetaStoreHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        if(CollUtil.isEmpty(hosts)){
            return;
        }
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (hosts.size() == 1) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${metastoreHost}", hosts.getFirst());
        }
        String metastoreHosts = StrUtil.join(",",CollUtil.map(hosts,ip -> "thrift://" + ip + ":9083",false));
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${metastoreHosts}",metastoreHosts);
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${masterHiveMetaStore}", hosts.getFirst());
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String key = "${masterHiveMetaStore}";
        if (globalVariables.containsKey(key)
                && !hostname.equals(globalVariables.get(key))) {
            log.info("set to slave masterHiveMetaStore");
            serviceRoleInfo.setSlave(true);
        }
        serviceRoleInfo.setMasterHost(globalVariables.get(key));
    }

}
