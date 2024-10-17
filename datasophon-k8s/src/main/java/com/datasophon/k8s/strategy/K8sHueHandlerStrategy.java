package com.datasophon.k8s.strategy;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.*;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.sql.SQLException;

public class K8sHueHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sHueHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        ExecResult startResult = new ExecResult();
        final String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());

        if (command.getEnableKerberos()) {
            logger.info("start to get hue keytab file");
            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            K8sKerberosUtils.createKeytabDir(hostname);
            if (!FileUtil.exist("/opt/datasophon/hue/hue.service.keytab")) {
                K8sKerberosUtils.downloadKeytabFromMaster(hostname,"hue/" + hostname, "hue.service.keytab");
                K8sMinaUtils.execCmdWithResult(hostname,"cp /etc/security/keytab/hue.service.keytab /opt/datasophon/hue/hue.service.keytab");
                K8sMinaUtils.execCmdWithResult(hostname,"chmod 777 /opt/datasophon/hue/hue.service.keytab");
            }
        }

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {

            logger.info("init hue database");
            String initCommand = "cd " + workPath + "/build/env/bin/ && "
                    + "su - hue -c \"./hue syncdb\" && "
                    + "su - hue -c \"./hue migrate\"";
            VolumeMountDTO[] volumeMounts = volumeMountList(workPath, command.getConfigFileMap(),command.getEnableKerberos());
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "hue-database-init",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        initCommand,
                        logger,
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
