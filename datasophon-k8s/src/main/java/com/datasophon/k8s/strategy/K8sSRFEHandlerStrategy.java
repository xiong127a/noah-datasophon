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
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import com.datasophon.common.utils.ThrowableUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

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
        startResult = serviceHandler.start(command);
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
            if (command.isSlave()) {
                if (startResult.getExecResult()) {
                    // add follower
                    try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {

                        // 拼接完整的mysql命令语句
                        String mysqlCmd = "mysql -h127.0.0.1 -P9030 -uroot -p -e \"ALTER SYSTEM add FOLLOWER \\\"" + serviceRoleFullName + "-1."+serviceRoleFullName+"."+Constants.DATASOPHON+".svc.cluster.local:9010\\\"\"";

                        // 进入容器执行mysql命令添加follower
                        startResult = K8sUtil.runCmd(
                                Constants.DATASOPHON,
                                kubeClient,
                                serviceRoleFullName,
                                command.getMasterHost(),
                                mysqlCmd
                        );
                    } catch (Exception e) {
                        logger.error("Add slave FE failed: {}", ThrowableUtils.getStackTrace(e));
                        startResult.setExecResult(false);
                        startResult.setExecOut(e.getMessage());
                    }
                    logger.info("slave fe start success");
                } else {
                    logger.error("slave fe start failed");
                }
            }
        }
        return startResult;
    }
}
