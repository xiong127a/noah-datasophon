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
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.converter.ClusterServiceInstanceRoleGroupConverter;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroupEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class SpringTool  {


    public static Map.Entry<String, List<ServiceConfig>> listServiceConfigByServiceInstance(Long serviceInstanceId) {
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                .getBean(ClusterServiceInstanceRoleGroupService.class);
        ClusterServiceRoleGroupConfigService groupConfigService = SpringUtil
                .getBean(ClusterServiceRoleGroupConfigService.class);
        ClusterServiceInstanceRoleGroupConverter roleGroupConverter = SpringUtil.getBean(ClusterServiceInstanceRoleGroupConverter.class);
        ClusterServiceRoleGroupConfigConverter configConverter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
        
        ClusterServiceInstanceRoleGroupDTO roleGroupDTO = roleGroupService.getRoleGroupByServiceInstanceId(serviceInstanceId);
        ClusterServiceInstanceRoleGroupEntity roleGroup = roleGroupDTO != null ?
                roleGroupConverter.dtoToEntity(roleGroupDTO) : null;
        
        if (roleGroup == null) {
            return null;
        }
        
        ClusterServiceRoleGroupConfigDTO configDTO = groupConfigService.getConfigByRoleGroupId(roleGroup.getId());
        ClusterServiceRoleGroupConfigEntity config = configDTO != null ?
                configConverter.dtoToEntity(configDTO) : null;

        ServiceInfo serviceInfo = ServiceInfoMap.get("DDP-1.2.1_" + roleGroup.getServiceName());
        String serviceHome = "";
        if (serviceInfo != null) {
            serviceHome = serviceInfo.getDecompressPackageName();
        }
        
        if (config == null) {
            return new AbstractMap.SimpleEntry<>(serviceHome, new ArrayList<>());
        }
        
        return new AbstractMap.SimpleEntry<>(serviceHome,
                JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class));
    }
}
