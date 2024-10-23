package com.datasophon.k8s.actor.handler;

import cn.hutool.core.convert.Convert;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
public class K8sYamlDeploymentHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;


    public K8sYamlDeploymentHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    private static void volumeLog(
            Map<Generators, List<ServiceConfig>> configFileMap, String logFile, String hostname, String appHome, Set<ServiceConfig> volumePathSet, String serviceName,RunAs runAs) {
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

        if (logFileName.startsWith(StrUtil.SLASH)) {
            logStr = logFileName;
        } else {
            logStr = appHome + Constants.SLASH + logFileName;
        }
        List<String> needService = Arrays.asList("TRINO", "PRESTO");
        if (needService.contains(serviceName)) {
            log.info("start config trino logfile");
            int lastSlashIndex = logStr.lastIndexOf('/');
            logStr = (lastSlashIndex != -1) ? logStr.substring(0, lastSlashIndex) : logStr;
        }
        try {
            if(!K8sMinaUtils.checkPathExists(hostname,logStr)){
                K8sMinaUtils.checkParentPath(hostname,logStr);
                K8sMinaUtils.createFile(hostname,logStr);
                K8sMinaUtils.execCmdWithResult(hostname,String.format("chown -R %s:%s %s",runAs.getUser(),runAs.getGroup(),logStr));
            }
        } catch (Exception e) {
            log.error("An error occurred while checking or creating the file: {}", e.getMessage(), e);
        }
        ServiceConfig logConfig = new ServiceConfig();
        logConfig.setName("logs");
        logConfig.setValue(logStr);
        volumePathSet.add(logConfig);
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

            volumeLog(configFileMap, logFile, hostname, appHome, volumePathSet, serviceName,runAs);

            volumeHadoopConfig(volumePathSet);

            volumeEnableKerberosConfig(volumePathSet,appHome,serviceRoleName, enableKerberos);

            Map<String, Object> data = prepareTemplateMap(runAs, startRunner, statusRunner, roleNodeCnt, appHome, volumePathSet, configFileMap,masterHost, enableKerberos, enableRangerPlugin);

            Template template = generateTemplate();

            String yamlFilePath = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

            K8sFreemakerUtils.writeToTemplateLocal(template, data, yamlFilePath);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} load k8s yaml template error!", serviceRoleName, e);
        }

        return execResult;
    }

    private void volumeEnableKerberosConfig(Set<ServiceConfig> volumePathSet,String appHome,String serviceRoleName, boolean enableKerberos) {
        if (enableKerberos) {
            String keytabDir = "/etc/security/keytab/";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("keytab");
            keytabConfig.setValue(keytabDir);
            volumePathSet.add(keytabConfig);

            String krb5Conf = "/etc/krb5.conf";
            ServiceConfig krb5ConfConfig = new ServiceConfig();
            krb5ConfConfig.setName("krd5conf");
            krb5ConfConfig.setValue(krb5Conf);
            volumePathSet.add(krb5ConfConfig);
        }else{
            if (serviceRoleName.equals("KafkaBroker")){
                Iterator<ServiceConfig> iterator = volumePathSet.iterator();
                while (iterator.hasNext()) {
                    ServiceConfig config = iterator.next();
                    String value =(String) config.getValue();
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
                runAs.getUser(), appHome,startRunner.getProgram(), String.join(" ", startRunner.getArgs())) : "tail -f /dev/null");
        data.put("statusCommand", statusRunner != null ? String.format("su - %s -c 'cd %s && sh %s %s'",
                runAs.getUser(), appHome,statusRunner.getProgram(), String.join(" ", statusRunner.getArgs())) : "exit 0");

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
            if (StrUtil.isNotBlank(outputDirectory)) {
                // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
                if (outputDirectory.startsWith(Constants.SLASH)) {
                    configFilePath = String.join(Constants.SLASH, outputDirectory, generators.getFilename());
                } else {
                    String output = generators.getOutputDirectory().replaceAll("^/+", "").replaceAll("/+$", "");
                    configFilePath = String.join(Constants.SLASH, appHome, output, generators.getFilename());
                }
            } else {
                configFilePath = String.join(Constants.SLASH, appHome, generators.getFilename());
            }

            Generators key = entry.getKey();
            String filename = key.getFilename();
            if (key.getOutputDirectory().startsWith("/var/kerberos/krb5kdc")) {
                continue;
            }
            // 配置文件挂载
            // if (!"java.env".equals(filename)) {
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("config" + fileCount++);
            fileConfig.setValue(configFilePath);
            volumePathSet.add(fileConfig);
            //}

            // path配置目录挂载
            for (ServiceConfig serviceConfig : entry.getValue()) {
                if (Constants.PATH.equals(serviceConfig.getConfigType())) {
                    ServiceConfig pathConfig = new ServiceConfig();
                    pathConfig.setName("path" + pathCount++);
                    pathConfig.setValue(serviceConfig.getValue());
                    volumePathSet.add(pathConfig);
                }
            }

        }
        if ("RANGER".equals(serviceName)) {
            volumePathSet.clear();
            String rangerDir = "/opt/datasophon/ranger-2.1.0";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("rangerdir");
            fileConfig.setValue(rangerDir);
            volumePathSet.add(fileConfig);
            String rangerAdminConf = "/etc/ranger/admin";
            ServiceConfig adminConfig = new ServiceConfig();
            adminConfig.setName("adminconf");
            adminConfig.setValue(rangerAdminConf);
            volumePathSet.add(adminConfig);
        }

        if ("Krb5Kdc".equals(serviceRoleName) || "KAdmin".equals(serviceRoleName)) {
            String krb5kdcDir = "/var/kerberos/krb5kdc";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("kerberos-data");
            fileConfig.setValue(krb5kdcDir);
            volumePathSet.add(fileConfig);
            String keytabDir = "/etc/security/keytab/";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("keytab");
            keytabConfig.setValue(keytabDir);
            volumePathSet.add(keytabConfig);
        }

        if ("OpenldapServer".equals(serviceRoleName)) {
            String openldapData = "/var/lib/openldap/";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("openldap-data");
            fileConfig.setValue(openldapData);
            volumePathSet.add(fileConfig);
            String openldapConf = "/etc/openldap/slapd.d";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("openldap-conf");
            keytabConfig.setValue(openldapConf);
            volumePathSet.add(keytabConfig);
        }

        if ("REDIS".equals(serviceName)) {
            String redisMasterCluster = appHome+"/cluster/";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("redis-cluster");
            fileConfig.setValue(redisMasterCluster);
            volumePathSet.add(fileConfig);
        }

        if ("POSTGRESQL".equals(serviceName)) {
            String postgresqlData = appHome+"/data/";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("postgresql-data");
            fileConfig.setValue(postgresqlData);
            volumePathSet.add(fileConfig);
        }

        if ("PostgresqlWorker".equals(serviceRoleName)) {
            volumePathSet.removeIf(config -> ((String)config.getValue()).contains("postgresql.conf"));
        }

        if ("ClickHouse".equals(serviceRoleName)) {
            String clickHouseData = "/var/lib/clickhouse/";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("clickhouse-data");
            fileConfig.setValue(clickHouseData);
            volumePathSet.add(fileConfig);
        }
    }

    private void volumeHadoopConfig(Set<ServiceConfig> volumePathSet) {
        List<String> needHadoopService = Arrays.asList("HIVE", "HBASE", "TRINO", "YARN", "SPARK3", "FLINK", "RANGER", "HUE");
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
                    ServiceConfig hadoopConfig = new ServiceConfig();
                    hadoopConfig.setName("hadoopconfig" + config++);
                    hadoopConfig.setValue(conf);
                    volumePathSet.add(hadoopConfig);
                }
            }
        }
        if ("HUE".equals(serviceName)) {
            ServiceConfig hiveConfig = new ServiceConfig();
            hiveConfig.setName("hive-config");
            hiveConfig.setValue("/opt/datasophon/hive-3.1.0/conf");
            volumePathSet.add(hiveConfig);

        }
    }

    private void volumeEnableRangerPluginConfig(Set<ServiceConfig> volumePathSet, boolean enableRangerPlugin) {
        if (enableRangerPlugin) {
            String keytabDir = "/etc/ranger/";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("rangerconf");
            keytabConfig.setValue(keytabDir);
            volumePathSet.add(keytabConfig);
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