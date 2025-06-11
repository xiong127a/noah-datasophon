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

                // 添加检查ZooKeeper和NameNode就绪状态的初始化容器
                List<String> initContainers = new ArrayList<>();
                List<String> initContainerNames = new ArrayList<>();

                // 1. 检查ZooKeeper集群就绪状态 - 使用BusyBox镜像
                initContainers.add(createZkReadinessCheck(workPath));
                initContainerNames.add("zk-readiness-check");

                // 2. 检查NameNode就绪状态 - 也使用BusyBox镜像
                initContainers.add(createNameNodeReadinessCheck(workPath, command.getServiceName()));
                initContainerNames.add("namenode-readiness-check");

                K8sUtil.runJobWithInitContainers(
                        Constants.DATASOPHON,
                        "zkfc-format",
                        kubeClient,
                        volumeMounts,
                        DockerImageUtils.getString(command.getServiceName()),
                        jobCmd,
                        command.getHostname(),
                        initContainers,
                        initContainerNames,
                        DockerImageUtils.getString("BUSYBOX"));
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

    /**
     * 创建检查ZooKeeper集群就绪状态的初始化容器命令
     * 
     * @param workPath HDFS安装路径
     * @return 初始化容器执行的命令
     */
    private String createZkReadinessCheck(String workPath) {
        StringBuilder command = new StringBuilder();
        command.append("echo \"正在检查ZooKeeper集群就绪状态...\";\n");
        command.append("ZOOKEEPER_SERVERS=$(grep -oP 'ha\\.zookeeper\\.quorum=\\K.*' ")
                .append(workPath).append("/etc/hadoop/core-site.xml | tr ',' ' ');\n");
        command.append("if [ -z \"$ZOOKEEPER_SERVERS\" ]; then\n");
        command.append("  echo \"错误: 无法从配置中获取ZooKeeper服务器列表\";\n");
        command.append("  exit 1;\n");
        command.append("fi;\n");
        command.append("echo \"检测到的ZooKeeper服务器: $ZOOKEEPER_SERVERS\";\n");
        command.append("for ZK_SERVER in $ZOOKEEPER_SERVERS; do\n");
        command.append("  ZK_HOST=$(echo $ZK_SERVER | cut -d':' -f1);\n");
        command.append("  ZK_PORT=$(echo $ZK_SERVER | cut -d':' -f2);\n");
        command.append("  echo \"正在检查ZooKeeper服务器: $ZK_HOST:$ZK_PORT\";\n");
        command.append("  RETRIES=0;\n");
        command.append("  MAX_RETRIES=60;\n");
        command.append("  while [ $RETRIES -lt $MAX_RETRIES ]; do\n");
        command.append("    if echo ruok | nc $ZK_HOST $ZK_PORT 2>/dev/null | grep -q imok; then\n");
        command.append("      echo \"ZooKeeper服务器 $ZK_HOST:$ZK_PORT 已就绪\";\n");
        command.append("      break;\n");
        command.append("    else\n");
        command.append("      echo \"ZooKeeper服务器 $ZK_HOST:$ZK_PORT 未就绪, 等待重试... ($((RETRIES+1))/$MAX_RETRIES)\";\n");
        command.append("      RETRIES=$((RETRIES+1));\n");
        command.append("      sleep 5;\n");
        command.append("    fi;\n");
        command.append("  done;\n");
        command.append("  if [ $RETRIES -eq $MAX_RETRIES ]; then\n");
        command.append("    echo \"错误: ZooKeeper服务器 $ZK_HOST:$ZK_PORT 在最大重试次数后仍未就绪\";\n");
        command.append("    exit 1;\n");
        command.append("  fi;\n");
        command.append("done;\n");
        command.append("echo \"所有ZooKeeper服务器已就绪\";\n");

        return command.toString();
    }

    /**
     * 创建检查NameNode就绪状态的初始化容器命令
     * 
     * @param workPath    HDFS安装路径
     * @param serviceName 服务名称
     * @return 初始化容器执行的命令
     */
    private String createNameNodeReadinessCheck(String workPath, String serviceName) {
        StringBuilder command = new StringBuilder();
        command.append("echo \"正在检查NameNode就绪状态...\";\n");

        // 从配置文件中获取NameNode地址
        command.append("# 从hdfs-site.xml获取NameNode地址\n");
        command.append("NN_ENDPOINTS=$(grep -oP 'dfs\\.namenode\\.rpc-address.*=\\K.*' ")
                .append(workPath).append("/etc/hadoop/hdfs-site.xml);\n");

        // 检查是否能获取到NameNode地址
        command.append("if [ -z \"$NN_ENDPOINTS\" ]; then\n")
                .append("  echo \"警告: 无法从hdfs-site.xml获取NameNode地址，将使用默认检测方式\";\n")
                .append("  # 尝试从core-site.xml获取fs.defaultFS\n")
                .append("  DEFAULT_FS=$(grep -oP 'fs\\.defaultFS=\\K.*' ").append(workPath)
                .append("/etc/hadoop/core-site.xml | sed 's|hdfs://\\(.*\\)|\\1|');\n")
                .append("  if [ -n \"$DEFAULT_FS\" ]; then\n")
                .append("    NN_ENDPOINTS=$DEFAULT_FS;\n")
                .append("    echo \"从core-site.xml获取到NameNode地址: $NN_ENDPOINTS\";\n")
                .append("  else\n")
                .append("    echo \"警告: 无法获取NameNode地址，将使用节点选择器中指定的主机\";\n")
                .append("    # 使用当前主机作为NameNode\n")
                .append("    NN_ENDPOINTS=\"$(hostname):8020\";\n")
                .append("  fi\n")
                .append("fi;\n\n");

        command.append("echo \"检测到的NameNode端点: $NN_ENDPOINTS\";\n\n");

        // 尝试连接每个NameNode端点
        command.append("# 尝试连接每个NameNode端点\n")
                .append("RETRIES=0;\n")
                .append("MAX_RETRIES=90;\n")
                .append("SUCCESS=0;\n\n");

        command.append("while [ $RETRIES -lt $MAX_RETRIES ] && [ $SUCCESS -eq 0 ]; do\n")
                .append("  # 检查配置中指定的端点\n")
                .append("  for ENDPOINT in $NN_ENDPOINTS; do\n")
                .append("    NN_HOST=$(echo $ENDPOINT | cut -d':' -f1);\n")
                .append("    NN_PORT=$(echo $ENDPOINT | cut -d':' -f2 || echo \"8020\");\n")
                .append("    echo \"检查NameNode配置端点: $NN_HOST:$NN_PORT\";\n")
                .append("    \n")
                .append("    if nc -z $NN_HOST $NN_PORT 2>/dev/null; then\n")
                .append("      echo \"NameNode $NN_HOST:$NN_PORT 端口已开放\";\n")
                .append("      SUCCESS=1;\n")
                .append("      break;\n")
                .append("    fi;\n")
                .append("  done\n\n")

                .append("  # 如果上面的检查失败，尝试检查默认的8020端口\n")
                .append("  if [ $SUCCESS -eq 0 ]; then\n")
                .append("    for ENDPOINT in $NN_ENDPOINTS; do\n")
                .append("      NN_HOST=$(echo $ENDPOINT | cut -d':' -f1);\n")
                .append("      echo \"检查NameNode默认端口: $NN_HOST:8020\";\n")
                .append("      \n")
                .append("      if nc -z $NN_HOST 8020 2>/dev/null; then\n")
                .append("        echo \"NameNode $NN_HOST:8020 默认端口已开放\";\n")
                .append("        SUCCESS=1;\n")
                .append("        break;\n")
                .append("      fi;\n")
                .append("    done\n")
                .append("  fi\n\n")

                .append("  # 检查Web UI端口\n")
                .append("  if [ $SUCCESS -eq 0 ]; then\n")
                .append("    for ENDPOINT in $NN_ENDPOINTS; do\n")
                .append("      NN_HOST=$(echo $ENDPOINT | cut -d':' -f1);\n")
                .append("      echo \"检查NameNode Web UI端口: $NN_HOST:9870\";\n")
                .append("      \n")
                .append("      if nc -z $NN_HOST 9870 2>/dev/null; then\n")
                .append("        echo \"NameNode $NN_HOST:9870 Web UI端口已开放\";\n")
                .append("        SUCCESS=1;\n")
                .append("        break;\n")
                .append("      fi;\n")
                .append("    done\n")
                .append("  fi\n\n")

                .append("  if [ $SUCCESS -eq 0 ]; then\n")
                .append("    echo \"未检测到就绪的NameNode, 等待重试... ($((RETRIES+1))/$MAX_RETRIES)\";\n")
                .append("    RETRIES=$((RETRIES+1));\n")
                .append("    sleep 5;\n")
                .append("  fi\n")
                .append("done\n\n")

                .append("if [ $SUCCESS -eq 1 ]; then\n")
                .append("  echo \"NameNode就绪检查完成\";\n")
                .append("  exit 0;\n")
                .append("else\n")
                .append("  echo \"错误: 在最大重试次数后未检测到就绪的NameNode\";\n")
                .append("  exit 1;\n")
                .append("fi");

        return command.toString();
    }
}
