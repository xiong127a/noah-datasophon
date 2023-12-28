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

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.KerberosUtils;

import java.sql.SQLException;
import java.util.ArrayList;

public class ResourceManagerHandlerStrategy extends  AbstractHandlerStrategy implements ServiceRoleStrategy {

    public ResourceManagerHandlerStrategy(String serviceName,String serviceRoleName) {
        super(serviceName,serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (command.getEnableKerberos()) {
            logger.info("start to get resourcemanager keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if (!FileUtil.exist("/etc/security/keytab/rm.service.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("rm/" + hostname, "rm.service.keytab");
            }
        }
        String hadoopHome = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            // create /user/yarn
            ShellUtils.exceShell("sudo -u hdfs " + hadoopHome + "/bin/hdfs dfs -mkdir -p /user/yarn");
            ShellUtils.exceShell("sudo -u hdfs " + hadoopHome + "/bin/hdfs dfs -chown yarn:hadoop /user/yarn");

            // 存在 tez 则创建软连接
            final String tezHomePath = Constants.INSTALL_PATH + Constants.SLASH + "tez";
            if (FileUtil.exist(tezHomePath)) {
                ShellUtils.exceShell("ln -s " + tezHomePath + "/conf/tez-site.xml " + workPath + "/etc/hadoop/tez-site.xml");
            }
        }
        if (command.getEnableRangerPlugin()) {
            logger.info("Start to enable ranger yarn plugin");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add(workPath + "/ranger-yarn-plugin/enable-yarn-plugin.sh");
            if (!FileUtil.exist(workPath + "/ranger-yarn-plugin/success.id")) {
                ExecResult execResult = ShellUtils.execWithStatus(workPath + "/ranger-yarn-plugin", commands, 30L, logger);
                if (execResult.getExecResult()) {
                    logger.info("Enable ranger yarn plugin success");
                    // 写入ranger plugin集成成功标识
                    FileUtil.writeUtf8String("success", workPath + "/ranger-yarn-plugin/success.id");
                } else {
                    logger.info("Enable ranger yarn plugin failed");
                    return execResult;
                }
            }
        }
        return serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
    }
}
