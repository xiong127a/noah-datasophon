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

package com.datasophon.kubernetes.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;


public class KubernetesStorageHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesStorageHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        new ExecResult();
        ExecResult startResult;
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = command.getHostname();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ArrayList<String> commands = getCommands(command, workPath, hostname);

            startResult = serviceHandler.start(command);
            if (startResult.getExecResult()) {
                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                    KubernetesUtil.runCmd(
                            command.getNamespace(),
                            kubeClient,
                            "nebulagraph-graph",
                            command.getGraphHost(),
                            String.join(" ", commands));
                    logger.info("add storage success");
                    startResult.setExecResult(true);
                } catch (Exception e) {
                    logger.info("add storage failed");
                    startResult.setExecResult(false);
                    return startResult;
                }
            } else {
                logger.error("add storage failed");
            }
        } else {
            startResult = serviceHandler.start(command);
        }
        return startResult;
    }

    private static ArrayList<String> getCommands(KubernetesServiceRoleOperateCommand command, String workPath, String hostname) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(workPath + "/bin/nebula-console");
        commands.add("--addr");
        commands.add(command.getGraphHost());

        commands.add("--port");
        commands.add("9669");

        commands.add("-u");
        commands.add("root");

        commands.add("-p");
        commands.add("nebula");

        commands.add("-e");
        commands.add("\"ADD HOSTS");
        commands.add("'" + hostname + "':9779\"");
        return commands;
    }
}
