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

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static com.datasophon.common.Constants.KUBERNETES_NODEPORT_MAPPING;

public class GrafanaHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Long clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        if (hosts.size() == 1) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${grafanaHost}",
                    hosts.getFirst());
        }
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        String key = "${grafanaHost}";
        if (globalVariables.containsKey(key)
                && !hostname.equals(globalVariables.get(key))) {
            log.info("set to slave Grafana");
            serviceRoleInfo.setSlave(true);
        }
        ServerProperties serverProperties = SpringUtil.getApplicationContext().getBean(ServerProperties.class);

        String localHostName = null;
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Failed to retrieve the local host name. The system could not resolve the hostname.", e);
        }

        // 获取服务器端口
        int port = serverProperties.getPort();

        // 获取上下文路径（context-path）
        String contextPath = serverProperties.getServlet().getContextPath();

        String url = "http://" + localHostName + ":" + port + contextPath + "/api/cluster/grafana/kerberos/";
        serviceRoleInfo.setExtendConfig(url);
        serviceRoleInfo.setMasterHost(globalVariables.get(key));
    }

    @Override
    public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
        String port = "3000";
        for (ServiceConfig serviceConfig : list) {
            if (StrUtil.equals(serviceConfig.getName(), "grafana_" + KUBERNETES_NODEPORT_MAPPING)) {
                Object configValue = serviceConfig.getValue();
                JSONObject jsonObject = JSONUtil.parseObj(configValue);
                 port = jsonObject.getStr("3000");
                break;
            }
        }
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);

        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${grafanaPort}", port);
    }
}
