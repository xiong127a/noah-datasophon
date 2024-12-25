package com.datasophon.worker.strategy;

import cn.hutool.core.util.StrUtil;
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
        String version = StrUtil.subAfter(command.getDecompressPackageName(), "-", false);
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ArrayList<String> commands = new ArrayList<>();

            logger.info("/clickhouse-common-static-{}/install/doinst.sh", version);
            commands.add(workPath + "/clickhouse-common-static-" + version + "/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse common static install success");


            logger.info("/clickhouse-common-static-dbg-{}/install/doinst.sh", version);
            commands.clear();
            commands.add(workPath + "/clickhouse-common-static-dbg-" + version + "/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse common static dbg install success");


            logger.info("/clickhouse-server-{}/install/doinst.sh configure", version);
            commands.clear();
            commands.add(workPath + "/clickhouse-server-" + version + "/install/doinst.sh");
            commands.add("configure");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);

            // 配置文件操作
            ShellUtils.exceShell("rm -rf /etc/clickhouse-server/config.xml");
            ShellUtils.exceShell("rm -rf /etc/clickhouse-server/users.xml");
            ShellUtils.exceShell("cp " + workPath + "/etc/config.xml /etc/clickhouse-server");
            ShellUtils.exceShell("cp " + workPath + "/etc/users.xml /etc/clickhouse-server");
            ShellUtils.exceShell("chown clickhouse:clickhouse /etc/clickhouse-server/config.xml /etc/clickhouse-server/users.xml");
            logger.info("clickhouse server install success");


            logger.info("/clickhouse-client-{}/install/doinst.sh", version);
            commands.clear();
            commands.add(workPath + "/clickhouse-client-" + version + "/install/doinst.sh");
            ShellUtils.execWithStatus(workPath, commands, 300L, logger);
            logger.info("clickhouse client install success");

            // 启动服务
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
