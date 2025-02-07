/*
 *
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
 *
 */

package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class NameNodeHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {



    private static final String ENABLE_RACK = "enableRack";

    private static final String ENABLE_KERBEROS = "enableKerberos";
    private static final String ENABLE_RANGER = "dfs.permissions";


    @Override
    public void handler(Integer clusterId, List<String> hosts) {

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nn1}", hosts.get(0));
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nn2}", hosts.get(1));
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);

        boolean enableRack = false;
        boolean enableKerberos = false;
        boolean enableRanger = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

        String key =
                clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HDFS" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);

        for (ServiceConfig config : list) {
            if (ENABLE_RACK.equals(config.getName())) {
                if ((Boolean) config.getValue()) {
                    enableRack = isEnableRack(enableRack, config);
                }
            }
            if (ENABLE_KERBEROS.equals(config.getName())) {
                enableKerberos =
                        isEnableKerberos(
                                clusterId, globalVariables, enableKerberos, config, "HDFS");
            }
            if (ENABLE_RANGER.equals(config.getName())) {
                if ((Boolean) config.getValue()) {
                    enableRanger = isEnableConfig(config);
                }
            }
        }
        List<ServiceConfig> rackConfigs = new ArrayList<>();
        if (enableRack) {
            log.info("start to add rack config");
            addConfigWithRack(globalVariables, map, configs, rackConfigs);
        } else {
            removeConfigWithRack(list, map, configs);
        }
        list.addAll(rackConfigs);

        List<ServiceConfig> rangerConfigs = new ArrayList<>();
        if (enableRanger) {
            log.info("start to add ranger config");
            addConfigWithRack(globalVariables, map, configs, rangerConfigs);
        } else {
            removeConfigWithRack(list, map, configs);
        }
        list.addAll(rangerConfigs);

        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithKerberos(list, map, configs);
        }
        handleConfig(list, enableRanger, globalVariables, map, configs, "permission");

        list.addAll(kbConfigs);

    }


    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        if (hostname.equals(globalVariables.get("${nn2}"))) {
            log.info("set to slave namenode");
            serviceRoleInfo.setSlave(true);
            serviceRoleInfo.setSortNum(5);
        }
    }

    @Override
    public void handlerServiceRoleCheck(
            ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
        performServiceRoleCheck(roleInstanceEntity, "nMStateActor");
    }

    @Override
    public void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        performServiceRoleCheck(roleInstanceEntity, "");
    }




    public ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        Map<String, String> globalVariable = GlobalVariables.get(roleInstanceEntity.getClusterId());
        String nn2 = globalVariable.get("${nn2}");
        String commandLine =
                globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn1";
        if (nn2.equals(roleInstanceEntity.getHostname())) {
            commandLine =
                    globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn2";
        }
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        cmdCommand.setCommandLine(commandLine);
        return cmdCommand;
    }



}
