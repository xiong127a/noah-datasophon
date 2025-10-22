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

package com.datasophon.api.master.handler.service;

import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.FreemarkerUtils;
import com.datasophon.common.utils.TemplatePathUtils;
import com.datasophon.api.utils.ClusterInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceConfigureHandler extends ServiceHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigureHandler.class);

    @Override
    public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo) {
        // config
        GenerateServiceConfigCommand generateServiceConfigCommand = new GenerateServiceConfigCommand();
        generateServiceConfigCommand.setServiceName(serviceRoleInfo.getParentName());
        generateServiceConfigCommand.setClusterId(serviceRoleInfo.getClusterId()); // 设置集群ID
        String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
        generateServiceConfigCommand.setNamespace(namespace);
        generateServiceConfigCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
        generateServiceConfigCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
        generateServiceConfigCommand.setRunAs(serviceRoleInfo.getRunAs());
        if ("zkserver".equalsIgnoreCase(serviceRoleInfo.getName())) {
            generateServiceConfigCommand.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
        }
        generateServiceConfigCommand.setServiceRoleName(serviceRoleInfo.getName());
        
        // 打包模板内容到命令中，避免 Worker 回连 API 获取
        packTemplateContents(generateServiceConfigCommand, serviceRoleInfo.getConfigFileMap());
        
        ActorSelection configActor = ActorUtils.actorSystem.actorSelection(
                "akka.tcp://datasophon@" + serviceRoleInfo.getHostname() + ":2552/user/worker/configureServiceActor");

        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> configureFuture = Patterns.ask(configActor, generateServiceConfigCommand, timeout);
        try {
            ExecResult configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
            if (Objects.nonNull(configResult) && configResult.getExecResult()) {
                if (Objects.nonNull(getNext())) {
                    return getNext().handlerRequest(serviceRoleInfo);
                }
            }
            return configResult;
        } catch (Exception e) {
            logger.error("配置服务失败", e);
            return new ExecResult();
        }
    }
    
    /**
     * 打包所有需要的模板内容到命令中
     */
    private void packTemplateContents(GenerateServiceConfigCommand command, 
                                      Map<Generators, List<ServiceConfig>> configFileMap) {
        if (configFileMap == null || configFileMap.isEmpty()) {
            return;
        }
        
        for (var generators : configFileMap.keySet()) {
            // 获取模板名称
            var templateName = FreemarkerUtils.determineTemplateName(generators);
            if (templateName != null && !command.getTemplateContents().containsKey(templateName)) {
                // 从本地读取模板内容
                var templateContent = TemplatePathUtils.getTemplateContent(templateName);
                if (templateContent != null) {
                    command.getTemplateContents().put(templateName, templateContent);
                    logger.info("打包模板 {} 到配置命令，内容长度: {}", templateName, templateContent.length());
                } else {
                    logger.warn("无法获取模板内容: {}", templateName);
                }
            }
        }
        
        logger.info("共打包 {} 个模板到配置命令", command.getTemplateContents().size());
    }
}
