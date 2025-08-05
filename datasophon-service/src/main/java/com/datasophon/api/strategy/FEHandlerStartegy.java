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

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.common.model.ProcInfo;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.OlapUtils;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FEHandlerStartegy implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FEHandlerStartegy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        // if feMaster is null, set the first host as feMaster
        //Prevent FE Observer nodes from starting and FE Master nodes from changing
//        if (!globalVariables.containsKey("${feMaster}") || ObjUtil.isNull(globalVariables.get("${feMaster}"))) {
        if (!hosts.isEmpty()) {
            SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${feMaster}", hosts.getFirst());
        }
//        }
    }


    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String feMaster = globalVariables.get("${feMaster}");
        if (hostname.equals(feMaster)) {
            logger.info("fe master is {}", feMaster);
            serviceRoleInfo.setSortNum(1);
        } else {
            logger.info("set fe follower master");
            serviceRoleInfo.setMasterHost(feMaster);
            serviceRoleInfo.setSlave(true);
            serviceRoleInfo.setSortNum(2);
        }

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
                                        Map<String, ClusterServiceRoleInstanceDTO> map) {
        Map<String, String> globalVariables = GlobalVariables.get(roleInstanceDto.clusterId());
        String feMaster = globalVariables.get("${feMaster}");
        if (roleInstanceDto.hostname().equals(feMaster)
                && ServiceRoleState.RUNNING.getValue() == roleInstanceDto.serviceRoleState()) {
            try {
                List<ProcInfo> frontends = OlapUtils.showFrontends(feMaster);
                resolveProcInfoAlert(roleInstanceDto.serviceRoleName(), frontends, map);
            } catch (Exception ignored) {

            }


        }
    }

    private void resolveProcInfoAlert(String serviceRoleName, List<ProcInfo> frontends,
                                      Map<String, ClusterServiceRoleInstanceDTO> map) {
        ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
        
        for (ProcInfo frontend : frontends) {
            ClusterServiceRoleInstanceDTO roleInstanceDto = map.get(frontend.getHostName() + serviceRoleName);
            if (!frontend.getAlive()) {
                String alertTargetName = serviceRoleName + " Not Add To Cluster";
                logger.info("{} at host {} is not add to cluster", serviceRoleName, frontend.getHostName());
                String alertAdvice = "The errmsg is " + frontend.getErrMsg();
                serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.WARN, alertAdvice);
            } else {
                serviceStateManagementService.recoverAlert(roleInstanceDto);
            }
        }
    }
}
