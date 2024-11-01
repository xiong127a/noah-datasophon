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

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.util.ArrayList;


public class StorageHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public StorageHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName,serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        String hostname = CacheUtils.getString(Constants.HOSTNAME);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add(workPath+"/bin/nebula-console");
            commands.add("--addr");
            commands.add(command.getGraphHost());

            commands.add("--port");
            commands.add("9669");

            commands.add("-u");
            commands.add("root");

            commands.add("-p");
            commands.add("nebula");

            commands.add("-e");
            commands.add("\"ADD HOSTS");
            commands.add("'"+hostname+"':9779\"");

            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
            if (startResult.getExecResult()) {
                ShellUtils.exceShell(String.join(" ",commands));
            } else {
                logger.error("add storage failed");
            }
        } else {
            startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
        }
        return startResult;
    }
}
