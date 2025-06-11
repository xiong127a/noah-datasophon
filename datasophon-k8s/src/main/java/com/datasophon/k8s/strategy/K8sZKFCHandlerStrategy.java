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

                                // 修改zkfc命令，添加namenode ID参数
                                String updatedCmd = workPath + "/bin/hdfs zkfc -formatZK";
                                // 使用初始化容器生成的配置文件
                                updatedCmd = "if [ -f /tmp/active_namenode_id ]; then\n" +
                                                "  echo \"使用初始化容器确定的NameNode ID\"\n" +
                                                "  . /tmp/active_namenode_id\n" +
                                                "  echo \"NAMENODE_ID=$NAMENODE_ID\"\n" +
                                                "  echo \"HADOOP_OPTS=$HADOOP_OPTS\"\n" +
                                                "  echo \"HDFS_NAMENODE_OPTS=$HDFS_NAMENODE_OPTS\"\n" +
                                                "elif [ -n \"$NAMENODE_ID\" ]; then\n" +
                                                "  echo \"使用环境变量中的NAMENODE_ID=$NAMENODE_ID\"\n" +
                                                "  export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"\n" +
                                                "  export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"\n" +
                                                "else\n" +
                                                "  echo \"未找到NameNode ID，使用默认值'nn1'\"\n" +
                                                "  export NAMENODE_ID=nn1\n" +
                                                "  export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=nn1\"\n" +
                                                "  export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=nn1\"\n" +
                                                "fi\n\n" +
                                                "echo \"准备执行ZKFC格式化，使用NAMENODE_ID=$NAMENODE_ID\"\n" +
                                                updatedCmd;
                                logger.info("更新命令添加namenode ID: {}", updatedCmd);

                                // 添加检查ZooKeeper和NameNode就绪状态的初始化容器
                                List<String> initContainers = new ArrayList<>();
                                List<String> initContainerNames = new ArrayList<>();

                                // 1. 检查ZooKeeper集群就绪状态 - 使用BusyBox镜像
                                initContainers.add(createZkReadinessCheck(workPath));
                                initContainerNames.add("zk-readiness-check");

                                // 2. 检查NameNode就绪状态 - 也使用BusyBox镜像
                                initContainers.add(createNameNodeReadinessCheck(workPath, command.getServiceName()));
                                initContainerNames.add("namenode-readiness-check");

                                K8sUtil.runJobWithInitContainersAndEnv(
                                                namespace,
                                                "zkfc-format",
                                                kubeClient,
                                                volumeMounts,
                                                DockerImageUtils.getString(command.getServiceName()),
                                                updatedCmd,
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
                return String.join("\n",
                                "echo \"正在检查NameNode就绪状态...\"",
                                "# 显示环境变量信息",
                                "echo \"NAMENODE_ID=$NAMENODE_ID\"",
                                "echo \"NAMENODE_IDS=$NAMENODE_IDS\"",
                                "echo \"NAMENODE_INFO=$NAMENODE_INFO\"",
                                "echo \"HADOOP_OPTS=$HADOOP_OPTS\"",
                                "",
                                "# 从环境变量中解析NameNode信息",
                                "if [ -n \"$NAMENODE_INFO\" ]; then",
                                "  echo \"使用环境变量中的NameNode信息: $NAMENODE_INFO\"",
                                "  # 提取所有NameNode ID和地址",
                                "  if [ -n \"$NAMENODE_IDS\" ]; then",
                                "    AVAILABLE_IDS=$NAMENODE_IDS",
                                "  else",
                                "    # 如果没有NAMENODE_IDS，则尝试从NAMENODE_INFO中提取",
                                "    # 假设格式是 {\"nn1\":\"host1:port\",\"nn2\":\"host2:port\"}",
                                "    AVAILABLE_IDS=$(echo $NAMENODE_INFO | sed 's/{//g' | sed 's/}//g' | awk -F: '{print $1}' | sed 's/\"//g')",
                                "  fi",
                                "  echo \"可用的NameNode IDs: $AVAILABLE_IDS\"",
                                "  ",
                                "  # 使用逗号分隔的地址列表",
                                "  if [ -n \"$NAMENODE_ADDRESSES\" ]; then",
                                "    NN_ENDPOINTS=$NAMENODE_ADDRESSES",
                                "  else",
                                "    # 如果没有NAMENODE_ADDRESSES，则尝试从NAMENODE_INFO中提取",
                                "    NN_ENDPOINTS=$(echo $NAMENODE_INFO | sed 's/{//g' | sed 's/}//g' | awk -F: '{print $2}' | sed 's/\"//g' | sed 's/,/ /g')",
                                "  fi",
                                "else",
                                "  # 使用环境变量中的NameNode地址",
                                "  if [ -n \"$NAMENODE_ADDRESSES\" ]; then",
                                "    echo \"使用环境变量中的NameNode地址: $NAMENODE_ADDRESSES\"",
                                "    NN_ENDPOINTS=$(echo $NAMENODE_ADDRESSES | tr ',' ' ')",
                                "  else",
                                "    echo \"错误: 环境变量NAMENODE_ADDRESSES和NAMENODE_INFO均未设置\"",
                                "    exit 1",
                                "  fi",
                                "fi",
                                "",
                                "echo \"检测到的NameNode端点: $NN_ENDPOINTS\"",
                                "",
                                "# 尝试连接每个NameNode端点",
                                "RETRIES=0",
                                "MAX_RETRIES=90",
                                "SUCCESS=0",
                                "ACTIVE_NAMENODE_ID=\"\"",
                                "",
                                "# 将NameNode ID列表放入变量，以便后面使用cut命令提取",
                                "IDS_LIST=$NAMENODE_IDS",
                                "",
                                "while [ $RETRIES -lt $MAX_RETRIES ] && [ $SUCCESS -eq 0 ]; do",
                                "  # 检查配置中指定的端点",
                                "  COUNT=0",
                                "  for ENDPOINT in $NN_ENDPOINTS; do",
                                "    # 去除多余的引号",
                                "    ENDPOINT=$(echo $ENDPOINT | sed 's/\"//g')",
                                "    NN_HOST=$(echo $ENDPOINT | cut -d':' -f1)",
                                "    NN_PORT=$(echo $ENDPOINT | cut -d':' -f2 || echo \"8020\")",
                                "    ",
                                "    echo \"检查NameNode端点: $NN_HOST:$NN_PORT\"",
                                "    ",
                                "    if nc -z $NN_HOST $NN_PORT 2>/dev/null; then",
                                "      echo \"NameNode $NN_HOST:$NN_PORT 端口已开放\"",
                                "      SUCCESS=1",
                                "      ",
                                "      # 找到对应的NameNode ID",
                                "      if [ -n \"$NAMENODE_IDS\" ]; then",
                                "        # 按位置获取ID（基于COUNT值）",
                                "        # 计算要提取的字段位置",
                                "        FIELD_POS=$((COUNT+1))",
                                "        # 使用cut命令从逗号分隔列表中提取指定位置的ID",
                                "        ACTIVE_NAMENODE_ID=$(echo $IDS_LIST | tr ',' ' ' | awk '{print $'$FIELD_POS'}')",
                                "        ",
                                "        # 如果未获取到ID，使用第一个ID或默认值",
                                "        if [ -z \"$ACTIVE_NAMENODE_ID\" ]; then",
                                "          ACTIVE_NAMENODE_ID=$(echo $IDS_LIST | cut -d',' -f1 || echo \"nn1\")",
                                "        fi",
                                "      else",
                                "        # 没有ID列表，使用默认ID",
                                "        ACTIVE_NAMENODE_ID=${NAMENODE_ID:-nn1}",
                                "      fi",
                                "      ",
                                "      echo \"找到活动的NameNode: ID=$ACTIVE_NAMENODE_ID, 地址=$NN_HOST:$NN_PORT\"",
                                "      break",
                                "    fi",
                                "    COUNT=$((COUNT+1))",
                                "  done",
                                "",
                                "  # 如果没有找到可用的NameNode，检查Web UI端口",
                                "  if [ $SUCCESS -eq 0 ]; then",
                                "    COUNT=0",
                                "    for ENDPOINT in $NN_ENDPOINTS; do",
                                "      ENDPOINT=$(echo $ENDPOINT | sed 's/\"//g')",
                                "      NN_HOST=$(echo $ENDPOINT | cut -d':' -f1)",
                                "      echo \"检查NameNode Web UI端口: $NN_HOST:9870\"",
                                "      ",
                                "      if nc -z $NN_HOST 9870 2>/dev/null; then",
                                "        echo \"NameNode $NN_HOST:9870 Web UI端口已开放\"",
                                "        SUCCESS=1",
                                "        ",
                                "        # 找到对应的NameNode ID",
                                "        if [ -n \"$NAMENODE_IDS\" ]; then",
                                "          # 按位置获取ID（基于COUNT值）",
                                "          FIELD_POS=$((COUNT+1))",
                                "          ACTIVE_NAMENODE_ID=$(echo $IDS_LIST | tr ',' ' ' | awk '{print $'$FIELD_POS'}')",
                                "          ",
                                "          # 如果未获取到ID，使用第一个ID或默认值",
                                "          if [ -z \"$ACTIVE_NAMENODE_ID\" ]; then",
                                "            ACTIVE_NAMENODE_ID=$(echo $IDS_LIST | cut -d',' -f1 || echo \"nn1\")",
                                "          fi",
                                "        else",
                                "          ACTIVE_NAMENODE_ID=${NAMENODE_ID:-nn1}",
                                "        fi",
                                "        ",
                                "        echo \"找到活动的NameNode: ID=$ACTIVE_NAMENODE_ID, 地址=$NN_HOST:9870 (Web UI)\"",
                                "        break",
                                "      fi",
                                "      COUNT=$((COUNT+1))",
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
                                "  echo \"找到就绪的NameNode, ID=$ACTIVE_NAMENODE_ID\"",
                                "  # 设置环境变量给主容器使用",
                                "  echo \"export NAMENODE_ID=$ACTIVE_NAMENODE_ID\" > /tmp/active_namenode_id",
                                "  echo \"export HADOOP_OPTS=\\\"-Ddfs.ha.namenode.id=$ACTIVE_NAMENODE_ID\\\"\" >> /tmp/active_namenode_id",
                                "  echo \"export HDFS_NAMENODE_OPTS=\\\"-Ddfs.ha.namenode.id=$ACTIVE_NAMENODE_ID\\\"\" >> /tmp/active_namenode_id",
                                "  chmod 755 /tmp/active_namenode_id",
                                "  echo \"NameNode就绪检查完成，将使用ID: $ACTIVE_NAMENODE_ID\"",
                                "  exit 0",
                                "else",
                                "  echo \"错误: 在最大重试次数后未检测到就绪的NameNode\"",
                                "  # 使用默认NameNode ID",
                                "  echo \"将使用默认NameNode ID: ${NAMENODE_ID:-nn1}\"",
                                "  echo \"export NAMENODE_ID=${NAMENODE_ID:-nn1}\" > /tmp/active_namenode_id",
                                "  echo \"export HADOOP_OPTS=\\\"-Ddfs.ha.namenode.id=${NAMENODE_ID:-nn1}\\\"\" >> /tmp/active_namenode_id",
                                "  echo \"export HDFS_NAMENODE_OPTS=\\\"-Ddfs.ha.namenode.id=${NAMENODE_ID:-nn1}\\\"\" >> /tmp/active_namenode_id",
                                "  chmod 755 /tmp/active_namenode_id",
                                "  exit 1",
                                "fi");
        }
}
