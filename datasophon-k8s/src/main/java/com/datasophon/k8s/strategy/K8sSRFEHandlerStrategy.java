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

package com.datasophon.k8s.strategy;

import cn.hutool.json.JSONUtil;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;

import java.util.concurrent.TimeUnit;

public class K8sSRFEHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRFEHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        logger.info("FEHandlerStrategy start fe" + JSONUtil.toJsonStr(command));
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
            if (command.isSlave()) {
                logger.info("first start  fe");
                startResult = serviceHandler.start(command);
                if (startResult.getExecResult()) {
                    // add follower
                    try {
                        startResult = OlapUtils.addFollower(command.getMasterHost(), command.getHostname());
                        int tryTimes = 0;
                        while (!startResult.getExecResult() && tryTimes < 3) {
                            TimeUnit.SECONDS.sleep(10L);
                            startResult = OlapUtils.addFollower(command.getMasterHost(), command.getHostname());
                            tryTimes++;
                        }
                        logger.info("slave fe start success");
                    } catch (Exception e) {
                        logger.error("add slave fe failed {}", ThrowableUtils.getStackTrace(e));
                    }
                } else {
                    logger.error("slave fe start failed");
                }
            } else {
                startResult = serviceHandler.start(command);
            }
        } else {
            startResult = serviceHandler.start(command);
        }
        return startResult;
    }
}
