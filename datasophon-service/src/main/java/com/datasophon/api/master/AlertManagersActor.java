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

package com.datasophon.api.master;

import akka.actor.UntypedActor;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 更新alertManger 的配置信息
 * 包含基础配置，路由，邮件通知组
 * 通知组全局通用，
 *      当通知组更新的时候，更新所有集群的alertManager中的路由
 */
public class AlertManagersActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(AlertManagersActor.class);

    private static final String SERVICENAME = "ALERTMANAGER";

    @Override
    public void onReceive(Object msg) throws Throwable {

        //更新所有集群的通知组
        //获取alertManager的所有实例
        ClusterServiceRoleInstanceService roleInstanceService = SpringUtil.getBean(ClusterServiceRoleInstanceService.class);
        List<ClusterServiceRoleInstanceEntity> roleInstanceEntitys = roleInstanceService.listServiceRoleByName("AlertManager");
        if (CollectionUtils.isEmpty(roleInstanceEntitys)) {
            return;
        }

        //分集群更新
        Map<Integer, List<ClusterServiceRoleInstanceEntity>> clusterRoules = roleInstanceEntitys.stream().collect(Collectors.groupingBy(ClusterServiceRoleInstanceEntity::getClusterId));
        for (Integer clusterId : clusterRoules.keySet()) {

            //查询集群框架
            ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            String getServiceDcPackageName = PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), SERVICENAME);

            //分服务实例更新，一般alertmanager只需要一个实例
            for (ClusterServiceRoleInstanceEntity alertManager : roleInstanceEntitys) {

                //通过实例的配置组id查询配置的详细信息，
                ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringUtil.getBean(ClusterServiceRoleGroupConfigService.class);
                ClusterServiceRoleGroupConfig roleGroupConfig = roleGroupConfigService.getConfigByRoleGroupId(alertManager.getRoleGroupId());

                //准备配置参数·       ·
                Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                ProcessUtils.generateConfigFileMap(configFileMap, roleGroupConfig, clusterId);

                //准备调用参数
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setHostname(alertManager.getHostname());
                serviceRoleInfo.setDecompressPackageName(getServiceDcPackageName);

                //执行配置生成操作
                ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
                ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);

                //返回结果处理
                if (execResult.getExecResult()) {
                    logger.info("Generate AlertManager  config success , now start to reload AlertManager");
                    // 刷新配置
                    HttpUtil.post("http://" + alertManager.getHostname() + ":9093/-/reload", "");
                } else {
                    logger.error("generate rack.properties failed");
                }
            }

        }
    }
}
