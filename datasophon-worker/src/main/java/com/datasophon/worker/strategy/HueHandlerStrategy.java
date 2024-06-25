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

public class HueHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public HueHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ExecResult execResult;
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());

        if (command.getEnableKerberos()) {
            logger.info("start to get hue keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KerberosUtils.createKeytabDir();
            if (!FileUtil.exist("/opt/datasophon/hue/hue.service.keytab")) {
                KerberosUtils.downloadKeytabFromMaster("hue/" + hostname, "hue.service.keytab");
                ShellUtils.exceShell("cp /etc/security/keytab/hue.service.keytab /opt/datasophon/hue/hue.service.keytab");
                ShellUtils.exceShell("chmod 777 /opt/datasophon/hue/hue.service.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            ShellUtils.exceShell("yum -y install cyrus-sasl-plain  cyrus-sasl-devel  cyrus-sasl-gssapi --skip-broken");

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
            execResult = ShellUtils.execWithStatus(workPath + "/build/env/bin/", commands, 600L);
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
