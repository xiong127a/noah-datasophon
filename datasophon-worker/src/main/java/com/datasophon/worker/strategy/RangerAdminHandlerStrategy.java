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

import java.util.ArrayList;

public class RangerAdminHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public RangerAdminHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());

        if (command.getEnableKerberos()) {
            logger.info("start to get ranger keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if (!FileUtil.exist("/etc/security/keytab/rangeradmin.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("rangeradmin/" + hostname, "rangeradmin.keytab");
            }
        }

        ShellUtils.exceShell("mv " + workPath + "/ranger-2.1.0-usersync/install.properties1 " + workPath + "/ranger-2.1.0-usersync/install.properties");
        ShellUtils.exceShell("chmod 755 " + workPath + "/ranger-2.1.0-usersync/install.properties");

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) && command.getServiceRoleName().equals("RangerUsersync")) {
            logger.info("setup ranger user sync");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add("./setup.sh");
            ExecResult execResult = ShellUtils.execWithStatus(workPath + "/ranger-2.1.0-usersync", commands, 300L, logger);
            if (execResult.getExecResult()) {
                logger.info("setup ranger user sync success");
            } else {
                logger.error("setup ranger user sync failed");
                return execResult;
            }

            ShellUtils.exceShell("sed -i '/<name>ranger\\.usersync\\.enabled<\\/name>/{n;s/<value>false<\\/value>/<value>true<\\/value>/}' "
                    + workPath +
                    "/ranger-2.1.0-usersync/conf/ranger-ugsync-site.xml");
        }
        ShellUtils.exceShell("mv " + workPath + "/ranger-2.1.0-kms/install.properties2 " + workPath + "/ranger-2.1.0-kms/install.properties");
        ShellUtils.exceShell("chmod 755 " + workPath + "/ranger-2.1.0-kms/install.properties");

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE) && command.getServiceRoleName().equals("RangerKms")) {
            logger.info("setup ranger kms");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add("./setup.sh");
            ExecResult execResult = ShellUtils.execWithStatus(workPath + "/ranger-2.1.0-kms", commands, 300L, logger);
            if (!execResult.getExecResult()) {
                logger.error("setup ranger kms failed, sh setup.sh error");
                return execResult;
            }
            commands.clear();
            commands.add("sh");
            commands.add("./enable-kms-plugin.sh");
            execResult = ShellUtils.execWithStatus(workPath + "/ranger-2.1.0-kms", commands, 300L, logger);
            if (execResult.getExecResult()) {
                logger.info("setup ranger kms success");
            } else {
                logger.error("setup ranger kms failed, sh enable-kms-plugin.sh error");
                return execResult;
            }
        }

        startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());

        return startResult;
    }

}
