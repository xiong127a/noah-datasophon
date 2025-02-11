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

import cn.hutool.core.convert.Convert;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KafkaHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {


    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        boolean enableKerberos = false;
        boolean enableAcl = false;
        boolean enableDistributed = false;
        boolean enableJmxAcl = false;
        boolean enableSasl = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
        for (ServiceConfig config : list) {
            if ("enableKerberos".equals(config.getName())) {
                enableKerberos = isEnableKerberos(clusterId, globalVariables, enableKerberos, config, "KAFKA");
            }
            if ("zookeeper.connect".equals(config.getName())) {
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${kafkaZkAddr}", Convert.toStr(config.getValue()));
            }
            if ("cluster1.zk.acl.enable".equals(config.getName())) {
                enableAcl = isEnableConfig(config);
            }
            if ("efak.distributed.enable".equals(config.getName())) {
                enableDistributed = isEnableConfig(config);
            }
            if ("cluster1.efak.jmx.acl".equals(config.getName())) {
                enableJmxAcl = isEnableConfig(config);
            }
            if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                enableSasl = isEnableConfig(config);
            }
            /*if ("JMX_PORT".equals(config.getName())) {
                if (ObjectUtil.isNotEmpty(config.getValue())){
                    config.setRequired(true);
                }
            }*/
        }

        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "KAFKA" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();

        // 根据 enableKerberos 的值更新配置
        if (enableKerberos) {
            //TODO  当kafka开启kerberos认证时，efak也要开启
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            //TODO  当kafka关闭kerberos认证时，efak也要关闭
            removeConfigWithKerberos(list, map, configs);
        }

        // 更新 EFAK 的 SASL 配置
        enableSasl = enableKerberos;
        updateEfakSaslConfig(list, enableSasl);

        handleConfig(list, enableAcl, globalVariables, map, configs, "acl");
        handleConfig(list, enableDistributed, globalVariables, map, configs, "efak-ha");
        handleConfig(list, enableJmxAcl, globalVariables, map, configs, "jmx-acl");
        handleConfig(list, enableSasl, globalVariables, map, configs, "sasl");


        list.addAll(kbConfigs);
    }

    private void updateEfakSaslConfig(List<ServiceConfig> list, boolean enableSasl) {
        for (ServiceConfig config : list) {
            if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                config.setValue(enableSasl);
            }
        }
    }





}
