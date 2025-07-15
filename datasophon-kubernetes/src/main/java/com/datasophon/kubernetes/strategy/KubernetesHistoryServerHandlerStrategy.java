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
import com.datasophon.kubernetes.util.KubernetesKerberosUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.datasophon.kubernetes.util.KubernetesUtil;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;

public class KubernetesHistoryServerHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesHistoryServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        String hostname = command.getHostname();
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String jobCmd="";
        if (command.getEnableKerberos()) {
            logger.info("start to get jobhistoryserver keytab file");
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/security/keytab/jhs.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname, "jhs/" + hostname, "jhs.service.keytab");
            }
            jobCmd= "su - hdfs -c \"kinit -kt /etc/security/keytab/spnego.service.keytab HTTP/"+hostname+"@HADOOP.COM && kinit -kt /etc/security/keytab/hdfs.user.keytab hdfs/user@HADOOP.COM\" && ";
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            String hdfsCmdPrefix = "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs ";
            jobCmd += hdfsCmdPrefix + "-test -e /user/yarn/yarn-logs\" || (" +
                    hdfsCmdPrefix + "-mkdir -p /user/yarn/yarn-logs\" && " +
                    hdfsCmdPrefix + "-chown yarn:hadoop /user/yarn/yarn-logs\") && " +
                    hdfsCmdPrefix + "-test -e /tmp\" || (" +
                    hdfsCmdPrefix + "-mkdir /tmp\" && " +
                    hdfsCmdPrefix + "-chmod 777 /tmp\")\n";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())){
                KubernetesUtil.runCmd(
                        Constants.DATASOPHON,
                        kubeClient,
                        "hdfs-namenode",
                        command.getNnHost(),
                        jobCmd);
                logger.info("create jobhistoryserver dir success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("create jobhistoryserver dir failed");
                startResult.setExecResult(false);
                return startResult;
            }
        }
        return serviceHandler.start(command);
    }
}
