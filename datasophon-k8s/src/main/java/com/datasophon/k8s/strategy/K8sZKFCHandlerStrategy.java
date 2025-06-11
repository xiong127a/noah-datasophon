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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
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
                                        // 获取namenode-core-site-xml ConfigMap
                                        ConfigMap coreSiteConfigMap = kubeClient.configMaps()
                                                        .inNamespace(namespace)
                                                        .withName("namenode-core-site-xml")
                                                        .get();

                                        if (coreSiteConfigMap != null && coreSiteConfigMap.getData() != null) {
                                                // 获取core-site.xml内容
                                                String coreSiteXml = coreSiteConfigMap.getData().get("core-site.xml");
                                                if (coreSiteXml != null && !coreSiteXml.isEmpty()) {
                                                        logger.info("成功从ConfigMap获取core-site.xml内容");

                                                        // 解析XML获取ZooKeeper地址
                                                        String zkQuorum = getPropertyFromXmlString(coreSiteXml,
                                                                        "ha.zookeeper.quorum");
                                                        if (zkQuorum != null && !zkQuorum.isEmpty()) {
                                                                envVars.put("ZOOKEEPER_SERVERS", zkQuorum);
                                                                logger.info("从ConfigMap中找到ZooKeeper地址: {}", zkQuorum);
                                                        } else {
                                                                logger.warn("在core-site.xml中未找到ZooKeeper地址");
                                                        }

                                                        // 尝试从core-site.xml获取默认FS
                                                        String defaultFs = getPropertyFromXmlString(coreSiteXml,
                                                                        "fs.defaultFS");
                                                        if (defaultFs != null && defaultFs.startsWith("hdfs://")) {
                                                                String nnAddress = defaultFs.substring(7);
                                                                // 如果是服务名而不是具体地址，需要进一步处理
                                                                if (!nnAddress.contains(":")) {
                                                                        logger.info("发现服务名形式的fs.defaultFS: {}",
                                                                                        nnAddress);
                                                                } else {
                                                                        envVars.put("NAMENODE_ADDRESSES", nnAddress);
                                                                        logger.info("从core-site.xml中的fs.defaultFS找到NameNode地址: {}",
                                                                                        nnAddress);
                                                                }
                                                        }
                                                } else {
                                                        logger.warn("ConfigMap中未找到core-site.xml内容");
                                                }
                                        } else {
                                                logger.warn("未找到namenode-core-site-xml ConfigMap");
                                        }

                                        // 获取namenode-hdfs-site-xml ConfigMap
                                        ConfigMap hdfsSiteConfigMap = kubeClient.configMaps()
                                                        .inNamespace(namespace)
                                                        .withName("namenode-hdfs-site-xml")
                                                        .get();

                                        if (hdfsSiteConfigMap != null && hdfsSiteConfigMap.getData() != null) {
                                                // 获取hdfs-site.xml内容
                                                String hdfsSiteXml = hdfsSiteConfigMap.getData().get("hdfs-site.xml");
                                                if (hdfsSiteXml != null && !hdfsSiteXml.isEmpty()) {
                                                        logger.info("成功从ConfigMap获取hdfs-site.xml内容");

                                                        // 解析XML获取NameNode地址
                                                        List<String> nnAddresses = getNameNodeAddressesFromXml(
                                                                        hdfsSiteXml);
                                                        if (!nnAddresses.isEmpty()) {
                                                                envVars.put("NAMENODE_ADDRESSES",
                                                                                String.join(",", nnAddresses));
                                                                logger.info("从hdfs-site.xml中找到NameNode地址: {}",
                                                                                String.join(",", nnAddresses));
                                                        } else {
                                                                logger.warn("在hdfs-site.xml中未找到NameNode地址配置");
                                                        }
                                                } else {
                                                        logger.warn("ConfigMap中未找到hdfs-site.xml内容");
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
                                                jobCmd,
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
         * 从XML字符串中获取指定属性的值
         * 
         * @param xmlContent   XML内容字符串
         * @param propertyName 属性名
         * @return 属性值，如果不存在则返回null
         */
        private String getPropertyFromXmlString(String xmlContent, String propertyName) {
                try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        // 禁用外部实体引用，防止XXE攻击
                        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
                        doc.getDocumentElement().normalize();

                        NodeList propertyList = doc.getElementsByTagName("property");
                        for (int i = 0; i < propertyList.getLength(); i++) {
                                Element property = (Element) propertyList.item(i);
                                NodeList nameNodes = property.getElementsByTagName("name");
                                if (nameNodes.getLength() > 0) {
                                        String name = nameNodes.item(0).getTextContent();
                                        if (propertyName.equals(name)) {
                                                NodeList valueNodes = property.getElementsByTagName("value");
                                                if (valueNodes.getLength() > 0) {
                                                        return valueNodes.item(0).getTextContent();
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        logger.error("解析XML字符串时出错: {}", e.getMessage(), e);
                }
                return null;
        }

        /**
         * 从hdfs-site.xml字符串中获取NameNode地址列表
         * 
         * @param xmlContent hdfs-site.xml内容字符串
         * @return NameNode地址列表
         */
        private List<String> getNameNodeAddressesFromXml(String xmlContent) {
                List<String> addresses = new ArrayList<>();
                try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        // 禁用外部实体引用，防止XXE攻击
                        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
                        doc.getDocumentElement().normalize();

                        NodeList propertyList = doc.getElementsByTagName("property");
                        for (int i = 0; i < propertyList.getLength(); i++) {
                                Element property = (Element) propertyList.item(i);
                                NodeList nameNodes = property.getElementsByTagName("name");
                                if (nameNodes.getLength() > 0) {
                                        String name = nameNodes.item(0).getTextContent();
                                        if (name.startsWith("dfs.namenode.rpc-address.")) {
                                                NodeList valueNodes = property.getElementsByTagName("value");
                                                if (valueNodes.getLength() > 0) {
                                                        addresses.add(valueNodes.item(0).getTextContent());
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        logger.error("解析hdfs-site.xml字符串时出错: {}", e.getMessage(), e);
                }
                return addresses;
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
                                "# 使用环境变量中的NameNode地址",
                                "if [ -n \"$NAMENODE_ADDRESSES\" ]; then",
                                "  echo \"使用环境变量中的NameNode地址: $NAMENODE_ADDRESSES\"",
                                "  NN_ENDPOINTS=$(echo $NAMENODE_ADDRESSES | tr ',' ' ')",
                                "else",
                                "  echo \"错误: 环境变量NAMENODE_ADDRESSES未设置\"",
                                "  exit 1",
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
