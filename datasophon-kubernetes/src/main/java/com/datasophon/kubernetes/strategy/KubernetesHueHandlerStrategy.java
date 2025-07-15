package com.datasophon.kubernetes.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.sql.SQLException;

public class KubernetesHueHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public KubernetesHueHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        ExecResult startResult = new ExecResult();
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());

        if (command.getEnableKerberos()) {
            logger.info("start to get hue keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            KubernetesKerberosUtils.createKeytabDir(hostname);
            if (!FileUtil.exist("/opt/datasophon/hue/hue.service.keytab")) {
                KubernetesKerberosUtils.downloadKeytabFromMaster(hostname,"hue/" + hostname, "hue.service.keytab");
                KubernetesMinaUtils.execCmdWithResult(hostname,"cp /etc/security/keytab/hue.service.keytab /opt/datasophon/hue/hue.service.keytab");
                KubernetesMinaUtils.execCmdWithResult(hostname,"chmod 777 /opt/datasophon/hue/hue.service.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {

            logger.info("init hue database");
            String initCommand = "su - hue -c \"cd " + workPath + "/build/env/bin/ && "
                    + " ./hue syncdb && "
                    + " ./hue migrate\"";
            VolumeMountDTO[] volumeMounts = volumeMountList(workPath, command.getConfigFileMap(),command.getEnableKerberos());
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                KubernetesUtil.runJob(
                        Constants.DATASOPHON,
                        "hue-database-init",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        initCommand,
                        command.getHostname()
                );
                logger.info("hue database init success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("hue database init failed");
                startResult.setExecResult(false);
            }
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }

}
