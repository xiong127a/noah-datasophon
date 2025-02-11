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

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ServiceHandlerAbstract {

    public ClusterServiceRoleInstanceEntity roleInstanceEntity;

    public void removeConfigWithKerberos(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                                         List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithKerberos()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }
    public void removeConfigWithHA(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                                   List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithHA()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    public void removeConfigWithRack(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                                     List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithRack()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    /**
     * 将所有service_ddl.json中configType是kb的配置项加入到当前配置列表
     * isConfigWithKerberos判定条件在 service_ddl.json 中设置 configWithKerberos = true
     * @param globalVariables 全局变量
     * @param map             当前前端传入的配置项
     * @param configs         所有service_ddl.json中设置的所有配置项
     * @param kbConfigs       需要添加到当前的配置项
     */
    public void addConfigWithKerberos(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                                      List<ServiceConfig> configs, ArrayList<ServiceConfig> kbConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithKerberos()) {
                addConfig(globalVariables, map, kbConfigs, serviceConfig);
            }
        }
    }

    public void addConfigWithHA(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                                List<ServiceConfig> configs, ArrayList<ServiceConfig> kbConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithHA()) {
                addConfig(globalVariables, map, kbConfigs, serviceConfig);
            }
        }
    }

    public void addConfigWithRack(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                                  List<ServiceConfig> configs, List<ServiceConfig> rackConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithRack()) {
                addConfig(globalVariables, map, rackConfigs, serviceConfig);
            }
        }
    }

    public void addConfig(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                           List<ServiceConfig> rackConfigs, ServiceConfig serviceConfig) {
        if (map.containsKey(serviceConfig.getName())) {
            ServiceConfig config = map.get(serviceConfig.getName());
            config.setRequired(true);
            config.setHidden(false);
            if (Constants.INPUT.equals(config.getType())) {
                String value = PlaceholderUtils.replacePlaceholders((String) config.getValue(), globalVariables,
                        Constants.REGEX_VARIABLE);
                config.setValue(value);
            }
        } else {
            serviceConfig.setRequired(true);
            serviceConfig.setHidden(false);
            if (Constants.INPUT.equals(serviceConfig.getType())) {
                String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceConfig.setValue(value);
            }
            rackConfigs.add(serviceConfig);
        }
    }

    public boolean isEnableKerberos(Integer clusterId, Map<String, String> globalVariables, boolean enableKerberos,
                                    ServiceConfig config, String serviceName) {
        if ((Boolean) config.getValue()) {
            enableKerberos = true;
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "Kerberos}",
                    "true");
        } else {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "Kerberos}",
                    "false");
        }
        return enableKerberos;
    }

    public boolean isEnableHA(Integer clusterId, Map<String, String> globalVariables, boolean enableHA,
                              ServiceConfig config, String serviceName) {
        if ((Boolean) config.getValue()) {
            enableHA = true;
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "HA}", "true");
        } else {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "HA}", "false");
        }
        return enableHA;
    }

    public boolean isEnableRack(boolean enableRack, ServiceConfig config) {
        if ((Boolean) config.getValue()) {
            enableRack = true;
        }
        return enableRack;
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
    public void addConfigWithConfigureGroupName(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
                                                List<ServiceConfig> configs, List<ServiceConfig> aclConfigs, String ConfigureGroupName) {
        for (ServiceConfig serviceConfig : configs) {
            if (StrUtil.equals(serviceConfig.getConfigureGroupName(), ConfigureGroupName)) {
                addConfig(globalVariables, map, aclConfigs, serviceConfig);
            }
        }
    }

    public void removeConfigWithConfigureGroupName(List<ServiceConfig> list, Map<String, ServiceConfig> map,
                                                   List<ServiceConfig> configs, String ConfigureGroupName) {
        for (ServiceConfig serviceConfig : configs) {
            if (StrUtil.equals(serviceConfig.getConfigureGroupName(), ConfigureGroupName)) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    public void handleConfig(List<ServiceConfig> list, boolean isEnable, Map<String, String> globalVariables, Map<String, ServiceConfig> map, List<ServiceConfig> configs, String ConfigureGroupName) {
        List<ServiceConfig> toProcessConfigs = new ArrayList<>();
        if (isEnable) {
            addConfigWithConfigureGroupName(globalVariables, map, configs, toProcessConfigs, ConfigureGroupName);
        } else {
            removeConfigWithConfigureGroupName(list, map, configs, ConfigureGroupName);
        }
        list.addAll(toProcessConfigs);
    }
}
