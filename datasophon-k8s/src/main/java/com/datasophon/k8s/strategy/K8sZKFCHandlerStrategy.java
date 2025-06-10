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

import java.util.ArrayList;
import java.util.List;

public class K8sZKFCHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) {
        ExecResult startResult = new ExecResult();
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();

        if (!command.isSlave() && command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            logger.info("start to execute hdfs zkfc -formatZK");

            // 使用新方法创建ConfigMap挂载
            VolumeMountDTO[] volumeMounts = createZkfcConfigMapMounts(workPath);

            String jobCmd = workPath + "/bin/hdfs" + " zkfc " + "-formatZK";
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                logger.info("Running ZKFC format job with ConfigMap mounts");

                K8sUtil.runJob(
                        Constants.DATASOPHON,
                        "zkfc-format",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        command.getHostname());
                logger.info("zkfc format success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.error("zkfc format failed: {}", e.getMessage(), e);
                startResult.setExecResult(false);
                startResult.setExecOut(e.getMessage());
            }
        }

        startResult = serviceHandler.start(command);
        return startResult;
    }

    /**
     * 创建ZKFC服务所需的ConfigMap挂载列表
     * 
     * @param workPath HDFS安装路径
     * @return ConfigMap类型的VolumeMountDTO数组
     */
    private VolumeMountDTO[] createZkfcConfigMapMounts(String workPath) {
        logger.info("Creating ConfigMap mounts for ZKFC");
        List<VolumeMountDTO> mounts = new ArrayList<>();

        // 指定HDFS配置路径前缀
        String configPath = workPath + "/etc/hadoop/";

        // 添加NameNode的ConfigMap挂载
        // 注意：volumeName以"configmap-"开头表示这是ConfigMap类型
        // hostPath存储ConfigMap名称
        // containerPath是容器内的挂载路径

        // core-site.xml
        mounts.add(new VolumeMountDTO(
                "configmap-namenode-core-site-xml",
                "namenode-core-site-xml", // ConfigMap名称
                configPath + "core-site.xml" // 容器内路径
        ));

        // hdfs-site.xml
        mounts.add(new VolumeMountDTO(
                "configmap-namenode-hdfs-site-xml",
                "namenode-hdfs-site-xml",
                configPath + "hdfs-site.xml"));

        // hadoop-env.sh
        mounts.add(new VolumeMountDTO(
                "configmap-namenode-hadoop-env-sh",
                "namenode-hadoop-env-sh",
                configPath + "hadoop-env.sh"));

        // httpfs-site.xml
        mounts.add(new VolumeMountDTO(
                "configmap-namenode-httpfs-site-xml",
                "namenode-httpfs-site-xml",
                configPath + "httpfs-site.xml"));

        logger.info("Created {} ConfigMap mounts for ZKFC", mounts.size());
        return mounts.toArray(new VolumeMountDTO[0]);
    }
}
