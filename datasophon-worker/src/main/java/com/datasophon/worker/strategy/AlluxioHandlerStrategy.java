package com.datasophon.worker.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.sql.SQLException;
import java.util.ArrayList;

public class AlluxioHandlerStrategy  extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public AlluxioHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ArrayList<String> commands = new ArrayList<>();

            logger.info("start format master");
            commands.add(workPath + "/bin/alluxio");
            commands.add("format");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("alluxio master format success");

            commands.clear();
            commands.add(workPath + "/alluxio/bin/alluxio-start.sh");
            commands.add("all");
            ExecResult execResult = ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            if (execResult.getExecResult()) {
                logger.info("alluxio start all success");
            }
        }

        return serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
    }
}
