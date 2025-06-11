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
                return String.join("\n",
                                "echo \"正在检查ZooKeeper集群就绪状态...\"",
                                "# 使用BusyBox兼容的命令提取ZooKeeper配置",
                                "ZOOKEEPER_SERVERS=$(grep \"ha.zookeeper.quorum\" " + workPath
                                                + "/etc/hadoop/core-site.xml | sed -n 's/.*<value>\\(.*\\)<\\/value>.*/\\1/p' | tr ',' ' ')",
                                "if [ -z \"$ZOOKEEPER_SERVERS\" ]; then",
                                "  echo \"错误: 无法从配置中获取ZooKeeper服务器列表\"",
                                "  exit 1",
                                "fi",
                                "echo \"检测到的ZooKeeper服务器: $ZOOKEEPER_SERVERS\"",
                                "for ZK_SERVER in $ZOOKEEPER_SERVERS; do",
                                "  ZK_HOST=$(echo $ZK_SERVER | cut -d':' -f1)",
                                "  ZK_PORT=$(echo $ZK_SERVER | cut -d':' -f2)",
                                "  echo \"正在检查ZooKeeper服务器: $ZK_HOST:$ZK_PORT\"",
                                "  RETRIES=0",
                                "  MAX_RETRIES=60",
                                "  while [ $RETRIES -lt $MAX_RETRIES ]; do",
                                "    if echo ruok | nc $ZK_HOST $ZK_PORT 2>/dev/null | grep -q imok; then",
                                "      echo \"ZooKeeper服务器 $ZK_HOST:$ZK_PORT 已就绪\"",
                                "      break",
                                "    else",
                                "      echo \"ZooKeeper服务器 $ZK_HOST:$ZK_PORT 未就绪, 等待重试... ($((RETRIES+1))/$MAX_RETRIES)\"",
                                "      RETRIES=$((RETRIES+1))",
                                "      sleep 5",
                                "    fi",
                                "  done",
                                "  if [ $RETRIES -eq $MAX_RETRIES ]; then",
                                "    echo \"错误: ZooKeeper服务器 $ZK_HOST:$ZK_PORT 在最大重试次数后仍未就绪\"",
                                "    exit 1",
                                "  fi",
                                "done",
                                "echo \"所有ZooKeeper服务器已就绪\"");
        }

        /**
         * 创建检查NameNode就绪状态的初始化容器命令
         * 
         * @param workPath    HDFS安装路径
         * @param serviceName 服务名称
         * @return 初始化容器执行的命令
         */
        private String createNameNodeReadinessCheck(String workPath, String serviceName) {
                return String.join("\n",
                                "echo \"正在检查NameNode就绪状态...\"",
                                "# 从hdfs-site.xml获取NameNode地址(使用BusyBox兼容方式)",
                                "NN_ENDPOINTS=$(grep \"dfs.namenode.rpc-address\" " + workPath
                                                + "/etc/hadoop/hdfs-site.xml | sed -n 's/.*<value>\\(.*\\)<\\/value>.*/\\1/p' | head -1)",
                                "if [ -z \"$NN_ENDPOINTS\" ]; then",
                                "  echo \"警告: 无法从hdfs-site.xml获取NameNode地址，将使用默认检测方式\"",
                                "  # 尝试从core-site.xml获取fs.defaultFS",
                                "  DEFAULT_FS=$(grep \"fs.defaultFS\" " + workPath
                                                + "/etc/hadoop/core-site.xml | sed -n 's/.*<value>\\(.*\\)<\\/value>.*/\\1/p')",
                                "  if [ -n \"$DEFAULT_FS\" ]; then",
                                "    # 提取hdfs://后面的主机:端口部分",
                                "    NN_ENDPOINTS=$(echo $DEFAULT_FS | sed 's|hdfs://\\(.*\\)|\\1|')",
                                "    echo \"从core-site.xml获取到NameNode地址: $NN_ENDPOINTS\"",
                                "  else",
                                "    echo \"警告: 无法获取NameNode地址，将使用节点选择器中指定的主机\"",
                                "    # 使用当前主机作为NameNode",
                                "    NN_ENDPOINTS=\"$(hostname):8020\"",
                                "  fi",
                                "fi",
                                "",
                                "echo \"检测到的NameNode端点: $NN_ENDPOINTS\"",
                                "",
                                "# 尝试连接每个NameNode端点",
                                "RETRIES=0",
                                "MAX_RETRIES=90",
                                "SUCCESS=0",
                                "",
                                "while [ $RETRIES -lt $MAX_RETRIES ] && [ $SUCCESS -eq 0 ]; do",
                                "  # 检查配置中指定的端点",
                                "  for ENDPOINT in $NN_ENDPOINTS; do",
                                "    NN_HOST=$(echo $ENDPOINT | cut -d':' -f1)",
                                "    NN_PORT=$(echo $ENDPOINT | cut -d':' -f2 || echo \"8020\")",
                                "    echo \"检查NameNode配置端点: $NN_HOST:$NN_PORT\"",
                                "    ",
                                "    if nc -z $NN_HOST $NN_PORT 2>/dev/null; then",
                                "      echo \"NameNode $NN_HOST:$NN_PORT 端口已开放\"",
                                "      SUCCESS=1",
                                "      break",
                                "    fi",
                                "  done",
                                "",
                                "  # 如果上面的检查失败，尝试检查默认的8020端口",
                                "  if [ $SUCCESS -eq 0 ]; then",
                                "    for ENDPOINT in $NN_ENDPOINTS; do",
                                "      NN_HOST=$(echo $ENDPOINT | cut -d':' -f1)",
                                "      echo \"检查NameNode默认端口: $NN_HOST:8020\"",
                                "      ",
                                "      if nc -z $NN_HOST 8020 2>/dev/null; then",
                                "        echo \"NameNode $NN_HOST:8020 默认端口已开放\"",
                                "        SUCCESS=1",
                                "        break",
                                "      fi",
                                "    done",
                                "  fi",
                                "",
                                "  # 检查Web UI端口",
                                "  if [ $SUCCESS -eq 0 ]; then",
                                "    for ENDPOINT in $NN_ENDPOINTS; do",
                                "      NN_HOST=$(echo $ENDPOINT | cut -d':' -f1)",
                                "      echo \"检查NameNode Web UI端口: $NN_HOST:9870\"",
                                "      ",
                                "      if nc -z $NN_HOST 9870 2>/dev/null; then",
                                "        echo \"NameNode $NN_HOST:9870 Web UI端口已开放\"",
                                "        SUCCESS=1",
                                "        break",
                                "      fi",
                                "    done",
                                "  fi",
                                "",
                                "  if [ $SUCCESS -eq 0 ]; then",
                                "    echo \"未检测到就绪的NameNode, 等待重试... ($((RETRIES+1))/$MAX_RETRIES)\"",
                                "    RETRIES=$((RETRIES+1))",
                                "    sleep 5",
                                "  fi",
                                "done",
                                "",
                                "if [ $SUCCESS -eq 1 ]; then",
                                "  echo \"NameNode就绪检查完成\"",
                                "  exit 0",
                                "else",
                                "  echo \"错误: 在最大重试次数后未检测到就绪的NameNode\"",
                                "  exit 1",
                                "fi");
        }
}
