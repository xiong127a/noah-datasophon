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
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ServiceHandlerAbstract {

    public ClusterServiceRoleInstanceEntity roleInstanceEntity;

    /**
     * 添加通用命令到命令行列表中
     * 
     * @param commandLines 命令行列表
     * @param hostname     主机名
     * @return 更新后的命令行列表
     */
    public List<CommandLineItem> addFinalPrompt(List<CommandLineItem> commandLines,String serviceHome, String hostname) {
        // 如果列表为空，创建一个新列表
        if (commandLines == null) {
            commandLines = new ArrayList<>();
        }

        // 添加root命令提示符
        String rootPrompt = "[root@" + hostname + " ~]# ";

        // 获取服务目录名称（去掉路径和版本号）
        CommandLineItem cdCommand = getCommandLineItem(serviceHome, rootPrompt);
        commandLines.add(0, cdCommand);


        // 添加一个date命令，显示当前日期时间
        CommandLineItem dateCommand = new CommandLineItem();
        dateCommand.setLabel("显示当前日期时间");
        dateCommand.setValue("date");
        // 获取当前日期时间
        String currentDateTime = new java.util.Date().toString();
        dateCommand.setCommandResult(currentDateTime);
        dateCommand.setCommandPrompt(rootPrompt);


        // 添加一个date命令，显示当前日期时间
        CommandLineItem command = new CommandLineItem();
        command.setLabel("");
        command.setValue("");
        command.setCommandPrompt(rootPrompt);
        // 将date命令添加到列表中
        commandLines.add(dateCommand);
        commandLines.add(command);

        return commandLines;
    }

    private static CommandLineItem getCommandLineItem(String serviceHome, String rootPrompt) {
        String serviceDirName = "";
        if (serviceHome != null && !serviceHome.isEmpty()) {
            // 获取路径的最后一部分
            String lastPathPart = serviceHome.substring(serviceHome.lastIndexOf('/') + 1);
            // 去掉版本号（假设版本号格式为数字和点，如 hadoop-3.2.1 -> hadoop）
            serviceDirName = lastPathPart.replaceAll("-[0-9]+(\\.[0-9]+)*", "");
        }

        // 进入服务目录
        CommandLineItem cdCommand = new CommandLineItem();
        cdCommand.setLabel("进入" + serviceDirName + "服务目录");
        cdCommand.setValue("cd " + serviceHome);
        cdCommand.setCommandPrompt(rootPrompt);
        return cdCommand;
    }

    public List<ServiceConfig> listServiceConfigByServiceInstance(Integer serviceInstanceId) {
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                .getBean(ClusterServiceInstanceRoleGroupService.class);
        ClusterServiceRoleGroupConfigService groupConfigService = SpringUtil
                .getBean(ClusterServiceRoleGroupConfigService.class);
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstanceId);
        ClusterServiceRoleGroupConfig config = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

    public static List<String> getRoleHosts(Integer clusterId, String roleName) {
        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        List<ClusterServiceRoleInstanceEntity> hiveServer2 = clusterServiceRoleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, roleName);
        return CollUtil.map(hiveServer2, ClusterServiceRoleInstanceEntity::getHostname, true);
    }

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
     * 
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
}
