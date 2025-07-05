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

import cn.hutool.core.util.BooleanUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * TEZ Server 启动支持类
 *
 * @author zhenqin
 */
public class K8sTezServerHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sTezServerHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ExecResult startResult = serviceHandler.start(command);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            if (BooleanUtil.isFalse(startResult.getExecResult())) {
                logger.error("start tez server failed");
                startResult.setExecResult(false);
                return startResult;
            }
            logger.info("start tez server success");
            final String hadoopHome = PropertyUtils.getString("HADOOP_HOME");
            final String tezLibPath = Optional.ofNullable(StringUtils.trimToNull(createEnvPath(workPath))).orElse("hdfs:///user/tez/tez.tar.gz");
            final String tezLibParentDir = new Path(URI.create(tezLibPath).getPath()).getParent().toString();
            logger.info("Start to execute hdfs dfs -mkdir {}", tezLibParentDir);
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                // 提取用户信息并进行一次判断
                boolean needSwitchUser = Objects.nonNull(command.getRunAs()) && StringUtils.isNotBlank(command.getRunAs().getUser());
                String runAsUser = Objects.nonNull(command.getRunAs()) ? command.getRunAs().getUser() : null;

                // 拼接命令，依次执行 mkdir, chown 和 put 操作
                StringBuilder fullCmd = new StringBuilder();

                // 第一个命令：创建目录
                String mkdirCmd = hadoopHome + "/bin/hdfs dfs -mkdir -p " + tezLibParentDir;
                if (needSwitchUser) {
                    mkdirCmd = "su - " + runAsUser + " -c '" + mkdirCmd + "'";
                }
                fullCmd.append(mkdirCmd);

                // 第二个命令：改变文件的所有者
                if (needSwitchUser) {
                    String chownCmd = hadoopHome + "/bin/hdfs dfs -chown " + runAsUser + ":" + command.getRunAs().getGroup() + " " + tezLibParentDir;
                    chownCmd = "su - " + runAsUser + " -c '" + chownCmd + "'";
                    fullCmd.append(" && ").append(chownCmd); // 使用 "&&" 确保命令按顺序执行
                }

                // 第三步：检查文件是否已存在
                String checkFileExistCmd = hadoopHome + "/bin/hdfs dfs -test -e " + tezLibParentDir + "/tez.tar.gz";
                if (needSwitchUser) {
                    checkFileExistCmd = "su - " + runAsUser + " -c '" + checkFileExistCmd + "'";
                }

                // 如果文件已存在，则跳过上传，否则执行上传
                String putCmd = hadoopHome + "/bin/hdfs dfs -put " + workPath + "/share/tez.tar.gz " + tezLibParentDir;
                if (needSwitchUser) {
                    putCmd = "su - " + runAsUser + " -c '" + putCmd + "'"; // 如果指定了用户，使用 su 切换用户
                }

                fullCmd.append(" && ").append(checkFileExistCmd);
                fullCmd.append(" && if [ $? -ne 0 ]; then "); // 如果文件不存在，则执行上传
                fullCmd.append(putCmd);
                fullCmd.append("; fi"); // 否则跳过上传操作

                // 执行拼接后的命令
                K8sUtil.runCmd(
                        Constants.DATASOPHON,
                        kubeClient,
                        (command.getServiceName() + "-" + command.getServiceRoleName()).toLowerCase(),
                        command.getHostname(),
                        fullCmd.toString()
                );

                logger.info("Init hive dir success");
                logger.info("Uploaded tez.tar.gz to {} output: {}", tezLibParentDir, startResult.getExecOut());
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.error("init hive dir failed");
                startResult.setExecResult(false);
                return startResult;
            }
        }
        return startResult;
    }


    /**
     * tez 的元数据
     *
     * @param workPath
     */
    String createEnvPath(final String workPath) {
        Configuration conf = new Configuration();
        try {
            final File tezSiteFile = new File(workPath, "conf/tez-site.xml");
            if (tezSiteFile.exists()) {
                conf.addResource(tezSiteFile.toURL());
                logger.info("add tez-site file: {}", tezSiteFile.getAbsolutePath());
            }

            // tez lib uri 启动清理
            String tezLibPath = conf.get("tez.lib.uris");
            return tezLibPath;
        } catch (Exception e) {
        }
        return null;
    }
}
