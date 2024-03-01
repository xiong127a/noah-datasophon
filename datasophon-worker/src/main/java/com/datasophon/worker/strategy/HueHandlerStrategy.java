package com.datasophon.worker.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.sql.SQLException;
import java.util.ArrayList;

public class HueHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public HueHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult execResult;
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ShellUtils.exceShell("yum -y install cyrus-sasl-plain  cyrus-sasl-devel  cyrus-sasl-gssapi");

            logger.info("init hue database");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("sudo");
            commands.add("-u");
            commands.add("hue");
            commands.add("./hue");
            commands.add("syncdb");
            execResult = ShellUtils.execWithStatus(workPath + "/build/env/bin/", commands, 60L);
            if (!execResult.getExecResult()) {
                logger.error("init hue database hue syncdb failed");
                logger.error(execResult.getExecErrOut());
                return execResult;
            }
            commands.clear();
            commands.add("sudo");
            commands.add("-u");
            commands.add("hue");
            commands.add("./hue");
            commands.add("migrate");
            execResult = ShellUtils.execWithStatus(workPath + "/build/env/bin/", commands, 60L);
            if (!execResult.getExecResult()) {
                logger.error("init hue database hue migrate failed");
                logger.error(execResult.getExecErrOut());
                return execResult;
            }
            logger.info("init hue database success");

            execResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
            return execResult;
        }

        execResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
        return execResult;
    }

}
