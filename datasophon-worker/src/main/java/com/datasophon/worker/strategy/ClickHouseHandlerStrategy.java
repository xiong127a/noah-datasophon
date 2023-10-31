package com.datasophon.worker.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.sql.SQLException;
import java.util.ArrayList;

public class ClickHouseHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public ClickHouseHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ArrayList<String> commands = new ArrayList<>();

            logger.info("/clickhouse-common-static-23.9.1.1854/install/doinst.sh");
            commands.add(workPath + "/clickhouse-common-static-23.9.1.1854/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse common static install success");

            logger.info("/clickhouse-common-static-dbg-23.9.1.1854/install/doinst.sh");
            commands.clear();
            commands.add(workPath + "/clickhouse-common-static-dbg-23.9.1.1854/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse common static dbg install success");

            logger.info("/clickhouse-server-23.9.1.1854/install/doinst.sh configure");
            commands.clear();
            commands.add(workPath + "/clickhouse-server-23.9.1.1854/install/doinst.sh");
            commands.add("configure");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);

            ShellUtils.exceShell("rm -rf /etc/clickhouse-server/config.xml");
            ShellUtils.exceShell("rm -rf /etc/clickhouse-server/users.xml");
            ShellUtils.exceShell("cp " + workPath + "/etc/config.xml /etc/clickhouse-server");
            ShellUtils.exceShell("cp " + workPath + "/etc/users.xml /etc/clickhouse-server");
            ShellUtils.exceShell("chown clickhouse:clickhouse /etc/clickhouse-server/config.xml /etc/clickhouse-server/users.xml");
            logger.info("clickhouse server install success");

            logger.info("/clickhouse-client-23.9.1.1854/install/doinst.sh");
            commands.clear();
            commands.add(workPath + "/clickhouse-client-23.9.1.1854/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse client install success");

            commands.clear();
            commands.add("sudo");
            commands.add("/etc/init.d/clickhouse-server");
            commands.add("start");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse start success");
        }

        ExecResult startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
        return startResult;
    }
}
