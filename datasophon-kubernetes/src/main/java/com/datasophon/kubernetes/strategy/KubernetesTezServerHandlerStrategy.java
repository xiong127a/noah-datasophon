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

import cn.hutool.core.util.BooleanUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * TEZ Server handler strategy implementation
 * Responsible for handling TEZ service role operations in Kubernetes
 * environment
 *
 * @author zhenqin
 */
public class KubernetesTezServerHandlerStrategy extends KubernetesAbstractHandlerStrategy
        implements KubernetesServiceRoleStrategy {

    public KubernetesTezServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    /**
     * Handles the Kubernetes service role operation command
     * 
     * @param command The command containing operation details
     * @return Execution result of the operation
     */
    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ExecResult startResult = serviceHandler.start(command);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            if (BooleanUtil.isFalse(startResult.getExecResult())) {
                logger.error("Failed to start TEZ server");
                startResult.setExecResult(false);
                return startResult;
            }
            logger.info("Successfully started TEZ server");
            final String hadoopHome = PropertyUtils.getString("HADOOP_HOME");
            final String tezLibPath = Optional.ofNullable(StringUtils.trimToNull(createEnvPath(workPath)))
                    .orElse("hdfs:///user/tez/tez.tar.gz");
            final String tezLibParentDir = new Path(URI.create(tezLibPath).getPath()).getParent().toString();
            logger.info("Preparing to create HDFS directory: {}", tezLibParentDir);
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                // Check user information and determine if user switch is needed
                boolean needSwitchUser = Objects.nonNull(command.getRunAs())
                        && StringUtils.isNotBlank(command.getRunAs().getUser());
                String runAsUser = Objects.nonNull(command.getRunAs()) ? command.getRunAs().getUser() : null;

                // Build command sequence to execute mkdir, chown, and put operations
                StringBuilder fullCmd = new StringBuilder();

                // First command: Create directory
                String mkdirCmd = hadoopHome + "/bin/hdfs dfs -mkdir -p " + tezLibParentDir;
                if (needSwitchUser) {
                    mkdirCmd = "su - " + runAsUser + " -c '" + mkdirCmd + "'";
                }
                fullCmd.append(mkdirCmd);

                // Second command: Change directory ownership
                if (needSwitchUser) {
                    String chownCmd = hadoopHome + "/bin/hdfs dfs -chown " + runAsUser + ":"
                            + command.getRunAs().getGroup() + " " + tezLibParentDir;
                    chownCmd = "su - " + runAsUser + " -c '" + chownCmd + "'";
                    fullCmd.append(" && ").append(chownCmd); // Use "&&" to ensure sequential execution
                }

                // Third step: Check if file already exists
                String checkFileExistCmd = hadoopHome + "/bin/hdfs dfs -test -e " + tezLibParentDir + "/tez.tar.gz";
                if (needSwitchUser) {
                    checkFileExistCmd = "su - " + runAsUser + " -c '" + checkFileExistCmd + "'";
                }

                // If the file exists, skip upload, otherwise perform upload
                String putCmd = hadoopHome + "/bin/hdfs dfs -put " + workPath + "/share/tez.tar.gz " + tezLibParentDir;
                if (needSwitchUser) {
                    putCmd = "su - " + runAsUser + " -c '" + putCmd + "'"; // Use su to switch user if specified
                }

                fullCmd.append(" && ").append(checkFileExistCmd);
                fullCmd.append(" && if [ $? -ne 0 ]; then "); // If file doesn't exist, perform upload
                fullCmd.append(putCmd);
                fullCmd.append("; fi"); // Otherwise skip upload operation

                // Execute the combined command
                ExecResult execResult = KubernetesUtil.runCmd(
                        command.getNamespace(),
                        kubeClient,
                        (command.getServiceName() + "-" + command.getServiceRoleName()).toLowerCase(),
                        command.getHostname(),
                        fullCmd.toString());

                logger.info("TEZ directory initialization successful");
                logger.info("Uploaded tez.tar.gz to {}. Command output: {}", tezLibParentDir,
                        execResult.getExecOut() != null
                                ? execResult.getExecOut().substring(0, Math.min(execResult.getExecOut().length(), 100))
                                : "no output");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.error("Failed to initialize TEZ directory", e);
                startResult.setExecResult(false);
                return startResult;
            }
        }
        return startResult;
    }

    /**
     * Creates the environment path for TEZ metadata
     * Reads tez.lib.uris from tez-site.xml configuration
     *
     * @param workPath The base work directory path
     * @return The TEZ library URIs from configuration, or null if not found
     */
    String createEnvPath(final String workPath) {
        Configuration conf = new Configuration();
        try {
            final File tezSiteFile = new File(workPath, "conf/tez-site.xml");
            if (tezSiteFile.exists()) {
                // Fixed: Using toURI().toURL() instead of deprecated toURL()
                conf.addResource(tezSiteFile.toURI().toURL());
                logger.info("Added tez-site configuration file: {}", tezSiteFile.getAbsolutePath());
            } else {
                logger.warn("tez-site.xml not found at: {}", tezSiteFile.getAbsolutePath());
                return null;
            }

            // Get TEZ library URI for startup
            String tezLibUri = conf.get("tez.lib.uris");
            if (tezLibUri == null) {
                logger.warn("tez.lib.uris not found in tez-site.xml configuration");
            } else {
                logger.debug("Found tez.lib.uris: {}", tezLibUri);
            }
            return tezLibUri;
        } catch (Exception e) {
            logger.error("Error reading tez-site.xml configuration", e);
            return null;
        }
    }
}
