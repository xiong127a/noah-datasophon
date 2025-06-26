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

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class K8sSRBEHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRBEHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("add be to cluster");
            if (startResult.getExecResult()) {
                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {

                    Object podNamesObj = CacheUtils.get(serviceRoleFullName + "_" + Constant.POD_NAME);

                    List<String> podNames =(List<String>) podNamesObj;

                    if (podNames == null || podNames.isEmpty()) {
                        return startResult;
                    }

                    StringBuilder beNodes = new StringBuilder();
                    for (int i = 0; i < podNames.size(); i++) {
                        if (i > 0) {
                            beNodes.append(",");
                        }
                        beNodes.append("\\\"").append(podNames.get(i)).append(".").append(serviceRoleFullName)
                                .append(".").append(Constants.DATASOPHON).append(".svc.cluster.local:9050\\\"");
                    }

                    String mysqlCmd = "mysql -h127.0.0.1 -P9030 -uroot -p -e \"ALTER SYSTEM add BACKEND " + beNodes.toString() + "\"";

                    startResult = K8sUtil.runCmd(
                            Constants.DATASOPHON,
                            kubeClient,
                            "starrocks-srfe",
                            command.getMasterHost(), // 在主FE上执行注册命令
                            mysqlCmd
                    );
                } catch (Exception e) {
                    logger.error("Add BE failed: {}", ThrowableUtils.getStackTrace(e));
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }
                logger.info("slave be start success");
            } else {
                logger.error("slave be start failed");
            }
        }
        return startResult;
    }
}
