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

package com.datasophon.api.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Component
public final class SpringTool implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringTool.applicationContext == null) {
            SpringTool.applicationContext = applicationContext;
            System.out.println(
                    "========ApplicationContext配置成功,SpringTool.getAppContext()获取applicationContext对象,applicationContext="
                            + applicationContext + "========");
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }



    public static Map.Entry<String, List<ServiceConfig>> listServiceConfigByServiceInstance(Integer serviceInstanceId) {
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                .getBean(ClusterServiceInstanceRoleGroupService.class);
        ClusterServiceRoleGroupConfigService groupConfigService = SpringUtil
                .getBean(ClusterServiceRoleGroupConfigService.class);
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstanceId);
        ClusterServiceRoleGroupConfig config = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());

        ServiceInfo serviceInfo = ServiceInfoMap.get("DDP-1.2.1_" + roleGroup.getServiceName());
        String serviceHome = "";
        if (serviceInfo != null) {
            serviceHome = serviceInfo.getDecompressPackageName();
        }
        return new AbstractMap.SimpleEntry<>(serviceHome,
                JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class));
    }

}
