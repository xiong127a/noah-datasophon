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
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Collections;
import java.util.List;

public class K8sSRFEObserverHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sSRFEObserverHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        logger.info("Start FE Observer installation: {}", JSONUtil.toJsonStr(command));
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        startResult = serviceHandler.start(command);
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
            if (startResult.getExecResult()) {
                try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                    Object podNamesObj = CacheUtils.get(serviceRoleFullName + "_" + Constant.POD_NAME);
                    List<String> podNames = (List<String>) podNamesObj;

                    if (podNames == null || podNames.isEmpty()) {
                        return startResult;
                    }

                    // 构建批量注册命令
                    StringBuilder batchCmd = new StringBuilder();
                    for (String podName : podNames) {
                        String observerAddr = String.format("%s.%s.%s.svc.cluster.local:9010",
                                podName, serviceRoleFullName, Constants.DATASOPHON);

                        batchCmd.append("mysql -h127.0.0.1 -P9030 -uroot -p -e \"")
                                .append("ALTER SYSTEM add OBSERVER '").append(observerAddr).append("';\" && ");
                    }

                    // 移除最后的 " && "
                    String finalCmd = batchCmd.substring(0, batchCmd.length() - 4);

                    startResult = K8sUtil.runCmd(
                            Constants.DATASOPHON,
                            kubeClient,
                            "starrocks-srfe",
                            command.getMasterHost(),
                            finalCmd
                    );
                } catch (Exception e) {
                    logger.error("Add Observer failed", e);
                    startResult.setExecResult(false);
                    startResult.setExecOut(e.getMessage());
                }

            }
        } else {
            startResult = serviceHandler.start(command);
        }

        logger.info("FE Observer installation {}", startResult.getExecResult() ? "succeeded" : "failed");
        return startResult;
    }

}
