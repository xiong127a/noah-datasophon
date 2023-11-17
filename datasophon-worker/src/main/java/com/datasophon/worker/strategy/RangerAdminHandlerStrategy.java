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

    public RangerAdminHandlerStrategy(String serviceName,String serviceRoleName) {
        super(serviceName,serviceRoleName);
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
            if (!FileUtil.exist("/etc/security/keytab/spnego.service.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("HTTP/" + hostname, "spnego.service.keytab");
            }
            if (!FileUtil.exist("/etc/security/keytab/rangeradmin.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("rangeradmin/" + hostname, "rangeradmin.keytab");
            }
        }
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("setup ranger user sync");
            ArrayList<String> commands = new ArrayList<>();
            commands.add(workPath + "/ranger-2.1.0-usersync/setup.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
//            ShellUtils.exceShell("sh " + workPath + "/ranger-2.1.0-usersync/setup.sh");
            logger.info("setup ranger user sync success");
            ShellUtils.exceShell("sed -i '/<name>ranger\\.usersync\\.enabled<\\/name>/{n;s/<value>false<\\/value>/<value>true<\\/value>/}' "
                    + workPath +
                    "/ranger-2.1.0-usersync/conf/ranger-ugsync-site.xml");
            ShellUtils.exceShell("mv " + workPath + "/ranger-2.1.0-usersync/install.properties1 " + workPath + "/ranger-2.1.0-usersync/install.properties");
            ShellUtils.exceShell("chmod 755 " + workPath + "/ranger-2.1.0-usersync/install.properties");
        }

        startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());

        return startResult;
    }

}
