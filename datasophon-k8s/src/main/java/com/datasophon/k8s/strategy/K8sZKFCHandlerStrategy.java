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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K8sZKFCHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {
        private static final Logger logger = LoggerFactory.getLogger(K8sZKFCHandlerStrategy.class);
        private String nameNodeDataDir = null; // 存储读取的NameNode数据目录路径

        public K8sZKFCHandlerStrategy(String serviceName, String serviceRoleName) {
                super(serviceName, serviceRoleName);
        }

        @Override
        public ExecResult handler(K8sServiceRoleOperateCommand command) {
                String user = command.getRunAs().getUser();

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

                                // 从Kubernetes ConfigMap读取配置
                                Map<String, String> envVars = new HashMap<>();
                                String namespace = Constants.DATASOPHON; // 使用默认命名空间

                                try {
                                        // 从core-site.xml获取ZooKeeper地址和默认FS
                                        Map<String, String> coreSiteValues = K8sUtil.getConfigValuesFromConfigMap(
                                                        kubeClient,
                                                        namespace,
                                                        "namenode-core-site-xml",
                                                        "core-site.xml",
                                                        "ha.zookeeper.quorum", "fs.defaultFS");

                                        // 设置ZooKeeper地址
                                        if (coreSiteValues.containsKey("ha.zookeeper.quorum")) {
                                                envVars.put("ZOOKEEPER_SERVERS",
                                                                coreSiteValues.get("ha.zookeeper.quorum"));
                                        } else {
                                                logger.warn("未找到ZooKeeper地址配置");
                                        }

                                        // 处理默认FS
                                        if (coreSiteValues.containsKey("fs.defaultFS")) {
                                                String defaultFs = coreSiteValues.get("fs.defaultFS");
                                                if (defaultFs.startsWith("hdfs://")) {
                                                        String nnAddress = defaultFs.substring(7);
                                                        if (nnAddress.contains(":")) {
                                                                envVars.put("NAMENODE_ADDRESSES", nnAddress);
                                                        }
                                                }
                                        }

                                        // 从hdfs-site.xml获取NameNode相关配置
                                        ConfigMap hdfsSiteConfigMap = kubeClient.configMaps()
                                                        .inNamespace(namespace)
                                                        .withName("namenode-hdfs-site-xml")
                                                        .get();

                                        if (hdfsSiteConfigMap != null && hdfsSiteConfigMap.getData() != null) {
                                                // 获取hdfs-site.xml中所有配置项
                                                Map<String, String> allHdfsProperties = K8sUtil
                                                                .getAllPropertiesFromConfigMap(
                                                                                hdfsSiteConfigMap, "hdfs-site.xml");

                                                // 获取NameNode数据目录配置
                                                String nameNodeDir = allHdfsProperties.get("dfs.namenode.name.dir");
                                                if (nameNodeDir != null) {
                                                        this.nameNodeDataDir = nameNodeDir;
                                                        logger.info("找到NameNode数据目录: {}", nameNodeDir);
                                                        envVars.put("NAMENODE_DATA_DIR", nameNodeDir);
                                                }

                                                // 提取nameservice ID
                                                String nameserviceId = allHdfsProperties.get("dfs.ha.nameservices");
                                                if (nameserviceId != null) {
                                                        logger.info("找到NameService ID: {}", nameserviceId);
                                                        envVars.put("NAMESERVICE_ID", nameserviceId);

                                                        // 获取该nameservice下的所有namenode ID
                                                        String namenodes = allHdfsProperties
                                                                        .get("dfs.ha.namenodes." + nameserviceId);
                                                        if (namenodes != null && !namenodes.isEmpty()) {
                                                                logger.info("找到NameNode列表: {}", namenodes);

                                                                // 解析namenode ID列表
                                                                String[] namenodeIdArray = namenodes.split(",");

                                                                // 收集所有namenode的地址信息
                                                                Map<String, String> namenodeAddresses = new HashMap<>();
                                                                for (String namenodeId : namenodeIdArray) {
                                                                        namenodeId = namenodeId.trim();
                                                                        String addressKey = "dfs.namenode.rpc-address."
                                                                                        +
                                                                                        nameserviceId + "."
                                                                                        + namenodeId;
                                                                        String address = allHdfsProperties
                                                                                        .get(addressKey);

                                                                        if (address != null) {
                                                                                namenodeAddresses.put(namenodeId,
                                                                                                address);
                                                                                logger.info("NameNode ID {} 地址: {}",
                                                                                                namenodeId, address);
                                                                        }
                                                                }

                                                                // 构建一个JSON格式的字符串，包含所有namenode ID和地址的映射
                                                                StringBuilder namenodeInfoJson = new StringBuilder("{");
                                                                int count = 0;
                                                                for (Map.Entry<String, String> entry : namenodeAddresses
                                                                                .entrySet()) {
                                                                        if (count > 0)
                                                                                namenodeInfoJson.append(",");
                                                                        namenodeInfoJson.append("\"")
                                                                                        .append(entry.getKey())
                                                                                        .append("\":")
                                                                                        .append("\"")
                                                                                        .append(entry.getValue())
                                                                                        .append("\"");
                                                                        count++;
                                                                }
                                                                namenodeInfoJson.append("}");

                                                                // 设置所有NameNode地址信息（JSON格式）
                                                                envVars.put("NAMENODE_INFO",
                                                                                namenodeInfoJson.toString());

                                                                // 同时提供逗号分隔的地址列表（向后兼容）
                                                                envVars.put("NAMENODE_ADDRESSES",
                                                                                String.join(",", namenodeAddresses
                                                                                                .values()));

                                                                // 默认使用第一个NameNode ID
                                                                envVars.put("NAMENODE_ID", namenodeIdArray[0].trim());
                                                                logger.info("设置默认NAMENODE_ID={}",
                                                                                namenodeIdArray[0].trim());

                                                                // 同时设置所有可能的NameNode ID，让初始化容器决定使用哪个
                                                                envVars.put("NAMENODE_IDS", String.join(",",
                                                                                Arrays.asList(namenodeIdArray)));

                                                        } else {
                                                                envVars.put("NAMENODE_ID", "nn1");
                                                                logger.warn("未找到NameNode ID配置，使用默认值'nn1'");
                                                        }
                                                } else {
                                                        // 如果没有HA配置，尝试直接查找所有dfs.namenode.rpc-address开头的配置
                                                        Map<String, String> namenodeAddresses = new HashMap<>();
                                                        Map<String, String> namenodeIds = new HashMap<>();

                                                        for (Map.Entry<String, String> entry : allHdfsProperties
                                                                        .entrySet()) {
                                                                String key = entry.getKey();
                                                                String value = entry.getValue();

                                                                if (key.startsWith("dfs.namenode.rpc-address.")) {
                                                                        // 从键名中提取NameNode ID
                                                                        String[] parts = key.split("\\.");
                                                                        if (parts.length >= 5) {
                                                                                String ns = parts[3];
                                                                                String nnId = parts[4];

                                                                                namenodeAddresses.put(nnId, value);
                                                                                namenodeIds.put(value, nnId);

                                                                                logger.info("找到NameNode: {}({}), 地址: {}",
                                                                                                nnId, ns, value);
                                                                        }
                                                                }
                                                        }

                                                        if (!namenodeAddresses.isEmpty()) {
                                                                // 构建JSON格式的NameNode信息
                                                                StringBuilder namenodeInfoJson = new StringBuilder("{");
                                                                int count = 0;
                                                                for (Map.Entry<String, String> entry : namenodeAddresses
                                                                                .entrySet()) {
                                                                        if (count > 0)
                                                                                namenodeInfoJson.append(",");
                                                                        namenodeInfoJson.append("\"")
                                                                                        .append(entry.getKey())
                                                                                        .append("\":")
                                                                                        .append("\"")
                                                                                        .append(entry.getValue())
                                                                                        .append("\"");
                                                                        count++;
                                                                }
                                                                namenodeInfoJson.append("}");

                                                                envVars.put("NAMENODE_INFO",
                                                                                namenodeInfoJson.toString());
                                                                envVars.put("NAMENODE_ADDRESSES",
                                                                                String.join(",", namenodeAddresses
                                                                                                .values()));

                                                                // 设置所有可能的NameNode ID
                                                                envVars.put("NAMENODE_IDS",
                                                                                String.join(",", namenodeAddresses
                                                                                                .keySet()));

                                                                // 默认使用第一个ID
                                                                String firstId = namenodeAddresses.keySet().iterator()
                                                                                .next();
                                                                envVars.put("NAMENODE_ID", firstId);
                                                                logger.info("设置默认NAMENODE_ID={}", firstId);
                                                        } else {
                                                                // 如果没有HA配置，使用默认值
                                                                envVars.put("NAMENODE_ID", "nn1");
                                                                logger.warn("未找到NameNode地址配置，使用默认NAMENODE_ID='nn1'");
                                                        }
                                                }
                                        } else {
                                                logger.warn("未找到namenode-hdfs-site-xml ConfigMap");
                                        }
                                } catch (Exception e) {
                                        logger.error("解析ConfigMap时出错: {}", e.getMessage(), e);
                                        // 继续执行，让容器内脚本尝试自行处理
                                }

                                // 输出获取到的环境变量
                                if (!envVars.isEmpty()) {
                                        logger.info("获取到的环境变量:");
                                        for (Map.Entry<String, String> entry : envVars.entrySet()) {
                                                logger.info("  {} = {}", entry.getKey(), entry.getValue());
                                        }
                                } else {
                                        logger.warn("未从ConfigMap中获取到任何环境变量");
                                }

                                // 找到zkfc format 命令，修改其以确保正确传递NameNode ID
                                String zkfcFormatCommand = "echo \"准备执行ZKFC格式化...\"\n" +
                                                "# ZKFC现在可以从数据目录自动识别NameNode ID，不再需要手动设置\n" +
                                                "echo \"使用数据目录: $NAMENODE_DATA_DIR\"\n" +
                                                "su - " + user + " -c \"cd " + workPath
                                                + " && bin/hdfs zkfc -formatZK\"";

                                // 添加检查ZooKeeper和NameNode就绪状态的初始化容器
                                List<String> initContainers = new ArrayList<>();
                                List<String> initContainerNames = new ArrayList<>();

                                // 1. 检查ZooKeeper集群就绪状态 - 使用BusyBox镜像
                                initContainers.add(createZkReadinessCheck(workPath));
                                initContainerNames.add("zk-readiness-check");

                                // 2. 检查NameNode就绪状态 - 也使用BusyBox镜像
                                initContainers.add(createNameNodeReadinessCheck(workPath, command.getServiceName()));
                                initContainerNames.add("namenode-readiness-check");

                                // 添加PVC支持，用于zkfc-format job访问namenode数据目录
                                List<VolumeMountDTO> allMounts = new ArrayList<>(Arrays.asList(volumeMounts));

                                // 添加对NameNode数据目录的访问 - 使用与NameNode相同的PVC
                                VolumeMountDTO nameNodeDataMount = new VolumeMountDTO(
                                                "namenode-data", // 与NameNode StatefulSet使用相同的Volume名称
                                                "hdfs-namenode-data", // 使用与NameNode StatefulSet相同的PVC基础名称
                                                this.nameNodeDataDir // 从ConfigMap获取的数据目录
                                );
                                allMounts.add(nameNodeDataMount);

                                // 更新nameNodeDataDir环境变量，传递给容器
                                envVars.put("NAMENODE_DATA_DIR", this.nameNodeDataDir);

                                // 更新初始化容器中的路径引用
                                String updatedInitContainer = initContainers.get(1).replace(
                                                "/mnt/hdfs-namenode-data",
                                                this.nameNodeDataDir);
                                initContainers.set(1, updatedInitContainer);

                                K8sUtil.runJobWithInitContainersAndEnv(
                                                namespace,
                                                "zkfc-format",
                                                kubeClient,
                                                allMounts.toArray(new VolumeMountDTO[0]),
                                                DockerImageUtils.getString(command.getServiceName()),
                                                zkfcFormatCommand,
                                                command.getHostname(),
                                                initContainers,
                                                initContainerNames,
                                                DockerImageUtils.getString("BUSYBOX"),
                                                envVars);
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
                                "# 显示环境变量信息",
                                "echo \"ZOOKEEPER_SERVERS=$ZOOKEEPER_SERVERS\"",
                                "",
                                "# 使用环境变量中的ZooKeeper服务器列表",
                                "if [ -n \"$ZOOKEEPER_SERVERS\" ]; then",
                                "  echo \"使用环境变量中的ZooKeeper服务器列表: $ZOOKEEPER_SERVERS\"",
                                "  ZK_SERVERS=$ZOOKEEPER_SERVERS",
                                "else",
                                "  echo \"错误: 环境变量ZOOKEEPER_SERVERS未设置\"",
                                "  exit 1",
                                "fi",
                                "",
                                "echo \"检测到的ZooKeeper服务器: $ZK_SERVERS\"",
                                "# 将逗号分隔的列表转换为空格分隔",
                                "ZK_SERVERS=$(echo $ZK_SERVERS | tr ',' ' ')",
                                "",
                                "for ZK_SERVER in $ZK_SERVERS; do",
                                "  ZK_HOST=$(echo $ZK_SERVER | cut -d':' -f1)",
                                "  ZK_PORT=$(echo $ZK_SERVER | cut -d':' -f2 || echo \"2181\")",
                                "  echo \"正在检查ZooKeeper服务器: $ZK_HOST:$ZK_PORT\"",
                                "  RETRIES=0",
                                "  MAX_RETRIES=60",
                                "  while [ $RETRIES -lt $MAX_RETRIES ]; do",
                                "    if nc -z $ZK_HOST $ZK_PORT; then",
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
                // 直接使用从ConfigMap获取的实际目录路径
                String namenodeDir = this.nameNodeDataDir;

                return String.join("\n",
                                "echo \"正在检查NameNode就绪状态...\"",
                                "# 显示环境变量信息",
                                "echo \"NAMENODE_DATA_DIR=$NAMENODE_DATA_DIR\"",
                                "",
                                "# 使用ConfigMap获取的实际数据目录路径",
                                "NAMENODE_DIR=\"$NAMENODE_DATA_DIR\"",
                                "echo \"使用NameNode数据目录: $NAMENODE_DIR\"",
                                "",
                                "# 检查NameNode服务是否可用",
                                "RETRIES=0",
                                "MAX_RETRIES=90",
                                "SUCCESS=0",
                                "",
                                "while [ $RETRIES -lt $MAX_RETRIES ] && [ $SUCCESS -eq 0 ]; do",
                                "  # 查找可用的NameNode服务",
                                "  if nc -z hdfs-namenode-0.hdfs-namenode.${namespace}.svc.cluster.local 8020 ||",
                                "     nc -z hdfs-namenode-1.hdfs-namenode.${namespace}.svc.cluster.local 8020; then",
                                "    echo \"NameNode RPC端口已开放\"",
                                "    SUCCESS=1",
                                "    ",
                                "    # 检查数据目录是否存在",
                                "    if [ -d \"${NAMENODE_DIR}\" ]; then",
                                "      echo \"NameNode数据目录存在: ${NAMENODE_DIR}\"",
                                "      if [ -f \"${NAMENODE_DIR}/current/VERSION\" ]; then",
                                "        echo \"找到VERSION文件，内容如下:\"",
                                "        cat ${NAMENODE_DIR}/current/VERSION",
                                "      else",
                                "        echo \"警告: VERSION文件不存在，但不做处理，让ZKFC自行判断\"",
                                "      fi",
                                "    else",
                                "      echo \"警告: NameNode数据目录不存在: ${NAMENODE_DIR}\"",
                                "      # 不再创建目录和VERSION文件，让ZKFC自动处理",
                                "    fi",
                                "  fi",
                                "  ",
                                "  if [ $SUCCESS -eq 0 ]; then",
                                "    echo \"NameNode服务未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)\"",
                                "    RETRIES=$((RETRIES+1))",
                                "    sleep 5",
                                "  fi",
                                "done",
                                "",
                                "if [ $SUCCESS -eq 1 ]; then",
                                "  echo \"NameNode就绪检查完成\"",
                                "  exit 0",
                                "else",
                                "  echo \"警告: NameNode在最大重试次数后仍未就绪，继续执行但可能出现问题\"",
                                "  exit 0  # 仍然返回成功，让主容器自行处理",
                                "fi");
        }
}
