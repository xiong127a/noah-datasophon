package com.datasophon.k8s.actor.handler;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceConfigVolume;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.FileUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.K8sFreeMakerUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.datasophon.common.Constants.PROMETHEUS_CONFIG;

@Data
public class K8sYamlDeploymentHandler {

    private static final Cache<String, ServiceConfig> CONFIG_CACHE = new TimedCache<>(60000);
    private static Logger logger;
    private String serviceName;
    private String serviceRoleName;
    private String serviceRoleFullName;
    private Map<String, Object> data = MapUtil.newHashMap();
    private Map<String, Object> k8sConfigMap = MapUtil.newHashMap();

    public K8sYamlDeploymentHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    private static void volumeLog(Map<Generators, List<ServiceConfig>> configFileMap, String logFile, String hostname,
            String appHome, Set<ServiceConfigVolume> volumePathSet, String serviceName, RunAs runAs) {
        String logStr;
        Map<String, String> paramMap = configFileMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(t -> "${" + t.getName() + "}", t -> Convert.toStr(t.getValue()),
                        (existing, replacement) -> replacement));
        paramMap.put("${user}", "root");
        paramMap.put("${hostname}", "$(hostname)");
        String logFileName = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

        try {
            if (logFileName.startsWith(StrUtil.SLASH)) {
                logStr = logFileName;
            } else {
                logStr = appHome + Constants.SLASH + logFileName;
            }

            // if (!K8sMinaUtils.checkPathExists(hostname, logStr)) {
            // K8sMinaUtils.checkParentPath(hostname, logStr);
            // }

            List<String> needService = Arrays.asList("TRINO", "PRESTO", "NEBULAGRAPH");

            if (needService.contains(serviceName) || logFile.contains("${hostname}")) {
                // 挂载日志目录
                int lastSlashIndex = logStr.lastIndexOf('/');
                logStr = (lastSlashIndex != -1) ? logStr.substring(0, lastSlashIndex) : logStr;
            } else {
                // K8sMinaUtils.createFile(hostname, logStr);
            }

            // K8sMinaUtils.execCmdWithResult(hostname,
            // String.format("chown -R %s:%s %s", runAs.getUser(), runAs.getGroup(),
            // logStr));
            addConfigFile(volumePathSet, "logs", logStr);
        } catch (Exception e) {
            logger.error("An error occurred while checking or creating the file: {}", e.getMessage(), e);
        }
    }

    public static void addConfigFile(Set<ServiceConfigVolume> volumePathSet, String configFileName,
            String configFilePath) {
        // 创建新的 ServiceConfigVolume 对象
        ServiceConfigVolume fileConfig = new ServiceConfigVolume();
        configFileName = configFileName.replace('.', '-');
        fileConfig.setName(configFileName);

        // 设置文件路径
        fileConfig.setValue(configFilePath);

        // 将新的 ServiceConfigVolume 对象添加到 volumePathSet
        volumePathSet.add(fileConfig);
    }

    public static void addConfigPath(Set<ServiceConfigVolume> volumePathSet, int count, String configFilePath) {
        // 创建新的 ServiceConfigVolume 对象
        ServiceConfigVolume fileConfig = new ServiceConfigVolume();

        // 设置名字为 "config" + fileCount
        fileConfig.setName("path" + count);

        // 设置文件路径
        fileConfig.setValue(configFilePath);

        // 将新的 ServiceConfigVolume 对象添加到 volumePathSet
        volumePathSet.add(fileConfig);
    }

    public static String generateConfigFilePath(String outputDirectory, Generators generators, String appHome) {
        String configFilePath;
        if (outputDirectory.startsWith(Constants.SLASH)) {
            // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
            configFilePath = FileUtils.concatPath(outputDirectory, generators.getFilename());
        } else {
            configFilePath = FileUtils.concatPath(appHome, outputDirectory, generators.getFilename());
        }

        return configFilePath;
    }

    public void addConfigFile(Set<ServiceConfigVolume> volumePathSet, Generators generators, String configFilePath,
            boolean containsHost) {

        String configMapName = K8sFreeMakerUtils.generateConfigMapName(serviceRoleFullName, generators);
        // 创建新的 ServiceConfigVolume 对象
        ServiceConfigVolume fileConfig = new ServiceConfigVolume();
        configMapName = configMapName.replace('.', '-');
        fileConfig.setName(configMapName);

        String filename = generators.getFilename();
        if (containsHost) {
            filename += ".example";
            configFilePath += ".example";
        }

        // 设置文件路径
        fileConfig.setValue(configFilePath);
        fileConfig.setFileName(filename);

        // 将新的 ServiceConfigVolume 对象添加到 volumePathSet
        if (BooleanUtil.isFalse(StrUtil.startWith(filename, Constants.K8S_CONFIG_PREFIX))) {
            volumePathSet.add(fileConfig);
        }
    }

    public ExecResult configure(Map<Generators, List<ServiceConfig>> configFileMap, RunAs runAs,
            ServiceRoleRunner startRunner, ServiceRoleRunner statusRunner, Integer roleNodeCnt,
            String decompressPackageName, String logFile, String hostname, String serviceRoleName, String masterHost,
            boolean enableKerberos, boolean enableRangerPlugin, CommandType commandType) {

        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        try {
            Set<ServiceConfigVolume> volumePathSet = new HashSet<>();

            Set<ServiceConfigVolume> volumeConfigMapSet = new HashSet<>();

            volumeConfig(configFileMap, appHome, volumePathSet, serviceRoleName, volumeConfigMapSet);

            volumeLog(configFileMap, logFile, hostname, appHome, volumePathSet, serviceName, runAs);

            volumeHadoopConfig(volumeConfigMapSet);

            volumeEnableKerberosConfig(volumeConfigMapSet, appHome, serviceRoleName, enableKerberos);

            Map<String, Object> data = prepareTemplateMap(runAs, startRunner, statusRunner, roleNodeCnt, appHome,
                    volumePathSet, volumeConfigMapSet, configFileMap, masterHost, enableKerberos, enableRangerPlugin,
                    logFile, commandType);
            if ("hdfs-zkfc".equalsIgnoreCase(serviceRoleFullName)) {
                // ZKFC作为NameNode Pod的Sidecar容器部署
                logger.info("ZKFC作为NameNode Pod的Sidecar容器部署，不生成k8s yaml文件");
                return execResult;
            }
            Template template = generateTemplate();

            String yamlFilePath = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

            K8sFreeMakerUtils.writeToTemplateLocal(template, data, yamlFilePath);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} load k8s yaml template error!", serviceRoleName, e);
        }

        return execResult;
    }

    private void volumeEnableKerberosConfig(Set<ServiceConfigVolume> volumeConfigMapSet, String appHome,
            String serviceRoleName, boolean enableKerberos) {
        if (enableKerberos) {
            addConfigFile(volumeConfigMapSet, "keytab", "/etc/security/keytab/");
            addConfigFile(volumeConfigMapSet, "krd5conf", "/etc/krb5.conf");
        } else {
            if (serviceRoleName.equals("KafkaBroker") || serviceRoleName.equals("efak")) {
                Iterator<ServiceConfigVolume> iterator = volumeConfigMapSet.iterator();
                while (iterator.hasNext()) {
                    ServiceConfigVolume config = iterator.next();
                    String value = (String) config.getValue();
                    if (value.endsWith(".sh")) {
                        String fileName = value.substring(value.lastIndexOf('/') + 1);
                        if (!fileName.equals("kafka-server-start.sh")) {
                            iterator.remove(); // 从集合中删除
                        }
                    }
                }
            }
        }
    }

    private Template generateTemplate() throws IOException {
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // 使用方括号语法替代后，不再需要特别设置命名约定
        config.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] { new ClassTemplateLoader(
                K8sFreeMakerUtils.class,
                "/k8s" + Constants.SLASH + "templates" + Constants.SLASH + serviceName + Constants.SLASH + "k8s") }));
        return config.getTemplate(serviceRoleFullName + ".yaml.ftl");
    }

    private Map<String, Object> prepareTemplateMap(RunAs runAs, ServiceRoleRunner startRunner,
            ServiceRoleRunner statusRunner, Integer roleNodeCnt, String appHome, Set<ServiceConfigVolume> volumePathSet,
            Set<ServiceConfigVolume> volumeConfigMapSet, Map<Generators, List<ServiceConfig>> configFileMap,
            String masterHost, Boolean enableKerberos, Boolean enableRangerPlugin, String logFile,
            CommandType commandType) {
        Map<String, String> paramMap = configFileMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(t -> "${" + t.getName() + "}", t -> Convert.toStr(t.getValue()),
                        (existing, replacement) -> replacement));
        paramMap.put("${user}", runAs.getUser());
        paramMap.put("${hostname}", "$(hostname)");
        logFile = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

        String logFilePath = FileUtils.concatPath(appHome, logFile);

        // 在这里处理configFileMap中的配置值，添加单位
        for (List<ServiceConfig> configs : configFileMap.values()) {
            for (ServiceConfig config : configs) {
                Object value = config.getValue();
                String unit = config.getUnit();

                // 如果值是数字类型且有单位，将值和单位组合
                if (StrUtil.isNotBlank(unit)) {
                    config.setValue(value + unit);
                }
            }
        }
        if (MapUtil.isEmpty(data)) {
            data = new HashMap<>();
        }
        data.put("volumePathSet", new ArrayList<>(volumePathSet));
        data.put("volumeConfigMapSet", new ArrayList<>(volumeConfigMapSet));
        data.put("serviceRoleFullName", serviceRoleFullName);
        data.put("serviceName", serviceName);
        data.put("namespace", Constant.K8S_NAMESPACE);
        data.put("dockerImage", DockerImageUtils.getString(serviceName));
        data.put("dockerBusyboxImage", DockerImageUtils.getString("BUSYBOX"));
        data.put("enableKerberos", enableKerberos.toString());
        data.put("enableRangerPlugin", enableRangerPlugin.toString());
        data.put("appHome", appHome);
        data.put("masterHost", masterHost);
        data.put("runAsUser", runAs.getUser());
        data.put("runAsGroup", runAs.getGroup());
        if (CommandType.INSTALL_SERVICE.equals(commandType)) {
            // 设置首次安装的首台机器标识
            data.put("isInstall", true);
        }
        data.put("startCommand",
                startRunner != null
                        ? String.format("su - %s -c 'cd %s && sh %s %s && tail -F -f %s'", runAs.getUser(),
                                appHome,
                                startRunner.getProgram(), String.join(" ", startRunner.getArgs()), logFilePath)
                        : "tail -F -f " + logFilePath);
        data.put("statusCommand",
                statusRunner != null
                        ? String.format("su - %s -c 'cd %s && sh %s %s'", runAs.getUser(), appHome,
                                statusRunner.getProgram(), String.join(" ", statusRunner.getArgs()))
                        : "exit 0");

        data.put(Constant.ROLE_NODE_CNT, roleNodeCnt);

        if (CONFIG_CACHE.isEmpty()) {
            loadConfigToCache(configFileMap, serviceRoleName);
        }

        // 处理有点的参数
        if ("HDFS".equals(serviceName)) {
            populateDataWithConfig("dfs.namenode.name.dir", "nn_name_dir");
            populateDataWithConfig("dfs.namenode.shared.edits.dir", "nn_shared_edits_dir");
            populateDataWithConfig("dfs.namenode.checkpoint.dir", "snn_checkpoint_dir");
            populateDataWithConfig("dfs.datanode.data.dir", "dn_data_dir");
            populateDataWithConfig("dfs.journalnode.edits.dir", "jn_node_dir");
            populateDataWithConfig("ha.zookeeper.quorum", "zkQuorum");
            populateDataWithConfig("dfs.nameservices", "nameServiceId");
        }
        if ("KAFKA".equals(serviceName)) {
            populateDataWithConfig("log.dirs", "kafka_log_dirs");
            populateDataWithConfig("zookeeper.connect", "zookeeper_connect");
            populateDataWithConfig("cluster1.zk.list", "cluster1ZkList");
            populateDataWithConfig("JMX_PORT", "JMX_PORT");
        }
        if ("YARN".equals(serviceName)) {
            populateDataWithConfig("yarn.resourcemanager.zk-address", "yarn_resourcemanager_zk_address");
            populateDataWithConfig("yarn.resourcemanager.webapp.address", "rmWebAppAddress");
        }
        if ("ZOOKEEPER".equals(serviceName)) {
            populateDataWithConfig("dataDir", "zk_data_dir");
        }
        if ("GRAFANA".equals(serviceName)) {
            String url = "http://%s:%s/ddh/api/cluster/grafana/kerberos/";
            String hostname = NetUtil.getLocalHostName();
            int port = 0;
            data.put("apiUrl", String.format(url, "192.168.1.54", 8081));
        }
        if ("HIVE".equals(serviceName)) {
            populateDataWithConfig("hive.metastore.uris", "metastore_uris");
            // 提取数据库连接信息，以便创建Secret
            extractHiveDatabaseInfo();
        }

        data.putAll(k8sConfigMap);
        CONFIG_CACHE.clear();
        CacheUtils.put(serviceRoleFullName + "_" + Constant.ROLE_NODE_CNT, roleNodeCnt);
        return data;
    }

    private void volumeConfig(Map<Generators, List<ServiceConfig>> configFileMap, String appHome,
            Set<ServiceConfigVolume> volumePathSet, String serviceRoleName,
            Set<ServiceConfigVolume> volumeConfigMapSet) {
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            if (StrUtil.startWith(generators.getFilename(), Constants.K8S_CONFIG_PREFIX)) {
                continue;
            }
            boolean containsHost = entry.getValue().stream()
                    .anyMatch(serviceConfig -> serviceConfig.getValue().toString().contains("{{HOST}}") ||
                            serviceConfig.getValue().toString().contains("{{IP}}"));

            String configFilePath;
            String outputDirectory = generators.getOutputDirectory();

            if (BooleanUtil.isFalse(generators.isNeedMount())) {
                continue;
            }
            if (StrUtil.isNotBlank(outputDirectory)) {
                for (String outPutDir : outputDirectory.split(StrUtil.COMMA)) {

                    configFilePath = generateConfigFilePath(outPutDir, generators, appHome);

                    addConfigFile(volumeConfigMapSet, generators, configFilePath, containsHost);
                }
            } else {
                configFilePath = FileUtils.concatPath(appHome, generators.getFilename());
                addConfigFile(volumeConfigMapSet, generators, configFilePath, containsHost);
            }

            if (generators.getOutputDirectory().startsWith("/var/kerberos/krb5kdc")) {
                continue;
            }

            // path配置目录挂载
            for (ServiceConfig ServiceConfig : entry.getValue()) {
                if (volumePathSet.stream().anyMatch(item -> ServiceConfig.getValue().equals(item.getValue()))) {
                    continue;
                }
                if (Constants.PATH.equals(ServiceConfig.getConfigType())) {
                    addConfigPath(volumePathSet, pathCount++, ServiceConfig.getValue().toString());
                }
            }

        }

        if ("PROMETHEUS".equals(serviceName)) {
            // 创建新的 ServiceConfigVolume 对象
            ServiceConfigVolume fileConfig = new ServiceConfigVolume();

            // 设置名字为 "config" + fileCount
            fileConfig.setName(PROMETHEUS_CONFIG);

            // 设置文件路径
            fileConfig.setValue("/opt/datasophon/prometheus-2.17.2/configs");

            volumeConfigMapSet.add(fileConfig);
        }

        if ("RANGER".equals(serviceName)) {
            volumePathSet.clear();
            addConfigFile(volumePathSet, "rangerdir", "/opt/datasophon/ranger-2.1.0");
            addConfigFile(volumePathSet, "adminconf", "/etc/ranger/admin");
        }

        if ("Krb5Kdc".equals(serviceRoleName) || "KAdmin".equals(serviceRoleName)) {
            addConfigFile(volumePathSet, "kerberos-data", "/var/kerberos/krb5kdc");
            addConfigFile(volumePathSet, "keytab", "/etc/security/keytab/");
        }

        if ("OpenldapServer".equals(serviceRoleName)) {
            addConfigFile(volumePathSet, "openldap-data", "/var/lib/openldap/");
            addConfigFile(volumePathSet, "openldap-conf", "/etc/openldap/slapd.d");
        }
        // Kafka特殊处理ConfigMap挂载
        if ("KAFKA".equals(serviceName)) {
            // 查找server.properties的配置卷
            for (ServiceConfigVolume configVolume : volumeConfigMapSet) {
                String fileName = configVolume.getFileName();
                if ("server.properties".equals(fileName)) {
                    // 修改其挂载路径到一个临时文件，便于主容器处理
                    String originalPath = (String) configVolume.getValue();
                    String tempPath = "/tmp/kafka-config/server.properties";

                    // 添加路径映射到数据中，供模板使用
                    if (!data.containsKey("kafkaConfigTempDir")) {
                        data.put("kafkaConfigTempDir", tempPath);
                    }
                    if (!data.containsKey("kafkaConfigTargetDir")) {
                        data.put("kafkaConfigTargetDir", originalPath.substring(0, originalPath.lastIndexOf('/')));
                    }

                    // 修改挂载路径到临时文件
                    configVolume.setValue(tempPath);
                    break;
                }
            }

            // 根据角色过滤挂载的ConfigMap，确保角色特定的配置文件只挂载到对应角色
            Iterator<ServiceConfigVolume> iterator = volumeConfigMapSet.iterator();
            while (iterator.hasNext()) {
                ServiceConfigVolume configVolume = iterator.next();
                String mountPath = (String) configVolume.getValue();
                String fileName = configVolume.getFileName();

                // EFAK相关配置文件只挂载到EFAK服务
                if ("KafkaBroker".equals(serviceRoleName) &&
                        (mountPath.contains("/efak/conf/") ||
                                "system-config.properties".equals(fileName) ||
                                "system-config.properties.example".equals(fileName))) {
                    logger.info("移除KafkaBroker中的EFAK配置文件挂载: {}", mountPath);
                    iterator.remove();
                }

                // Kafka Broker相关配置文件只挂载到Kafka Broker服务
                if ("efak".equals(serviceRoleName) &&
                        (mountPath.contains("/config/server.properties") ||
                                "server.properties".equals(fileName))) {
                    logger.info("移除EFAK中的Kafka Broker配置文件挂载: {}", mountPath);
                    iterator.remove();
                }
            }
        }

        // redis数据目录
        if ("REDIS".equals(serviceName)) {
            addConfigFile(volumePathSet, "redis-cluster", appHome + "/cluster/");
        }

        if ("POSTGRESQL".equals(serviceName)) {
            addConfigFile(volumePathSet, "postgresql-data", appHome + "/data/");
        }

        if ("PostgresqlWorker".equals(serviceRoleName)) {
            volumePathSet.removeIf(config -> ((String) config.getValue()).contains("postgresql.conf"));
        }

        if ("ClickHouse".equals(serviceRoleName)) {
            addConfigFile(volumePathSet, "clickhouse-data", "/var/lib/clickhouse/");
        }

        if ("HUE".equals(serviceName)) {
            addConfigFile(volumePathSet, "hive-config", "/opt/datasophon/hive-3.1.0/conf");
        }
        // redisSentinel数据目录
        if ("RedisSentinelMaster".equals(serviceRoleName) || "RedisSentinelSlave".equals(serviceRoleName)) {
            addConfigFile(volumePathSet, "redis-sentinel-data", appHome + "/var/data/");
        }

        // 检查并移除volumeConfigMapSet中属性不完整的ServiceConfigVolume对象
        Iterator<ServiceConfigVolume> iterator = volumeConfigMapSet.iterator();
        while (iterator.hasNext()) {
            ServiceConfigVolume configVolume = iterator.next();
            if (configVolume.getName() == null || configVolume.getValue() == null || configVolume.getFileName() == null
                    ||
                    StrUtil.isBlank(configVolume.getName()) || StrUtil.isBlank((String) configVolume.getValue()) ||
                    StrUtil.isBlank(configVolume.getFileName())) {
                logger.warn("移除属性不完整的ConfigVolume: name={}, value={}, fileName={}",
                        configVolume.getName(), configVolume.getValue(), configVolume.getFileName());
                iterator.remove();
            }
        }

    }

    private void volumeHadoopConfig(Set<ServiceConfigVolume> volumeConfigMapSet) {
        List<String> needHadoopService = Arrays.asList("HIVE", "HBASE", "TRINO", "YARN", "SPARK3", "FLINK", "RANGER",
                "HUE", "ALLUXIO", "TEZ", "ZEPPELIN");
        if (needHadoopService.contains(serviceName)) {
            List<String> hadoopConf = Arrays.asList("/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml");

            for (String conf : hadoopConf) {
                // 检查是否已经存在相同的配置
                boolean exists = volumeConfigMapSet.stream()
                        .anyMatch(existingConfig -> existingConfig.getValue().equals(conf));

                // 仅当不存在相同配置时才添加
                if (!exists) {
                    String hadoopRole = "hdfs-namenode";
                    if (conf.contains("yarn") || conf.contains("mapred")) {
                        hadoopRole = "yarn-resourcemanager";
                    }

                    String fileName = conf.substring(conf.lastIndexOf('/') + 1);
                    String configName = hadoopRole + "-" + fileName.replace(".", "-");

                    ServiceConfigVolume fileConfig = new ServiceConfigVolume();
                    fileConfig.setName(configName);
                    fileConfig.setValue(conf);
                    fileConfig.setFileName(fileName);
                    volumeConfigMapSet.add(fileConfig);
                }
            }
        }
    }

    // 提取出一个通用方法，用于从配置中提取目录
    private void populateDataWithConfig(String configName,
            String targetDataKey) {
        ServiceConfig serviceConfig = CONFIG_CACHE.get(configName);
        if (ObjUtil.isNull(serviceConfig)) {
            return;
        }
        Object value = serviceConfig.getValue();
        if (ObjUtil.isNull(value)) {
            return;
        }
        data.put(targetDataKey, value);
    }

    /**
     * 加载配置到缓存并处理Kubernetes特定配置
     *
     * @param configFileMap   配置文件映射
     * @param serviceRoleName 服务角色名称
     */
    public void loadConfigToCache(Map<Generators, List<ServiceConfig>> configFileMap, String serviceRoleName) {
        // 转换角色名为小写并添加下划线作为前缀
        String rolePrefixPattern = serviceRoleName.toLowerCase() + "_";

        for (List<ServiceConfig> configList : configFileMap.values()) {
            for (ServiceConfig config : configList) {
                // 所有配置都加入通用缓存
                CONFIG_CACHE.put(config.getName(), config);

                // 处理Kubernetes配置，条件更具体
                if (StrUtil.startWith(config.getConfigGroup(), Constants.K8S_CONFIG_PREFIX)) {
                    if (StrUtil.startWith(config.getName(), rolePrefixPattern)) {
                        String keyWithoutPrefix = config.getName().substring(rolePrefixPattern.length());

                        // 特殊处理端口映射配置
                        if (StrUtil.endWith(keyWithoutPrefix, "_port_mappings")) {
                            // 使用Hutool的ObjectUtil.isEmpty方法判断值是否为空
                            if (ObjUtil.isEmpty(config.getValue())) {
                                logger.debug("跳过空的端口映射配置: {}", keyWithoutPrefix);
                                continue;
                            }
                            logger.debug("添加有效的端口映射配置: {} = {}", keyWithoutPrefix, config.getValue());
                        }

                        k8sConfigMap.put(keyWithoutPrefix, config.getValue());
                    }
                }
            }
        }
    }

    /**
     * 提取Hive数据库连接信息以创建Secret
     * 此方法从配置中提取数据库连接信息和HDFS路径配置，并创建Secret
     *
     */
    private void extractHiveDatabaseInfo() {
        logger.info("正在提取Hive数据库连接信息和HDFS路径配置用于创建Secret...");

        // 尝试从缓存中获取数据库连接配置
        ServiceConfig dbUrlConfig = CONFIG_CACHE.get("javax.jdo.option.ConnectionURL");
        ServiceConfig dbUserConfig = CONFIG_CACHE.get("javax.jdo.option.ConnectionUserName");
        ServiceConfig dbPassConfig = CONFIG_CACHE.get("javax.jdo.option.ConnectionPassword");
        ServiceConfig dbDriverConfig = CONFIG_CACHE.get("javax.jdo.option.ConnectionDriverName");

        // 获取HDFS路径配置
        ServiceConfig warehouseConfig = CONFIG_CACHE.get("hive.metastore.warehouse.dir");
        ServiceConfig scratchConfig = CONFIG_CACHE.get("hive.exec.scratch.dir");
        ServiceConfig tempStorageConfig = CONFIG_CACHE.get("hive.exec.temporary.table.storage");

        if (ObjUtil.isNull(dbUrlConfig) || ObjUtil.isNull(dbUrlConfig.getValue())) {
            logger.error("未找到数据库URL配置，无法创建数据库Secret，Hive元数据存储将无法正常工作");
            throw new RuntimeException("缺少必要的数据库配置信息");
        }

        String dbUrl = dbUrlConfig.getValue().toString();
        String dbUser = dbUserConfig != null && dbUserConfig.getValue() != null ? dbUserConfig.getValue().toString()
                : "";
        String dbPass = dbPassConfig != null && dbPassConfig.getValue() != null ? dbPassConfig.getValue().toString()
                : "";
        String dbDriver = dbDriverConfig != null && dbDriverConfig.getValue() != null
                ? dbDriverConfig.getValue().toString()
                : "";

        // 获取HDFS路径值，如果未配置则使用默认值
        String warehouseDir = warehouseConfig != null && warehouseConfig.getValue() != null
                ? warehouseConfig.getValue().toString()
                : "/user/hive/warehouse";
        String scratchDir = scratchConfig != null && scratchConfig.getValue() != null
                ? scratchConfig.getValue().toString()
                : "/tmp/hive/scratch";
        String tempStorage = tempStorageConfig != null && tempStorageConfig.getValue() != null
                ? tempStorageConfig.getValue().toString()
                : "";

        if (StrUtil.isBlank(dbUser) || StrUtil.isBlank(dbPass)) {
            logger.error("数据库用户名或密码为空，无法创建数据库Secret，Hive元数据存储将无法正常工作");
            throw new RuntimeException("数据库凭据不完整");
        }

        // 解析数据库类型、主机、端口和名称
        String dbType = "";
        String dbHost = "";
        String dbPort = "";
        String dbName = "";

        try {
            // 提取数据库类型 (如 mysql, postgresql)
            if (dbUrl.contains("jdbc:")) {
                dbType = dbUrl.substring(dbUrl.indexOf("jdbc:") + 5, dbUrl.indexOf("://"));
            }

            // 提取主机和端口
            if (dbUrl.contains("://")) {
                String hostPortPart = dbUrl.substring(dbUrl.indexOf("://") + 3);
                if (hostPortPart.contains(":")) {
                    dbHost = hostPortPart.substring(0, hostPortPart.indexOf(":"));
                    String portAndRest = hostPortPart.substring(hostPortPart.indexOf(":") + 1);
                    if (portAndRest.contains("/")) {
                        dbPort = portAndRest.substring(0, portAndRest.indexOf("/"));
                    }
                }
            }

            // 提取数据库名称
            if (dbUrl.contains("/")) {
                String nameAndParams = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
                if (nameAndParams.contains("?")) {
                    dbName = nameAndParams.substring(0, nameAndParams.indexOf("?"));
                } else {
                    dbName = nameAndParams;
                }
            }

            // 清理可能的分号
            dbName = dbName.replace(";", "");

            // 验证所有必要信息是否提取成功
            if (StrUtil.isBlank(dbHost) || StrUtil.isBlank(dbPort) ||
                    StrUtil.isBlank(dbName) || StrUtil.isBlank(dbType)) {
                logger.error("无法从JDBC URL解析完整的数据库连接信息");
                throw new RuntimeException("数据库连接URL格式无效");
            }

            // 创建Secret数据映射
            Map<String, String> secretData = new HashMap<>();
            // 数据库连接信息
            secretData.put("db-host", dbHost);
            secretData.put("db-port", dbPort);
            secretData.put("db-name", dbName);
            secretData.put("db-user", dbUser);
            secretData.put("db-password", dbPass);
            secretData.put("db-type", dbType);

            // HDFS路径配置
            secretData.put("warehouse-dir", warehouseDir);
            secretData.put("scratch-dir", scratchDir);
            secretData.put("temp-storage", tempStorage);

            // 缓存Secret，将在K8s集群中创建
            K8sFreeMakerUtils.cacheDatabaseSecret(serviceRoleFullName, secretData, "-db-secret");

            logger.info("成功提取数据库连接信息和HDFS路径配置并创建Secret");
            logger.info("数据库信息: 类型={}, 主机={}, 端口={}, 数据库名={}",
                    dbType, dbHost, dbPort, dbName);
            logger.info("HDFS路径: warehouse={}, scratch={}", warehouseDir, scratchDir);
        } catch (Exception e) {
            logger.error("解析数据库连接URL时出错", e);
            throw new RuntimeException("创建数据库Secret失败: " + e.getMessage(), e);
        }
    }
}