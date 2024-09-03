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

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;

public class K8sHistoryServerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sHistoryServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws IOException {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getEnableKerberos()) {
            logger.info("start to get jobhistoryserver keytab file");
            String hostname = command.getHostname();
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/security/keytab/jhs.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname, "jhs/" + hostname, "jhs.service.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            String coreSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml";
            String hdfsSite = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml";
            String hadoopEnv = "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh";
            VolumeMountDTO[] volumeMounts = {
                    new VolumeMountDTO("core-site", coreSite, coreSite),
                    new VolumeMountDTO("hdfs-site", hdfsSite, hdfsSite),
                    new VolumeMountDTO("hadoop-env", hadoopEnv, hadoopEnv),
            };
            String jobCmd =
                    "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -test -e /user/yarn/yarn-logs\" || (" +
                            "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -mkdir -p /user/yarn/yarn-logs\" && " +
                            "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -chown yarn:hadoop /user/yarn/yarn-logs\" ) && " +
                            "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -test -e /tmp\" || (" +
                            "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -mkdir /tmp\" && " +
                            "su - hdfs -c \"/opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -chmod 777 /tmp\" )\n";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())){
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "create-jobhistoryserver-dir",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
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
