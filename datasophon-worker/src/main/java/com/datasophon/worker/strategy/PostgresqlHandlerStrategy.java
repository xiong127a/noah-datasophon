package com.datasophon.worker.strategy;

import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.sql.SQLException;

public class PostgresqlHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public PostgresqlHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ExecResult execResult;
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        if (command.getCommandType() == CommandType.INSTALL_SERVICE ) {

            ShellUtils.exceShell("chmod -R 700 /opt/datasophon/postgresql-16.1/data/");

            if ("PostgresqlWorker".equals(command.getServiceRoleName())) {
                ShellUtils.exceShell("rm -rf /opt/datasophon/postgresql-16.1/data/*");
                String backupShell =
                        "sudo -u postgres /opt/datasophon/postgresql-16.1/bin/pg_basebackup -h "
                                + command.getMasterHost() +
                                " -U postgres -F p -X s -v -P -R -D /opt/datasophon/postgresql-16.1/data";
                logger.info(backupShell);
                execResult = ShellUtils.exceShell(backupShell);
                if (!execResult.getExecResult()) {
                    logger.error("copy data from master failed --> {}", execResult.getExecErrOut());
                    return execResult;
                } else {
                    logger.info("copy data from master success");
                }
            }

        }
        execResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                    command.getDecompressPackageName(), command.getRunAs());
        return execResult;
    }
}