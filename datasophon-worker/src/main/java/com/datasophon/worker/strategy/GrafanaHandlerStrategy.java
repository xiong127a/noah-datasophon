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

package com.datasophon.worker.strategy;

import org.apache.pekko.actor.ActorRef;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.command.Sqlite3ExecCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.ActorUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class GrafanaHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public GrafanaHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        new ExecResult();
        ExecResult startResult;
        logger.info("GrafanaHandlerStrategy start grafana{}", JSONUtil.toJsonStr(command));
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());

        String localHostName = null;
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Failed to retrieve the local host name. The system could not resolve the hostname.", e);
        }
        String ip = NetUtil.getIpByHost(localHostName);
        if (command.getCommandType() == CommandType.INSTALL_SERVICE&& !command.isSlave() && StrUtil.equalsAny(command.getMasterHost(), localHostName, ip)) {
            logger.info("first start grafana");
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
            if (startResult.getExecResult()) {
                // Note: Grafana 数据源配置需要在 API 端手动配置或通过其他方式同步
                logger.info("Grafana start success");
            } else {
                logger.error("Grafana start failed");
            }
        } else {
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
        }
        return startResult;
    }


}
