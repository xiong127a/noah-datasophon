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

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;

import com.datasophon.api.master.handler.host.CheckWorkerMd5Handler;
import com.datasophon.api.master.handler.host.DecompressWorkerHandler;
import com.datasophon.api.master.handler.host.DispatcherWorkerHandlerChain;
import com.datasophon.api.master.handler.host.StartWorkerHandler;
import com.datasophon.api.master.handler.host.UploadWorkerHandler;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.model.HostInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;



public class DispatcherWorkerActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherWorkerActor.class);
    
    // SSH连接服务
    private final SshConnectionService sshService = 
            SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();

    /**
     * 构建SSH检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshPort(hostInfo.getSshPort())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .build();
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("host actor restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(DispatcherHostAgentCommand.class, command -> {
                    HostInfo hostInfo = command.getHostInfo();
                    logger.info("【分发Worker Actor】开始分发主机代理: {}", hostInfo.getIp());
                    
                    hostInfo.setMessage(
                            MessageResolverUtils.getMessage(
                                    "distributed.host.management.agent.installation.package"));
                    
                    try {
                        // 通过SSH插件适配器验证连接
                        // 使用SSH插件辅助工具测试连接
                        HostCheckContext context = buildHostCheckContext(hostInfo);
                        CommandResult connectionTest = sshService.testConnection(context);
                        boolean connectionValid = connectionTest.isSuccess();
                        
                        if (!connectionValid) {
                            logger.error("【分发Worker Actor】SSH连接验证失败，无法分发代理: {}", hostInfo.getIp());
                            hostInfo.setErrorMessage("SSH连接失败，无法分发代理");
                            return;
                        }
                        
                        // 创建处理链，SSH连接由各个Handler内部通过插件适配器管理
                        DispatcherWorkerHandlerChain handlerChain = new DispatcherWorkerHandlerChain();
                        handlerChain.addHandler(new UploadWorkerHandler());
                        handlerChain.addHandler(new CheckWorkerMd5Handler());
                        handlerChain.addHandler(new DecompressWorkerHandler());
                        handlerChain.addHandler(
                                new StartWorkerHandler(command.getClusterId(), command.getClusterFrame()));
                        
                        // 执行处理链，SSH连接由插件适配器统一管理
                        handlerChain.handle(hostInfo);
                        
                        logger.info("【分发Worker Actor】主机代理分发完成: {}", hostInfo.getIp());
                        
                    } catch (Exception e) {
                        logger.error("【分发Worker Actor】主机代理分发异常: {} -> {}", hostInfo.getIp(), e.getMessage(), e);
                        hostInfo.setErrorMessage("代理分发异常: " + e.getMessage());
                    }
                })
                .matchAny(this::unhandled)
                .build();
    }
}
