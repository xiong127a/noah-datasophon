/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.TemplatePathUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.FreemarkerUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ServiceHandlerAbstract {
    private static final Logger log = LoggerFactory.getLogger(ServiceHandlerAbstract.class);

    public ClusterServiceRoleInstanceEntity roleInstanceEntity;

    /**
     * 添加通用命令到命令行列表中
     *
     * @param commandLines 命令行列表
     * @param hostname     主机名
     * @return 更新后的命令行列表
     */
    public List<CommandLineItem> addFinalPrompt(List<CommandLineItem> commandLines, String serviceName,
            String serviceHome,
            String hostname) {
        // 如果列表为空，创建一个新列表
        if (commandLines == null) {
            commandLines = new ArrayList<>();
        }
        String processedServiceHome = processServiceHome(serviceHome);

        // 获取正确的服务目录提示符
        String serviceDirName = serviceHome.substring(serviceHome.lastIndexOf('/') + 1);
        String serviceHomePrompt = "[root@" + hostname + " " + serviceDirName + "]# ";

        // 添加SSH登录命令，放在最前面
        CommandLineItem sshCommand = new CommandLineItem();
        sshCommand.setLabel("通过SSH登录到服务器");
        sshCommand.setValue("ssh root@" + hostname);
        sshCommand.setCommandPrompt("[user@localhost ~]$ ");
        sshCommand.setCommandResult(
                "Last login: " + new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.CHINA).format(new Date())
                        + " from 192.168.x.x\n" +
                        "Welcome to CentOS Linux 7 (Core)");

        // 将SSH命令添加到列表最前面
        commandLines.add(0, sshCommand);

        // 添加cd到服务目录的命令
        CommandLineItem cdCommand = new CommandLineItem();

        cdCommand.setLabel("切换到" + serviceName + "服务");
        cdCommand.setValue("cd " + processedServiceHome);
        cdCommand.setCommandPrompt("[root@" + hostname + " ~]# ");
        cdCommand.setCommandResult(""); // 通常cd命令没有输出
        commandLines.add(1, cdCommand);

        // 添加一个date命令，显示当前日期时间 (中文格式)
        CommandLineItem dateCommand = new CommandLineItem();
        dateCommand.setLabel("显示当前日期时间");
        dateCommand.setValue("date");
        // 获取当前日期时间并格式化为中文
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss E", Locale.CHINA);
        // 延迟10秒左右，确保显示的时间比实际时间晚一点
        // 随机生成5-10秒的延迟时间
        int randomDelay = 5000 + new Random().nextInt(5001); // 5000-10000毫秒之间的随机值
        String currentDateTime = sdf.format(new Date(System.currentTimeMillis() + randomDelay));
        dateCommand.setCommandResult(currentDateTime);
        dateCommand.setCommandPrompt(serviceHomePrompt); // 使用服务目录提示符

        // 添加一个空的命令提示符在最后
        CommandLineItem command = new CommandLineItem();
        command.setLabel("");
        command.setValue("");
        command.setCommandPrompt(serviceHomePrompt); // 使用服务目录提示符

        // 将date命令和空命令添加到列表末尾
        commandLines.add(dateCommand);
        commandLines.add(command);

        return commandLines;
    }

    public List<String> getRoleHosts(Integer clusterId, Integer serviceInstanceId, String roleName) {
        ClusterServiceRoleInstanceService clusterServiceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        List<ClusterServiceRoleInstanceEntity> roleInstances = clusterServiceRoleInstanceService
                .getServiceRoleInstanceListByServiceInstanceIdAndRoleName(clusterId, serviceInstanceId, roleName);
        return CollUtil.map(roleInstances, ClusterServiceRoleInstanceEntity::getHostname, true);
    }

    public void removeConfigWithKerberos(List<ServiceConfig> list, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithKerberos()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    public void removeConfigWithHA(List<ServiceConfig> list, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithHA()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    public void removeConfigWithRack(List<ServiceConfig> list, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithRack()) {
                if (map.containsKey(serviceConfig.getName())) {
                    list.remove(map.get(serviceConfig.getName()));
                }
            }
        }
    }

    /**
     * 将所有service_ddl.json中configType是kb的配置项加入到当前配置列表
     * isConfigWithKerberos判定条件在 service_ddl.json 中设置 configWithKerberos = true
     *
     * @param globalVariables 全局变量
     * @param map             当前前端传入的配置项
     * @param configs         所有service_ddl.json中设置的所有配置项
     * @param kbConfigs       需要添加到当前的配置项
     */
    public void addConfigWithKerberos(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs, ArrayList<ServiceConfig> kbConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithKerberos()) {
                addConfig(globalVariables, map, kbConfigs, serviceConfig);
            }
        }
    }

    public void addConfigWithHA(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs, ArrayList<ServiceConfig> kbConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithHA()) {
                addConfig(globalVariables, map, kbConfigs, serviceConfig);
            }
        }
    }

    public void addConfigWithRack(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
            List<ServiceConfig> configs, List<ServiceConfig> rackConfigs) {
        for (ServiceConfig serviceConfig : configs) {
            if (serviceConfig.isConfigWithRack()) {
                addConfig(globalVariables, map, rackConfigs, serviceConfig);
            }
        }
    }

    public void addConfig(Map<String, String> globalVariables, Map<String, ServiceConfig> map,
            List<ServiceConfig> rackConfigs, ServiceConfig serviceConfig) {
        if (map.containsKey(serviceConfig.getName())) {
            ServiceConfig config = map.get(serviceConfig.getName());
            config.setRequired(true);
            config.setHidden(false);
            if (Constants.INPUT.equals(config.getType())) {
                String value = PlaceholderUtils.replacePlaceholders((String) config.getValue(), globalVariables,
                        Constants.REGEX_VARIABLE);
                config.setValue(value);
            }
        } else {
            serviceConfig.setRequired(true);
            serviceConfig.setHidden(false);
            if (Constants.INPUT.equals(serviceConfig.getType())) {
                String value = PlaceholderUtils.replacePlaceholders((String) serviceConfig.getValue(), globalVariables,
                        Constants.REGEX_VARIABLE);
                serviceConfig.setValue(value);
            }
            rackConfigs.add(serviceConfig);
        }
    }

    public boolean isEnableKerberos(Integer clusterId, Map<String, String> globalVariables, boolean enableKerberos,
            ServiceConfig config, String serviceName) {
        if ((Boolean) config.getValue()) {
            enableKerberos = true;
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "Kerberos}",
                    "true");
        } else {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "Kerberos}",
                    "false");
        }
        return enableKerberos;
    }

    public boolean isEnableHA(Integer clusterId, Map<String, String> globalVariables, boolean enableHA,
            ServiceConfig config, String serviceName) {
        if ((Boolean) config.getValue()) {
            enableHA = true;
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "HA}", "true");
        } else {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enable" + serviceName + "HA}", "false");
        }
        return enableHA;
    }

    public boolean isEnableRack(boolean enableRack, ServiceConfig config) {
        if ((Boolean) config.getValue()) {
            enableRack = true;
        }
        return enableRack;
    }

    /**
     * 从模板文件中读取并替换变量
     *
     * @param templateType 模板类型（java, python, shell）
     * @param serviceName  服务名称（小写，如：clickhouse, doris等）
     * @param data         数据模型对象(ConnectionInfo)
     * @return 处理后的模板内容
     */
    protected String getTemplateContent(String templateType, String serviceName, Object data) {
        try {
            // 构建模板文件路径
            String templatePath = String.format("templates/%s/", templateType);

            // 根据模板类型确定文件名
            String fileName;
            switch (templateType) {
                case "java":
                    fileName = String.format("%sExample.ftl", StrUtil.toCamelCase(serviceName));
                    break;
                case "python":
                    fileName = String.format("%s_example.ftl", serviceName.toLowerCase());
                    break;
                case "shell":
                    fileName = String.format("%s_commands.ftl", serviceName.toLowerCase());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的模板类型: " + templateType);
            }

            // 读取模板文件
            File templateFile = TemplatePathUtils.getTemplateFile(templatePath, fileName);

            // 添加日志，输出模板文件路径
            log.info("模板文件路径: {}", Objects.requireNonNull(templateFile).getAbsolutePath());

            String content = FileUtil.readString(templateFile, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(content)) {
                log.error("模板文件内容为空: {}", templatePath + fileName);
                return "";
            }

            try {
                // 使用FreemarkerUtils处理模板
                Template template = FreemarkerUtils.createTemplateFromContent(content, fileName);

                // 创建数据模型，将ConnectionInfo对象作为data变量传递给模板
                Map<String, Object> dataModel = new HashMap<>();
                dataModel.put("data", data);

                // 如果数据对象是ConnectionInfo类型，并且有模板变量，将其展开到顶级
                if (data instanceof ConnectionInfo) {
                    ConnectionInfo connectionInfo = (ConnectionInfo) data;
                    Map<String, Object> templateVars = connectionInfo.getTemplateVariables();
                    if (templateVars != null && !templateVars.isEmpty()) {
                        // 将模板变量展开到顶级，这样在模板中可以直接使用这些变量
                        dataModel.putAll(templateVars);
                        log.debug("添加顶级模板变量: {}", templateVars.keySet());
                    }
                }

                return FreemarkerUtils.renderTemplateToString(template, dataModel);
            } catch (Exception e) {
                log.error("Freemarker处理模板失败: {}", e.getMessage(), e);
                return "";
            }
        } catch (Exception e) {
            log.error("获取模板内容失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 从代码中提取依赖信息部分
     *
     * @param code 完整代码
     * @return 依赖信息部分
     */
    protected String extractDependencies(String code) {
        if (StringUtils.isBlank(code)) {
            return "";
        }

        // 使用正则表达式提取DEPENDENCIES_START和DEPENDENCIES_END之间的内容
        Pattern pattern = Pattern.compile("DEPENDENCIES_START\\s*([\\s\\S]*?)\\s*DEPENDENCIES_END", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /**
     * 从依赖信息中生成摘要
     *
     * @param dependencies 依赖信息
     * @param type         依赖类型（java或python）
     * @return 依赖摘要
     */
    protected String generateDependencySummary(String dependencies, String type) {
        if (StringUtils.isBlank(dependencies)) {
            return "";
        }

        if ("java".equals(type)) {
            // 对Java Maven依赖生成摘要
            Pattern artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
            Matcher matcher = artifactPattern.matcher(dependencies);

            List<String> artifacts = new ArrayList<>();
            while (matcher.find()) {
                artifacts.add(matcher.group(1));
            }

            if (!artifacts.isEmpty()) {
                return "Maven: " + String.join(", ", artifacts);
            }
        } else if ("python".equals(type)) {
            // 对Python pip依赖生成摘要
            // 首先按行分割文本，排除注释行
            String[] lines = dependencies.split("\\r?\\n");
            List<String> packages = new ArrayList<>();

            for (String line : lines) {
                line = line.trim();
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 匹配形如 "package==version" 或 "package>=version" 的依赖
                Pattern pipPattern = Pattern.compile("^([\\w\\-.]+)(?:==|>=|>|<|<=|~=|!=)?[\\d.]*");
                Matcher matcher = pipPattern.matcher(line);

                if (matcher.find()) {
                    String pkg = matcher.group(1).trim();
                    if (!pkg.isEmpty() && !packages.contains(pkg)) {
                        packages.add(pkg);
                    }
                }
            }

            if (!packages.isEmpty()) {
                return "Pip: " + String.join(", ", packages);
            }
        }

        return "";
    }

    /**
     * 移除代码中的依赖标记部分，返回纯代码
     *
     * @param code 包含依赖标记的完整代码
     * @return 移除依赖标记后的代码
     */
    protected String removeDependencyMarkers(String code) {
        if (StringUtils.isBlank(code)) {
            return "";
        }

        // 移除DEPENDENCIES_START到DEPENDENCIES_END之间的所有内容（包括标记本身）
        return code.replaceAll("DEPENDENCIES_START[\\s\\S]*?DEPENDENCIES_END\\s*", "");
    }

    /**
     * 生成Java代码示例
     *
     * @param serviceName    服务名称
     * @param connectionInfo 连接信息对象
     * @return Java代码内容的Map，包含代码、依赖和依赖摘要
     */
    protected Map<String, String> generateJavaCode(String serviceName, ConnectionInfo connectionInfo) {
        Map<String, String> result = new HashMap<>();
        result.put("code", "");
        result.put("dependencies", "");
        result.put("dependencySummary", "");

        if (connectionInfo == null) {
            log.error("生成Java代码失败: ConnectionInfo对象为空");
            return result;
        }

        try {
            // 日志记录开始生成Java代码
            log.info("开始生成Java代码示例，服务名称: {}", serviceName);

            // 获取并处理模板内容，直接使用connectionInfo作为数据模型
            String fullCode = getTemplateContent("java", serviceName, connectionInfo);

            if (StringUtils.isBlank(fullCode)) {
                log.error("生成的Java代码为空，服务名称: {}", serviceName);
                return result;
            }

            // 提取依赖信息
            String dependencies = extractDependencies(fullCode);
            String dependencySummary = generateDependencySummary(dependencies, "java");

            // 移除依赖标记，获取纯代码
            String pureCode = removeDependencyMarkers(fullCode);

            result.put("code", pureCode);
            result.put("dependencies", dependencies);
            result.put("dependencySummary", dependencySummary);

            log.info("Java代码生成成功，代码长度: {}, 依赖长度: {}, 摘要: {}",
                    pureCode != null ? pureCode.length() : 0,
                    dependencies != null ? dependencies.length() : 0,
                    dependencySummary);
        } catch (Exception e) {
            log.error("生成Java代码失败: {}, 异常堆栈: {}", e.getMessage(), ProcessUtils.getExceptionMessage(e));
        }

        return result;
    }

    /**
     * 生成Python代码示例
     *
     * @param serviceName    服务名称
     * @param connectionInfo 连接信息对象
     * @return Python代码内容的Map，包含代码、依赖和依赖摘要
     */
    protected Map<String, String> generatePythonCode(String serviceName, ConnectionInfo connectionInfo) {
        Map<String, String> result = new HashMap<>();
        result.put("code", "");
        result.put("dependencies", "");
        result.put("dependencySummary", "");

        if (connectionInfo == null) {
            log.error("生成Python代码失败: ConnectionInfo对象为空");
            return result;
        }

        try {
            // 日志记录开始生成Python代码
            log.info("开始生成Python代码示例，服务名称: {}", serviceName);

            // 获取并处理模板内容，直接使用connectionInfo作为数据模型
            String fullCode = getTemplateContent("python", serviceName, connectionInfo);

            if (StringUtils.isBlank(fullCode)) {
                log.error("生成的Python代码为空，服务名称: {}", serviceName);
                return result;
            }

            // 提取依赖信息
            String dependencies = extractDependencies(fullCode);
            String dependencySummary = generateDependencySummary(dependencies, "python");

            // 移除依赖标记，获取纯代码
            String pureCode = removeDependencyMarkers(fullCode);

            result.put("code", pureCode);
            result.put("dependencies", dependencies);
            result.put("dependencySummary", dependencySummary);

            log.info("Python代码生成成功，代码长度: {}, 依赖长度: {}, 摘要: {}",
                    pureCode != null ? pureCode.length() : 0,
                    dependencies != null ? dependencies.length() : 0,
                    dependencySummary);
        } catch (Exception e) {
            log.error("生成Python代码失败: {}, 异常堆栈: {}", e.getMessage(), ProcessUtils.getExceptionMessage(e));
        }

        return result;
    }

    /**
     * 生成Shell命令示例
     *
     * @param serviceName    服务名称
     * @param connectionInfo 连接信息对象
     * @return Shell命令内容
     */
    protected String generateShellCommands(String serviceName, ConnectionInfo connectionInfo) {
        return getTemplateContent("shell", serviceName, connectionInfo);
    }

    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId, String serviceHome,
            Map<String, String> configMap) {
        // 提取服务名称（从子类类名）
        String serviceName = getServiceName(serviceInstanceId);

        try {
            // 获取基本连接信息对象
            ConnectionInfo connectionInfo;

            try {
                // 调用子类实现，获取服务特定的连接信息
                ConnectionInfo.ConnectionInfoBuilder builder = getServiceSpecificConnectionInfo(clusterId,
                        serviceInstanceId, configMap);

                if (builder == null) {
                    log.warn("子类未提供有效的ConnectionInfo，使用空对象");
                    connectionInfo = ConnectionInfo.builder().build();
                } else {
                    // 构建连接信息对象
                    connectionInfo = builder.build();
                }

                if (connectionInfo.getServiceHome() == null || connectionInfo.getServiceHome().isEmpty()) {
                    connectionInfo.setServiceHome(serviceHome);
                }

                // 设置默认文件名和标题，父类统一处理
                if (connectionInfo.getJavaFileName() == null || connectionInfo.getJavaFileName().isEmpty() ||
                        connectionInfo.getPythonFileName() == null || connectionInfo.getPythonFileName().isEmpty() ||
                        connectionInfo.getCommandTitle() == null || connectionInfo.getCommandTitle().isEmpty() ||
                        connectionInfo.getJavaTitle() == null || connectionInfo.getJavaTitle().isEmpty() ||
                        connectionInfo.getPythonTitle() == null || connectionInfo.getPythonTitle().isEmpty()) {

                    setDefaultFileNamesAndTitles(connectionInfo, serviceName);
                }

            } catch (Exception e) {
                log.error("获取基本连接信息失败: {}", e.getMessage(), e);
                // 创建一个空的ConnectionInfo对象
                connectionInfo = ConnectionInfo.builder().build();
            }

            try {
                // 处理Java代码和依赖，直接传入connectionInfo对象
                Map<String, String> javaCodeResult = generateJavaCode(serviceName, connectionInfo);
                connectionInfo.setJavaCode(javaCodeResult.get("code"));
                connectionInfo.setJavaDependencies(javaCodeResult.get("dependencies"));
                connectionInfo.setJavaDependenciesSummary(javaCodeResult.get("dependencySummary"));

            } catch (Exception e) {
                log.error("处理Java代码示例失败: {}", e.getMessage(), e);
                connectionInfo.setJavaCode("");
                connectionInfo.setJavaDependencies("");
                connectionInfo.setJavaDependenciesSummary("");
            }

            try {
                // 处理Python代码和依赖，直接传入connectionInfo对象
                Map<String, String> pythonCodeResult = generatePythonCode(serviceName, connectionInfo);
                connectionInfo.setPythonCode(pythonCodeResult.get("code"));
                connectionInfo.setPythonDependencies(pythonCodeResult.get("dependencies"));
                connectionInfo.setPythonDependenciesSummary(pythonCodeResult.get("dependencySummary"));

            } catch (Exception e) {
                log.error("处理Python代码示例失败: {}", e.getMessage(), e);
                connectionInfo.setPythonCode("");
                connectionInfo.setPythonDependencies("");
                connectionInfo.setPythonDependenciesSummary("");
            }

            try {
                // 处理命令行示例
                String shellCommands = generateShellCommands(serviceName, connectionInfo);
                List<CommandLineItem> commandLines = parseCommandLines(shellCommands, serviceName, serviceHome,
                        connectionInfo.getHostName());
                connectionInfo.setCommandLines(commandLines);
            } catch (Exception e) {
                log.error("处理命令行示例失败: {}", e.getMessage(), e);
                connectionInfo.setCommandLines(new ArrayList<>()); // 设置为空列表
            }

            return connectionInfo;
        } catch (Exception e) {
            log.error("获取{}连接信息出错: {}", serviceName, e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    private String getServiceName(Integer serviceInstanceId) {
        ClusterServiceInstanceService clusterServiceInstanceService = SpringUtil
                .getBean(ClusterServiceInstanceService.class);
        ClusterServiceInstanceEntity clusterServiceInstanceEntity = clusterServiceInstanceService
                .getById(serviceInstanceId);
        return clusterServiceInstanceEntity.getServiceName();
    }

    /**
     * 解析命令行格式，转换为CommandLineItem列表
     *
     * @param commandLines 命令行字符串
     * @param serviceHome  服务安装目录
     * @param hostname     主机名
     * @return CommandLineItem列表
     */
    protected List<CommandLineItem> parseCommandLines(String commandLines, String serviceName, String serviceHome,
            String hostname) {
        List<CommandLineItem> commandLineItems = new ArrayList<>();
        if (StringUtils.isBlank(commandLines)) {
            return commandLineItems;
        }

        // 获取正确的服务目录提示符（只显示最后一级目录名）
        String defaultPrompt = "[root@" + hostname + " " + serviceHome + "]# ";

        // 分割命令行
        String[] lines = commandLines.split("\r?\n"); // 支持不同换行符
        CommandLineItem currentItem = null;
        String currentPrompt = defaultPrompt; // 当前使用的提示符
        String lastLineType = ""; // 记录上一行类型 (CMD, RES, TIP, PRT, COMMENT, OTHER)

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过空行
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }

            // 处理命令分隔符
            if (trimmedLine.startsWith("<--->")) {
                currentItem = null; // 分隔符表示新命令开始
                lastLineType = "SEPARATOR";
                continue;
            }

            // 处理注释行
            if (trimmedLine.startsWith("#")) {
                // 如果当前有命令项且没有标签，则添加注释作为说明
                if (currentItem != null && StringUtils.isBlank(currentItem.getLabel())) {
                    currentItem.setLabel(trimmedLine.substring(1).trim());
                }
                lastLineType = "COMMENT";
                continue;
            }

            // 处理命令提示符行
            if (trimmedLine.startsWith("PRT>")) {
                currentPrompt = trimmedLine.substring(4).trim();
                lastLineType = "PRT";
                continue;
            }

            // 处理提示信息行
            if (trimmedLine.startsWith("TIP>")) {
                String tipText = trimmedLine.substring(4).trim();

                // 如果TIP行出现在CMD行之前，则创建一个新的CommandLineItem
                if (currentItem == null) {
                    currentItem = new CommandLineItem();
                    currentItem.setLabel(tipText);
                    // 预设默认提示符，后续会被PRT行或CMD行更新
                    currentItem.setCommandPrompt(defaultPrompt);
                    commandLineItems.add(currentItem);
                } else if (StringUtils.isBlank(currentItem.getLabel())) {
                    // 已有CommandLineItem但尚未设置标签
                    currentItem.setLabel(tipText);
                } else {
                    // 已有标签的情况，追加新的TIP内容
                    currentItem.setLabel(currentItem.getLabel() + "\n" + tipText);
                }
                lastLineType = "TIP";
                continue;
            }

            // 处理命令输入行
            if (trimmedLine.startsWith("CMD>")) {
                String commandValue = trimmedLine.substring(4).trim();
                // 如果是新命令或上一个不是CMD行，则创建新项
                if (currentItem == null || !"CMD".equals(lastLineType)) {
                    currentItem = new CommandLineItem();
                    currentItem.setValue(commandValue);
                    // 确保使用当前最新的提示符
                    currentItem.setCommandPrompt(currentPrompt);
                    commandLineItems.add(currentItem);
                } else {
                    // 否则，追加到当前命令项
                    currentItem.setValue(currentItem.getValue() + "\n" + commandValue);
                }
                lastLineType = "CMD";
                continue;
            }

            // 处理命令输出结果行
            if (trimmedLine.startsWith("RES>") && currentItem != null) {
                String result = currentItem.getCommandResult();
                if (StringUtils.isBlank(result)) {
                    currentItem.setCommandResult(trimmedLine.substring(4).trim());
                } else {
                    currentItem.setCommandResult(result + "\n" + trimmedLine.substring(4).trim());
                }
                lastLineType = "RES";
                continue;
            }

            // 处理其他行（通常视为上一个命令的输出结果）
            if (currentItem != null) {
                String result = currentItem.getCommandResult();
                if (StringUtils.isBlank(result)) {
                    currentItem.setCommandResult(trimmedLine);
                } else {
                    currentItem.setCommandResult(result + "\n" + trimmedLine);
                }
                lastLineType = "OTHER"; // 标记为其他行，防止被错误地追加为命令
            }
        }

        // 统一命令提示符格式
        for (CommandLineItem item : commandLineItems) {
            String prompt = item.getCommandPrompt();
            // 如果提示符包含完整路径格式，替换为只显示目录名的格式
            if (prompt != null && prompt.contains("/opt/")) {
                item.setCommandPrompt(defaultPrompt);
            }
        }

        // 添加最终提示
        return addFinalPrompt(commandLineItems, serviceName, serviceHome, hostname);
    }

    /**
     * 子类需要实现的方法，提供服务特定的连接信息
     *
     * @return ConnectionInfo.ConnectionInfoBuilder 包含服务特定信息的构建器
     */
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
            Integer clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        // 默认实现返回空构建器
        return ConnectionInfo.builder();
    }

    /**
     * 处理服务目录路径，确保路径格式正确
     *
     * @param serviceHome 服务安装目录
     * @return 处理后的完整服务目录路径
     */
    protected String processServiceHome(String serviceHome) {
        if (StrUtil.isBlank(serviceHome)) {
            return Constants.INSTALL_PATH;
        }

        // 使用hutool的FileUtil.normalize方法规范化路径
        // 自动处理路径分隔符，避免重复的斜杠，并确保路径格式正确
        return FileUtil.normalize(Constants.INSTALL_PATH + "/" + serviceHome);
    }

    /**
     * 设置ConnectionInfo的默认文件名和标题
     * 子类可以直接调用此方法，避免重复编写设置代码
     *
     * @param connectionInfo 连接信息对象或构建器
     * @param serviceName    服务名称
     */
    protected void setDefaultFileNamesAndTitles(ConnectionInfo connectionInfo, String serviceName) {
        // 默认Java文件名：XxxExample.java
        if (connectionInfo.getJavaFileName() == null || connectionInfo.getJavaFileName().isEmpty()) {
            connectionInfo.setJavaFileName(StringUtils.capitalize(serviceName) + "Example.java");
        }

        // 默认Python文件名：xxx_example.py
        if (connectionInfo.getPythonFileName() == null || connectionInfo.getPythonFileName().isEmpty()) {
            connectionInfo.setPythonFileName(serviceName.toLowerCase() + "_example.py");
        }

        // 默认Shell命令标题：Xxx命令行操作示例
        if (connectionInfo.getCommandTitle() == null || connectionInfo.getCommandTitle().isEmpty()) {
            connectionInfo.setCommandTitle(StringUtils.capitalize(serviceName) + "命令行操作示例");
        }

        // 默认Java标题：Xxx Java示例
        if (connectionInfo.getJavaTitle() == null || connectionInfo.getJavaTitle().isEmpty()) {
            connectionInfo.setJavaTitle(StringUtils.capitalize(serviceName) + " Java示例");
        }

        // 默认Python标题：Xxx Python示例
        if (connectionInfo.getPythonTitle() == null || connectionInfo.getPythonTitle().isEmpty()) {
            connectionInfo.setPythonTitle(StringUtils.capitalize(serviceName) + " Python示例");
        }
    }

}
