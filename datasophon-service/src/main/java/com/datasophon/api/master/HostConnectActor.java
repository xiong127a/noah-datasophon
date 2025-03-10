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

import com.datasophon.api.enums.Status;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;

import org.apache.sshd.client.session.ClientSession;

import scala.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;
import cn.hutool.core.util.ObjectUtil;
import org.apache.commons.lang3.StringUtils;
import java.util.Objects;

public class HostConnectActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(HostConnectActor.class);

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("or restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof HostCheckCommand) {
            HostCheckCommand hostCheckCommand = (HostCheckCommand) message;
            HostInfo hostInfo = hostCheckCommand.getHostInfo();
            logger.info("开始主机检查:{}", hostInfo.getHostname());

            ClientSession session = null;
            try {
                // 1. 先尝试免密登录
                session = MinaUtils.openConnection(
                        hostInfo.getHostname(), 
                        hostInfo.getSshPort(), 
                        hostInfo.getSshUser());

                if (Objects.isNull(session) && StringUtils.isNotBlank(hostInfo.getSshPassword())) {
                    // 2. 免密失败且密码不为空，尝试密码登录
                    logger.info("免密登录失败，尝试密码登录:{}", hostInfo.getHostname());
                    session = MinaUtils.openConnectionWithPassword(
                            hostInfo.getHostname(),
                            hostInfo.getSshPort(),
                            hostInfo.getSshUser(),
                            hostInfo.getSshPassword());

                    if (Objects.nonNull(session)) {
                        // 3. 密码登录成功，设置免密
                        logger.info("开始设置免密登录:{}", hostInfo.getHostname());
                        boolean success = MinaUtils.setupPasswordlessLogin(session, 
                                hostInfo.getSshUser(),
                                hostInfo.getSshPassword());
                        
                        if (success) {
                            logger.info("免密设置成功:{}", hostInfo.getHostname());
                            hostInfo.setCheckResult(new CheckResult(
                                    Status.CHECK_HOST_SUCCESS.getCode(),
                                    Status.CHECK_HOST_SUCCESS.getMsg()));
                        } else {
                            logger.error("免密设置失败:{}", hostInfo.getHostname());
                            hostInfo.setCheckResult(new CheckResult(
                                    Status.CONNECTION_FAILED.getCode(),
                                    "免密设置失败"));
                        }
                    } else {
                        logger.error("密码登录失败:{}", hostInfo.getHostname());
                        hostInfo.setCheckResult(new CheckResult(
                                Status.CONNECTION_FAILED.getCode(),
                                "密码登录失败"));
                    }
                } else if (Objects.nonNull(session)) {
                    // 4. 免密登录成功
                    logger.info("免密登录成功:{}", hostInfo.getHostname());
                    hostInfo.setCheckResult(new CheckResult(
                            Status.CHECK_HOST_SUCCESS.getCode(),
                            Status.CHECK_HOST_SUCCESS.getMsg()));
                } else {
                    // 5. 所有登录方式都失败
                    logger.error("所有登录方式均失败:{}", hostInfo.getHostname());
                    hostInfo.setCheckResult(new CheckResult(
                            Status.CONNECTION_FAILED.getCode(),
                            "无法建立SSH连接"));
                }
            } catch (Exception e) {
                logger.error("主机连接异常:" + hostInfo.getHostname(), e);
                hostInfo.setCheckResult(new CheckResult(
                        Status.CONNECTION_FAILED.getCode(),
                        "连接异常: " + e.getMessage()));
            } finally {
                if (Objects.nonNull(session)) {
                    MinaUtils.closeConnection(session);
                }
                logger.info("完成主机检查:{}", hostInfo.getHostname());
            }
        } else {
            unhandled(message);
        }
    }
}
