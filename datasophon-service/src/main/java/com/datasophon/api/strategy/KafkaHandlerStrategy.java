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
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KafkaHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {


    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

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
        }

        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "KAFKA" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);
        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();

        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
            //TODO  当kafka开启kerberos认证时，efak也要开启
            enableSasl = true;
            for (ServiceConfig config : list) {
                if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                    config.setValue(enableSasl);
                }
            }
        } else {
            removeConfigWithKerberos(list, map, configs);
            //TODO  当kafka关闭kerberos认证时，efak也要关闭
            enableSasl = false;
            for (ServiceConfig config : list) {
                if ("cluster1.efak.sasl.enable".equals(config.getName())) {
                    config.setValue(enableSasl);
                }
            }
        }

        handleConfig(list, enableAcl, globalVariables, map, configs, "acl");
        handleConfig(list, enableDistributed, globalVariables, map, configs, "efak-ha");
        handleConfig(list, enableJmxAcl, globalVariables, map, configs, "jmx-acl");
        handleConfig(list, enableSasl, globalVariables, map, configs, "sasl");


        list.addAll(kbConfigs);
    }

    private void handleConfig(List<ServiceConfig> list, boolean enableAcl, Map<String, String> globalVariables, Map<String, ServiceConfig> map, List<ServiceConfig> configs, String configType) {
        List<ServiceConfig> toProcessConfigs = new ArrayList<>();
        if (enableAcl) {
            addConfigWithConfigType(globalVariables, map, configs, toProcessConfigs, configType);
        } else {
            removeConfigWithConfigType(list, map, configs, configType);
        }
        list.addAll(toProcessConfigs);
    }

    public boolean isEnableConfig(ServiceConfig config) {
        return BooleanUtil.toBoolean(StrUtil.toString(config.getValue()));
    }

    /**
     * 将所有service_ddl.json中configType是acl的配置项加入到当前配置列表
     * isConfigWithAcl判定条件在 service_ddl.json 中设置 cluster1.zk.acl.enable = true
     *
     * @param globalVariables 全局变量
     * @param map             当前前端传入的配置项
     * @param configs         所有service_ddl.json中设置的所有配置项
     * @param aclConfigs      需要添加到当前的配置项
     */
    public void addConfigWithConfigType(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                                        List<ServiceConfig> configs, List<ServiceConfig> aclConfigs, String configType) {
        for (ServiceConfig serviceConfig : configs) {
            if (StrUtil.equals(serviceConfig.getConfigType(), configType)) {
                addConfig(globalVariables, map, aclConfigs, serviceConfig);
            }
        }
    }

    public void removeConfigWithConfigType(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                                           List<ServiceConfig> configs, String configType) {
        for (ServiceConfig serviceConfig : configs) {
            if (StrUtil.equals(serviceConfig.getConfigType(), configType)) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        handlerConfig(clusterId, list);
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
                                        Map<String, ClusterServiceRoleInstanceEntity> map) {

    }
}
