package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sZKFCHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        if (!command.isSlave() && command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("start to execute hdfs zkfc -formatZK");
            VolumeMountDTO[] volumeMounts = volumeMountList(workPath, command.getConfigFileMap());
            String jobCmd = workPath + "/bin/hdfs" + " zkfc " + "-formatZK";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "zkfc-format",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        logger,
                        command.getHostname()
                );
                logger.info("zkfc format success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("zkfc format failed");
                startResult.setExecResult(false);
            }
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }
}
