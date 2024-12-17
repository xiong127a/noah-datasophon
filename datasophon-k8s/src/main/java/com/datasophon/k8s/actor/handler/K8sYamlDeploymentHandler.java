package com.datasophon.k8s.actor.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.K8sFreemakerUtils;
import com.datasophon.k8s.util.K8sMinaUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class K8sYamlDeploymentHandler {

    private static Logger logger;
    private String serviceName;
    private String serviceRoleName;
    private String serviceRoleFullName;


    public K8sYamlDeploymentHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    private static void volumeLog(
            Map<Generators, List<ServiceConfig>> configFileMap, String logFile, String hostname, String appHome, Set<ServiceConfig> volumePathSet, String serviceName, RunAs runAs) {
        String logStr;
        Map<String, String> paramMap = configFileMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        t -> "${" + t.getName() + "}",
                        t -> Convert.toStr(t.getValue()),
                        (existing, replacement) -> replacement
                ));
        paramMap.put("${user}", "root");
        paramMap.put("${host}", hostname);
        String logFileName = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

        try {
            if (logFileName.startsWith(StrUtil.SLASH)) {
                logStr = logFileName;
            } else {
                logStr = appHome + Constants.SLASH + logFileName;
            }

            if (!K8sMinaUtils.checkPathExists(hostname, logStr)) {
                K8sMinaUtils.checkParentPath(hostname, logStr);
            }

            List<String> needService = Arrays.asList("TRINO", "PRESTO", "NEBULAGRAPH");

            if (needService.contains(serviceName) || logFile.contains("${host}")) {
                //挂载日志目录
                int lastSlashIndex = logStr.lastIndexOf('/');
                logStr = (lastSlashIndex != -1) ? logStr.substring(0, lastSlashIndex) : logStr;
            } else {
                K8sMinaUtils.createFile(hostname, logStr);
            }

            K8sMinaUtils.execCmdWithResult(hostname, String.format("chown -R %s:%s %s", runAs.getUser(), runAs.getGroup(), logStr));
            addConfigFile(volumePathSet, "logs", logStr);
        } catch (Exception e) {
            logger.error("An error occurred while checking or creating the file: {}", e.getMessage(), e);
        }
    }

    public static void addConfigFile(Set<ServiceConfig> volumePathSet, int count, String configFilePath) {
        // 创建新的 ServiceConfig 对象
        ServiceConfig fileConfig = new ServiceConfig();

        // 设置名字为 "config" + fileCount
        fileConfig.setName("config" + count);

        // 设置文件路径
        fileConfig.setValue(configFilePath);

        // 将新的 ServiceConfig 对象添加到 volumePathSet
        volumePathSet.add(fileConfig);
    }

    public static void addConfigFile(Set<ServiceConfig> volumePathSet, String configFileName, String configFilePath) {
        // 创建新的 ServiceConfig 对象
        ServiceConfig fileConfig = new ServiceConfig();

        // 设置名字为 "config" + fileCount
        fileConfig.setName(configFileName);

        // 设置文件路径
        fileConfig.setValue(configFilePath);

        // 将新的 ServiceConfig 对象添加到 volumePathSet
        volumePathSet.add(fileConfig);
    }

    public static void addConfigPath(Set<ServiceConfig> volumePathSet, int count, String configFilePath) {
        // 创建新的 ServiceConfig 对象
        ServiceConfig fileConfig = new ServiceConfig();

        // 设置名字为 "config" + fileCount
        fileConfig.setName("path" + count);

        // 设置文件路径
        fileConfig.setValue(configFilePath);

        // 将新的 ServiceConfig 对象添加到 volumePathSet
        volumePathSet.add(fileConfig);
    }

    public static String generateConfigFilePath(String outputDirectory, Generators generators, String appHome) {
        String configFilePath;
        if (outputDirectory.startsWith(Constants.SLASH)) {
            // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
            configFilePath = String.join(Constants.SLASH, outputDirectory, generators.getFilename());
        } else {
            configFilePath = String.join(Constants.SLASH, appHome, outputDirectory, generators.getFilename());
        }

        return configFilePath;
    }

    public ExecResult configure(Map<Generators, List<ServiceConfig>> configFileMap,
                                RunAs runAs,
                                ServiceRoleRunner startRunner,
                                ServiceRoleRunner statusRunner,
                                Integer roleNodeCnt,
                                String decompressPackageName,
                                String logFile,
                                String hostname,
                                String serviceRoleName,
                                String masterHost,
                                boolean enableKerberos,
                                boolean enableRangerPlugin) {

        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        try {
            Set<ServiceConfig> volumePathSet = new HashSet<>();

            volumeConfig(configFileMap, appHome, volumePathSet, serviceRoleName);

            volumeLog(configFileMap, logFile, hostname, appHome, volumePathSet, serviceName, runAs);

            volumeHadoopConfig(volumePathSet, hostname);

            volumeEnableKerberosConfig(volumePathSet, appHome, serviceRoleName, enableKerberos);

            Map<String, Object> data = prepareTemplateMap(runAs, startRunner, statusRunner, roleNodeCnt, appHome, volumePathSet, configFileMap, masterHost, enableKerberos, enableRangerPlugin);

            Template template = generateTemplate();

            String yamlFilePath = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

            K8sFreemakerUtils.writeToTemplateLocal(template, data, yamlFilePath);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} load k8s yaml template error!", serviceRoleName, e);
        }

        return execResult;
    }

    private void volumeEnableKerberosConfig(Set<ServiceConfig> volumePathSet, String appHome, String serviceRoleName, boolean enableKerberos) {
        if (enableKerberos) {
            addConfigFile(volumePathSet, "keytab", "/etc/security/keytab/");
            addConfigFile(volumePathSet, "krd5conf", "/etc/krb5.conf");
        } else {
            if (serviceRoleName.equals("KafkaBroker") || serviceRoleName.equals("efak")) {
                Iterator<ServiceConfig> iterator = volumePathSet.iterator();
                while (iterator.hasNext()) {
                    ServiceConfig config = iterator.next();
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
        config.setTemplateLoader(new MultiTemplateLoader(
                new TemplateLoader[]{
                        new ClassTemplateLoader(K8sFreemakerUtils.class,
                                "/k8s" + Constants.SLASH + "templates" + Constants.SLASH + serviceName + Constants.SLASH + "k8s")
                }
        ));
        return config.getTemplate(serviceRoleFullName + ".yaml.ftl");
    }

    private Map<String, Object> prepareTemplateMap(RunAs runAs,
                                                   ServiceRoleRunner startRunner,
                                                   ServiceRoleRunner statusRunner,
                                                   Integer roleNodeCnt,
                                                   String appHome,
                                                   Set<ServiceConfig> volumePathSet,
                                                   Map<Generators, List<ServiceConfig>> configFileMap,
                                                   String masterHost,
                                                   Boolean enableKerberos,
                                                   Boolean enableRangerPlugin) {
        Map<String, Object> data = new HashMap<>();
        data.put("itemList", new ArrayList<>(volumePathSet));
        data.put("serviceRoleFullName", serviceRoleFullName);
        data.put("serviceName", serviceName);
        data.put("namespace", Constant.K8S_NAMESPACE);
        data.put("dockerImage", DockerImageUtils.getString(serviceName));
        data.put("enableKerberos", enableKerberos.toString());
        data.put("enableRangerPlugin", enableRangerPlugin.toString());
        data.put("appHome", appHome);
        data.put("masterHost", masterHost);
        data.put("runAs", runAs.getUser());
        data.put("startCommand", startRunner != null ? String.format("su - %s -c 'cd %s && sh %s %s && tail -f /dev/null'",
                runAs.getUser(), appHome, startRunner.getProgram(), String.join(" ", startRunner.getArgs())) : "tail -f /dev/null");
        data.put("statusCommand", statusRunner != null ? String.format("su - %s -c 'cd %s && sh %s %s'",
                runAs.getUser(), appHome, statusRunner.getProgram(), String.join(" ", statusRunner.getArgs())) : "exit 0");

        data.put(Constant.ROLE_NODE_CNT, roleNodeCnt);
        // 获取 journalnodeDir 和 namenodeDir
        String journalnodeDir = getConfigDirectory(configFileMap, "dfs.namenode.name.dir");
        if (Objects.nonNull(journalnodeDir)) {
            data.put("namenodeDir", journalnodeDir);
        }

        String namenodeDir = getConfigDirectory(configFileMap, "dfs.journalnode.edits.dir");
        if (Objects.nonNull(namenodeDir)) {
            data.put("journalnodeDir", namenodeDir);
        }
        CacheUtils.put(serviceRoleFullName + "_" + Constant.ROLE_NODE_CNT, roleNodeCnt);
        return data;
    }

    private void volumeConfig(Map<Generators, List<ServiceConfig>> configFileMap, String appHome, Set<ServiceConfig> volumePathSet, String serviceRoleName) {
        int fileCount = 1;
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            String configFilePath;
            String outputDirectory = generators.getOutputDirectory();
            if (BooleanUtil.isFalse(generators.isNeedMount())) {
                continue;
            }
            if (StrUtil.isNotBlank(outputDirectory)) {
                for (String outPutDir : outputDirectory.split(StrUtil.COMMA)) {
                    configFilePath = generateConfigFilePath(outPutDir, generators, appHome);
                    addConfigFile(volumePathSet, fileCount++, configFilePath);
                }
            } else {
                configFilePath = String.join(Constants.SLASH, appHome, generators.getFilename());
                addConfigFile(volumePathSet, fileCount++, configFilePath);
            }

            if (generators.getOutputDirectory().startsWith("/var/kerberos/krb5kdc")) {
                continue;
            }

            // path配置目录挂载
            for (ServiceConfig serviceConfig : entry.getValue()) {
                if (volumePathSet.stream().anyMatch(item -> serviceConfig.getValue().equals(item.getValue()))) {
                    continue;
                }
                if (Constants.PATH.equals(serviceConfig.getConfigType())) {
                    addConfigPath(volumePathSet, pathCount++, serviceConfig.getValue().toString());
                }
            }

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
        //redis数据目录
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
        //redisSentinel数据目录
        if ("RedisSentinelMaster".equals(serviceRoleName) || "RedisSentinelSlave".equals(serviceRoleName)) {
            addConfigFile(volumePathSet, "redis-sentinel-data", appHome + "/var/data/");
        }

    }

    private void volumeHadoopConfig(Set<ServiceConfig> volumePathSet, String hostname) {
        List<String> needHadoopService = Arrays.asList("HIVE", "HBASE", "TRINO", "YARN", "SPARK3", "FLINK", "RANGER", "HUE", "ALLUXIO", "TEZ", "ZEPPELIN");
        if (needHadoopService.contains(serviceName)) {
            List<String> hadoopConf = Arrays.asList(
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml"
            );
            int config = 1;
            for (String conf : hadoopConf) {
                // 检查是否已经存在相同的配置
                boolean exists = volumePathSet.stream()
                        .anyMatch(existingConfig -> existingConfig.getValue().equals(conf));

                // 仅当不存在相同配置时才添加
                if (!exists) {
                    addConfigFile(volumePathSet, "hadoopconfig" + config++, conf);
                }
            }
        }
    }


    // 提取出一个通用方法，用于从配置中提取目录
    private String getConfigDirectory(Map<Generators, List<ServiceConfig>> configFileMap, String key) {
        return configFileMap.values().stream()
                .flatMap(List::stream)
                .filter(t -> key.equals(t.getName()))
                .map(t -> Convert.toStr(t.getValue()))
                .findFirst()
                .orElse(null);
    }
}