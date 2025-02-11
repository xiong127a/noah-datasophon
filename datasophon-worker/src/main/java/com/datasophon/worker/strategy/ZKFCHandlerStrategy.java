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
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.util.ArrayList;
import java.util.List;

public class ZKFCHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    private static final String CONTROL_SCRIPT = "control_hadoop.sh";
    private static final String ZKFC_STATUS_CMD = "status";
    private static final String ZKFC_STOP_CMD = "stop";
    private static final long STATUS_CHECK_TIMEOUT = 30L;
    private static final long STOP_SERVICE_TIMEOUT = 60L;

    public ZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();

        try {
            if (shouldHandleCommand(command)) {
                if (isZkfcRunning(workPath)) {
                    stopZkfcService(workPath);
                }

                ExecResult formatResult = formatZkfc(workPath);
                if (!formatResult.getExecResult()) {
                    return formatResult;
                }
            }

            startResult = startService(serviceHandler, command);
        } catch (Exception e) {
            logger.error("ZKFC handler error", e);
            startResult.setExecResult(false);
            startResult.setExecErrOut(e.getMessage());
        }

        return startResult;
    }

    private boolean shouldHandleCommand(ServiceRoleOperateCommand command) {
        return !command.isSlave()
                && command.getCommandType().equals(CommandType.INSTALL_SERVICE);
    }

    private boolean isZkfcRunning(String workPath) {
        List<String> statusCommand = buildControlCommand(workPath, ZKFC_STATUS_CMD);
        ExecResult statusResult = ShellUtils.execWithStatus(workPath, statusCommand, STATUS_CHECK_TIMEOUT, logger);

        if (statusResult.getExecResult()) {
            logger.info("ZKFC service is running");
            return true;
        }

        logger.info("ZKFC service is not running");
        return false;
    }

    private ExecResult stopZkfcService(String workPath) {
        List<String> stopCommand = buildControlCommand(workPath, ZKFC_STOP_CMD);
        ExecResult stopResult = ShellUtils.execWithStatus(workPath, stopCommand, STOP_SERVICE_TIMEOUT, logger);

        if (!stopResult.getExecResult()) {
            logger.error("Failed to stop ZKFC service");
            return stopResult;
        }

        logger.info("ZKFC service stopped successfully");
        return stopResult;
    }

    private ExecResult formatZkfc(String workPath) {
        logger.info("Starting to format ZKFC");
        List<String> formatCommand = new ArrayList<>();
        formatCommand.add(workPath + "/bin/hdfs");
        formatCommand.add("zkfc");
        formatCommand.add("-formatZK");

        ExecResult formatResult = ShellUtils.execWithStatus(workPath, formatCommand, 300L, logger);
        if (formatResult.getExecResult()) {
            logger.info("ZKFC format succeeded");
        } else {
            logger.error("ZKFC format failed");
        }
        return formatResult;
    }

    private List<String> buildControlCommand(String workPath, String action) {
        List<String> command = new ArrayList<>();
        command.add(workPath + "/" + CONTROL_SCRIPT);
        command.add(action);
        command.add("zkfc");
        return command;
    }

    private ExecResult startService(ServiceHandler serviceHandler, ServiceRoleOperateCommand command) {
        return serviceHandler.start(
                command.getStartRunner(),
                command.getStatusRunner(),
                command.getDecompressPackageName(),
                command.getRunAs());
    }
}
