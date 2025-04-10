/*
 *
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
 *
 */

package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.ClusterYarnScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RMHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final Logger logger = LoggerFactory.getLogger(RMHandlerStrategy.class);

        @Override
        public void handler(Integer clusterId, List<String> hosts) {

                Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${rm1}", hosts.get(0));
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${rm2}", hosts.get(1));
                ProcessUtils.generateClusterVariable(
                                globalVariables, clusterId, "${rmHost}", String.join(",", hosts));
        }

        @Override
        public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
                ClusterYarnSchedulerService schedulerService = SpringTool.getApplicationContext()
                                .getBean(ClusterYarnSchedulerService.class);
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
                boolean enableKerberos = false;
                Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);
                for (ServiceConfig config : list) {
                        if ("yarn.resourcemanager.scheduler.class".equals(config.getName())) {
                                ClusterYarnScheduler scheduler = schedulerService.getScheduler(clusterId);
                                if ("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler"
                                                .equals(config.getValue())) {
                                        if ("capacity".equals(scheduler.getScheduler())) {
                                                scheduler.setScheduler("fair");
                                                schedulerService.updateById(scheduler);
                                        }
                                } else {
                                        if ("fair".equals(scheduler.getScheduler())) {
                                                scheduler.setScheduler("capacity");
                                                schedulerService.updateById(scheduler);
                                        }
                                }
                        }
                        if ("enableKerberos".equals(config.getName())) {
                                enableKerberos = isEnableKerberos(
                                                clusterId, globalVariables, enableKerberos, config, "YARN");
                        }
                }
                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "YARN" + Constants.CONFIG;
                List<ServiceConfig> configs = ServiceConfigMap.get(key);
                ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
                if (enableKerberos) {
                        addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
                } else {
                        removeConfigWithKerberos(list, map, configs);
                }
                list.addAll(kbConfigs);
        }

        @Override
        public void handlerServiceRoleCheck(
                        ClusterServiceRoleInstanceEntity roleInstanceEntity,
                        Map<String, ClusterServiceRoleInstanceEntity> map) {
                // 调用通用方法，传递特定的actorPath
                performServiceRoleCheck(roleInstanceEntity, "rMStateActor");
        }

        @Override
        public void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
                        Map<String, ClusterServiceRoleInstanceEntity> map) {
                // 调用通用方法，传递特定的actorPath
                performServiceRoleCheck(roleInstanceEntity, "");
        }

        public ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
                Map<String, String> globalVariable = GlobalVariables.get(roleInstanceEntity.getClusterId());
                String rm2 = globalVariable.get("${rm2}");
                String commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/yarn rmadmin -getServiceState rm1";
                if (rm2.equals(roleInstanceEntity.getHostname())) {
                        commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/yarn rmadmin -getServiceState rm2";
                }
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                command.setCommandLine(commandLine);
                return command;
        }

        @Override
        public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
                try {
                        // 1. 获取服务配置
                        List<ServiceConfig> serviceConfigs = listServiceConfigByServiceInstance(serviceInstanceId);

                        // 2. 从配置中解析配置到map，方便快速查询
                        Map<String, Object> configMap = new HashMap<>();
                        for (ServiceConfig config : serviceConfigs) {
                                configMap.put(config.getName(), config.getValue());
                        }

                        // 3. 获取全局变量
                        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                        // 4. 获取ResourceManager主备节点
                        String rm1 = globalVariables.get("${rm1}");
                        String rm2 = globalVariables.get("${rm2}");

                        // 5. 获取服务角色主机列表
                        List<String> rmList = getRoleHosts(clusterId, "ResourceManager");

                        // 6. 判断是否启用了HA
                        boolean enableHA = rmList.size() > 1;

                        // 7. 判断是否启用了Kerberos
                        boolean enableKerberos = false;
                        for (ServiceConfig config : serviceConfigs) {
                                if ("enableKerberos".equals(config.getName())) {
                                        enableKerberos = Boolean.parseBoolean(config.getValue().toString());
                                        break;
                                }
                        }

                        // 8. 获取YARN端口，默认为8088(Web UI), 8032(应用程序提交)
                        String webPort = "8088";
                        String submissionPort = "8032";

                        // 9. 构建YARN Resource Manager地址
                        String rmAddress = rm1 + ":" + submissionPort;
                        String rmWebAddress = "http://" + rm1 + ":" + webPort;

                        // 10. 如果启用了HA，修改地址格式
                        if (enableHA && rm2 != null) {
                                rmAddress = rm1 + ":" + submissionPort + "," + rm2 + ":" + submissionPort;
                                // Web UI地址仍使用活跃的ResourceManager节点
                        }

                        // 11. 获取Hadoop安装目录
                        String hadoopHome = globalVariables.get("${HADOOP_HOME}");

                        // 12. 获取YARN调度器类型
                        String schedulerType = "Capacity Scheduler"; // 默认值
                        Object schedulerClass = configMap.get("yarn.resourcemanager.scheduler.class");
                        if (schedulerClass != null) {
                                String schedulerClassStr = schedulerClass.toString();
                                if (schedulerClassStr.contains("FairScheduler")) {
                                        schedulerType = "Fair Scheduler";
                                }
                        }

                        // 13. 构建基本连接信息
                        Map<String, String> basicInfo = new HashMap<>();
                        basicInfo.put("YARN Resource Manager地址", rmAddress);
                        basicInfo.put("YARN Web UI", rmWebAddress);
                        basicInfo.put("调度器类型", schedulerType);
                        if (enableHA) {
                                basicInfo.put("ResourceManager主节点", rm1);
                                basicInfo.put("ResourceManager从节点", rm2);
                        } else {
                                basicInfo.put("ResourceManager节点", rm1);
                        }
                        basicInfo.put("启用Kerberos", enableKerberos ? "是" : "否");

                        // 14. 构建有序的基本连接信息列表
                        List<Map<String, String>> basicInfoList = new ArrayList<>();

                        // YARN Resource Manager地址
                        Map<String, String> rmItem = new HashMap<>();
                        rmItem.put("label", "YARN Resource Manager地址");
                        rmItem.put("value", rmAddress);
                        basicInfoList.add(rmItem);

                        // YARN Web UI地址
                        Map<String, String> webItem = new HashMap<>();
                        webItem.put("label", "YARN Web UI");
                        webItem.put("value", rmWebAddress);
                        basicInfoList.add(webItem);

                        // 调度器类型
                        Map<String, String> schedulerItem = new HashMap<>();
                        schedulerItem.put("label", "调度器类型");
                        schedulerItem.put("value", schedulerType);
                        basicInfoList.add(schedulerItem);

                        // ResourceManager节点信息
                        if (enableHA) {
                                Map<String, String> rm1Item = new HashMap<>();
                                rm1Item.put("label", "ResourceManager主节点");
                                rm1Item.put("value", rm1);
                                basicInfoList.add(rm1Item);

                                Map<String, String> rm2Item = new HashMap<>();
                                rm2Item.put("label", "ResourceManager从节点");
                                rm2Item.put("value", rm2);
                                basicInfoList.add(rm2Item);
                        } else {
                                Map<String, String> rmNodeItem = new HashMap<>();
                                rmNodeItem.put("label", "ResourceManager节点");
                                rmNodeItem.put("value", rm1);
                                basicInfoList.add(rmNodeItem);
                        }

                        // Kerberos状态
                        Map<String, String> kerberosItem = new HashMap<>();
                        kerberosItem.put("label", "启用Kerberos");
                        kerberosItem.put("value", enableKerberos ? "是" : "否");
                        basicInfoList.add(kerberosItem);

                        // 15. 生成Java代码示例
                        String javaCode = generateJavaCode(rmAddress, enableKerberos);

                        // 16. 生成Python代码示例
                        String pythonCode = generatePythonCode(rmWebAddress, enableKerberos);

                        // 17. 生成命令行示例
                        List<CommandLineItem> commandLines = generateCommandLines(hadoopHome, enableKerberos, rm1);

                        // 18. 返回构建好的连接信息
                        return ConnectionInfo.builder()
                                        .basicInfo(basicInfo)
                                        .basicInfoList(basicInfoList)
                                        .javaCode(javaCode)
                                        .pythonCode(pythonCode)
                                        .commandLines(commandLines)
                                        .serviceHome(hadoopHome)
                                        .hostName(rm1)
                                        .build();
                } catch (Exception e) {
                        logger.error("获取YARN连接信息出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder().build();
                }
        }

        /**
         * 生成Java代码示例
         *
         * @param rmAddress      ResourceManager地址
         * @param enableKerberos 是否启用Kerberos
         * @return Java代码示例
         */
        private String generateJavaCode(String rmAddress, boolean enableKerberos) {
                StringBuilder code = new StringBuilder();
                code.append("// YARN Java API 示例代码\n")
                                .append("import org.apache.hadoop.conf.Configuration;\n")
                                .append("import org.apache.hadoop.yarn.api.records.*;\n")
                                .append("import org.apache.hadoop.yarn.client.api.YarnClient;\n")
                                .append("import org.apache.hadoop.yarn.conf.YarnConfiguration;\n")
                                .append("import org.apache.hadoop.yarn.exceptions.YarnException;\n")
                                .append("import org.apache.hadoop.yarn.util.Records;\n\n")
                                .append("import java.io.IOException;\n")
                                .append("import java.util.Collections;\n")
                                .append("import java.util.EnumSet;\n")
                                .append("import java.util.List;\n\n")
                                .append("public class YarnExample {\n")
                                .append("    public static void main(String[] args) {\n")
                                .append("        try {\n")
                                .append("            // 创建配置对象\n")
                                .append("            Configuration conf = new Configuration();\n")
                                .append("            YarnConfiguration yarnConf = new YarnConfiguration(conf);\n");

                // 设置ResourceManager地址，支持HA
                code.append("            // 设置ResourceManager地址\n")
                                .append("            yarnConf.set(YarnConfiguration.RM_ADDRESS, \"").append(rmAddress)
                                .append("\");\n\n");

                // 添加Kerberos配置（如果启用）
                if (enableKerberos) {
                        code.append("            // Kerberos 认证配置\n")
                                        .append("            conf.set(\"hadoop.security.authentication\", \"kerberos\");\n")
                                        .append("            conf.set(\"hadoop.security.authorization\", \"true\");\n")
                                        .append("            // 需要根据实际情况配置以下参数\n")
                                        .append("            // conf.set(\"yarn.resourcemanager.principal\", \"yarn/_HOST@HADOOP.COM\");\n")
                                        .append("            org.apache.hadoop.security.UserGroupInformation.setConfiguration(conf);\n")
                                        .append("            org.apache.hadoop.security.UserGroupInformation.loginUserFromKeytab(\"yarn@HADOOP.COM\", \"/path/to/yarn.keytab\");\n\n");
                }

                // 创建YARN客户端并提交应用
                code.append("            // 创建YARN客户端\n")
                                .append("            YarnClient yarnClient = YarnClient.createYarnClient();\n")
                                .append("            yarnClient.init(yarnConf);\n")
                                .append("            yarnClient.start();\n\n")
                                .append("            // 获取集群信息\n")
                                .append("            System.out.println(\"获取YARN集群信息:\");\n")
                                .append("            YarnClusterMetrics clusterMetrics = yarnClient.getYarnClusterMetrics();\n")
                                .append("            System.out.println(\"节点数量: \" + clusterMetrics.getNumNodeManagers());\n\n")
                                .append("            // 列出NodeManager\n")
                                .append("            List<NodeReport> nodeReports = yarnClient.getNodeReports(\n")
                                .append("                    org.apache.hadoop.yarn.api.records.NodeState.RUNNING);\n")
                                .append("            System.out.println(\"Node Reports:\");\n")
                                .append("            for (NodeReport node : nodeReports) {\n")
                                .append("                System.out.println(\"Node ID: \" + node.getNodeId() + \n")
                                .append("                        \", 地址: \" + node.getHttpAddress() + \n")
                                .append("                        \", 资源: \" + node.getCapability());\n")
                                .append("            }\n\n")
                                .append("            // 列出所有应用\n")
                                .append("            List<ApplicationReport> applications = yarnClient.getApplications();\n")
                                .append("            System.out.println(\"应用列表:\");\n")
                                .append("            for (ApplicationReport app : applications) {\n")
                                .append("                System.out.println(\"应用ID: \" + app.getApplicationId() + \n")
                                .append("                        \", 名称: \" + app.getName() + \n")
                                .append("                        \", 用户: \" + app.getUser() + \n")
                                .append("                        \", 队列: \" + app.getQueue() + \n")
                                .append("                        \", 状态: \" + app.getYarnApplicationState());\n")
                                .append("            }\n\n")
                                .append("            // 创建应用提交上下文\n")
                                .append("            ApplicationSubmissionContext appContext = Records.newRecord(ApplicationSubmissionContext.class);\n")
                                .append("            ApplicationId appId = yarnClient.createApplication().getApplicationSubmissionContext().getApplicationId();\n")
                                .append("            appContext.setApplicationId(appId);\n")
                                .append("            appContext.setApplicationName(\"YARN示例应用\");\n\n")
                                .append("            // 设置容器上下文\n")
                                .append("            ContainerLaunchContext containerContext = Records.newRecord(ContainerLaunchContext.class);\n")
                                .append("            appContext.setResource(Records.newRecord(Resource.class));\n")
                                .append("            appContext.getResource().setMemorySize(1024); // 1GB内存\n")
                                .append("            appContext.getResource().setVirtualCores(1);  // 1个虚拟核心\n")
                                .append("            appContext.setAMContainerSpec(containerContext);\n")
                                .append("            appContext.setPriority(Records.newRecord(Priority.class));\n")
                                .append("            appContext.getPriority().setPriority(0);\n\n")
                                .append("            // 实际应用程序需要设置更多参数，如命令、环境变量、本地资源等\n")
                                .append("            // containerContext.setCommands(...)\n")
                                .append("            // containerContext.setEnvironment(...)\n")
                                .append("            // containerContext.setLocalResources(...)\n\n")
                                .append("            // 提交应用程序\n")
                                .append("            // yarnClient.submitApplication(appContext);\n")
                                .append("            // System.out.println(\"应用程序 \" + appId + \" 已提交\");\n\n")
                                .append("            // 关闭YARN客户端\n")
                                .append("            yarnClient.close();\n")
                                .append("        } catch (Exception e) {\n")
                                .append("            e.printStackTrace();\n")
                                .append("        }\n")
                                .append("    }\n")
                                .append("}\n");

                return code.toString();
        }

        /**
         * 生成Python代码示例
         *
         * @param rmWebAddress   ResourceManager Web UI地址
         * @param enableKerberos 是否启用Kerberos
         * @return Python代码示例
         */
        private String generatePythonCode(String rmWebAddress, boolean enableKerberos) {
                StringBuilder code = new StringBuilder();
                code.append("# YARN Python API 示例代码\n")
                                .append("# 注意：Python没有官方的YARN客户端库\n")
                                .append("# 以下示例使用RESTful API和HTTP请求访问YARN\n\n")
                                .append("import requests\n")
                                .append("import json\n")
                                .append("import os\n\n");

                // Kerberos认证设置（如果启用）
                if (enableKerberos) {
                        code.append("# Kerberos认证设置\n")
                                        .append("import kerberos\n")
                                        .append("import requests_kerberos\n\n")
                                        .append("# 初始化Kerberos认证\n")
                                        .append("os.environ['KRB5CCNAME'] = '/tmp/krb5cc_$(id -u)'\n")
                                        .append("# 确保已经使用kinit获取了ticket\n")
                                        .append("# os.system('kinit -kt /path/to/user.keytab user@HADOOP.COM')\n\n")
                                        .append("# 创建带Kerberos认证的会话\n")
                                        .append("kerberos_auth = requests_kerberos.HTTPKerberosAuth(mutual_authentication=requests_kerberos.OPTIONAL)\n")
                                        .append("session = requests.Session()\n")
                                        .append("session.auth = kerberos_auth\n\n");
                } else {
                        code.append("# 创建HTTP会话\n")
                                        .append("session = requests.Session()\n\n");
                }

                // 查询YARN集群信息
                code.append("# ResourceManager Web UI地址\n")
                                .append("rm_web_address = \"").append(rmWebAddress).append("\"\n\n")
                                .append("# 获取YARN集群指标\n")
                                .append("def get_cluster_metrics():\n")
                                .append("    url = f\"{rm_web_address}/ws/v1/cluster/metrics\"\n")
                                .append("    response = session.get(url)\n")
                                .append("    if response.status_code == 200:\n")
                                .append("        return response.json()\n")
                                .append("    else:\n")
                                .append("        print(f\"获取集群指标失败：{response.status_code}\")\n")
                                .append("        return None\n\n")
                                .append("# 获取节点信息\n")
                                .append("def get_nodes():\n")
                                .append("    url = f\"{rm_web_address}/ws/v1/cluster/nodes\"\n")
                                .append("    response = session.get(url)\n")
                                .append("    if response.status_code == 200:\n")
                                .append("        return response.json()\n")
                                .append("    else:\n")
                                .append("        print(f\"获取节点信息失败：{response.status_code}\")\n")
                                .append("        return None\n\n")
                                .append("# 获取应用程序列表\n")
                                .append("def get_applications():\n")
                                .append("    url = f\"{rm_web_address}/ws/v1/cluster/apps\"\n")
                                .append("    response = session.get(url)\n")
                                .append("    if response.status_code == 200:\n")
                                .append("        return response.json()\n")
                                .append("    else:\n")
                                .append("        print(f\"获取应用程序列表失败：{response.status_code}\")\n")
                                .append("        return None\n\n")
                                .append("# 获取特定应用程序信息\n")
                                .append("def get_application(app_id):\n")
                                .append("    url = f\"{rm_web_address}/ws/v1/cluster/apps/{app_id}\"\n")
                                .append("    response = session.get(url)\n")
                                .append("    if response.status_code == 200:\n")
                                .append("        return response.json()\n")
                                .append("    else:\n")
                                .append("        print(f\"获取应用程序信息失败：{response.status_code}\")\n")
                                .append("        return None\n\n")
                                .append("# 获取队列信息\n")
                                .append("def get_scheduler_queues():\n")
                                .append("    url = f\"{rm_web_address}/ws/v1/cluster/scheduler\"\n")
                                .append("    response = session.get(url)\n")
                                .append("    if response.status_code == 200:\n")
                                .append("        return response.json()\n")
                                .append("    else:\n")
                                .append("        print(f\"获取调度器队列信息失败：{response.status_code}\")\n")
                                .append("        return None\n\n")
                                .append("# 示例使用\n")
                                .append("print(\"获取YARN集群指标...\")\n")
                                .append("metrics = get_cluster_metrics()\n")
                                .append("if metrics:\n")
                                .append("    print(json.dumps(metrics, indent=2))\n")
                                .append("    print(f\"活跃的NodeManager数量: {metrics['clusterMetrics']['activeNodes']}\")\n")
                                .append("    print(f\"总内存容量: {metrics['clusterMetrics']['totalMB']} MB\")\n")
                                .append("    print(f\"总CPU虚拟核心: {metrics['clusterMetrics']['totalVirtualCores']}\")\n\n")
                                .append("print(\"\\n获取队列信息...\")\n")
                                .append("scheduler = get_scheduler_queues()\n")
                                .append("if scheduler:\n")
                                .append("    print(json.dumps(scheduler['scheduler']['schedulerInfo'], indent=2))\n\n")
                                .append("print(\"\\n获取应用程序列表...\")\n")
                                .append("apps = get_applications()\n")
                                .append("if apps and 'apps' in apps and apps['apps']:\n")
                                .append("    for app in apps['apps']['app'][:5]:  # 只显示前5个应用\n")
                                .append("        print(f\"ID: {app['id']}, 名称: {app['name']}, 状态: {app['state']}, 进度: {app['progress']}%\")\n");

                return code.toString();
        }

        /**
         * 生成命令行示例
         *
         * @param hadoopHome     HADOOP_HOME环境变量
         * @param enableKerberos 是否启用Kerberos
         * @param hostname       主机名
         * @return 命令行示例列表
         */
        private List<CommandLineItem> generateCommandLines(String hadoopHome, boolean enableKerberos, String hostname) {
                List<CommandLineItem> commandLines = new ArrayList<>();

                // 命令提示符
                String serviceName = hadoopHome != null ? hadoopHome.substring(hadoopHome.lastIndexOf('/') + 1)
                                : "hadoop";
                String shellPrompt = "[root@" + hostname + " " + serviceName + "]# ";

                // 生成当前时间和不同的时间点
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                String currentYear = String.valueOf(now.getYear());

                // 为不同命令生成不同时间，每个命令的时间差异几分钟
                java.time.LocalDateTime time1 = now.minusMinutes(30);
                java.time.LocalDateTime time2 = now.minusMinutes(28);
                java.time.LocalDateTime time3 = now.minusMinutes(25);
                java.time.LocalDateTime time4 = now.minusMinutes(20);
                java.time.LocalDateTime time5 = now.minusMinutes(18);
                java.time.LocalDateTime time7 = now.minusMinutes(10);
                java.time.LocalDateTime time8 = now.minusMinutes(8);
                java.time.LocalDateTime time9 = now.minusMinutes(5);
                java.time.LocalDateTime time10 = now.minusMinutes(2);

                // 格式化时间字符串
                String timeStr1 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time1.getMonthValue(), time1.getDayOfMonth(),
                                time1.getHour(), time1.getMinute(), time1.getSecond());

                String timeStr2 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time2.getMonthValue(), time2.getDayOfMonth(),
                                time2.getHour(), time2.getMinute(), time2.getSecond());

                String timeStr3 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time3.getMonthValue(), time3.getDayOfMonth(),
                                time3.getHour(), time3.getMinute(), time3.getSecond());

                String timeStr4 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time4.getMonthValue(), time4.getDayOfMonth(),
                                time4.getHour(), time4.getMinute(), time4.getSecond());

                String timeStr5 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time5.getMonthValue(), time5.getDayOfMonth(),
                                time5.getHour(), time5.getMinute(), time5.getSecond());

                String timeStr7 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time7.getMonthValue(), time7.getDayOfMonth(),
                                time7.getHour(), time7.getMinute(), time7.getSecond());

                String timeStr8 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time8.getMonthValue(), time8.getDayOfMonth(),
                                time8.getHour(), time8.getMinute(), time8.getSecond());

                String timeStr9 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time9.getMonthValue(), time9.getDayOfMonth(),
                                time9.getHour(), time9.getMinute(), time9.getSecond());

                String timeStr10 = String.format("%s-%02d-%02d %02d:%02d:%02d",
                                currentYear, time10.getMonthValue(), time10.getDayOfMonth(),
                                time10.getHour(), time10.getMinute(), time10.getSecond());

                // Kerberos参数
                String kerberosParams = "";
                if (enableKerberos) {
                        kerberosParams = " -D \"hadoop.security.authentication=kerberos\"";
                }

                // 1. 查看YARN应用状态
                CommandLineItem applicationStatusCmd = CommandLineItem.builder()
                                .label("查看YARN应用状态")
                                .value("bin/yarn application -list" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                "Total number of applications (application-types: [] and states: [SUBMITTED, ACCEPTED, RUNNING]):4\n"
                                                                +
                                                                "                Application-Id\t    Application-Name\t    Application-Type\t      User\t     Queue\t             State\t       Final-State\t       Progress\t                       Tracking-URL\n"
                                                                +
                                                                "application_1619158697207_0004\t          SleepJob\t            MAPREDUCE\t      hdfs\t  default\t           RUNNING\t         UNDEFINED\t            10%\thttp://node03:8088/proxy/application_1619158697207_0004/\n"
                                                                +
                                                                "application_1619158697207_0003\t        SparkPi_1\t               SPARK\t      hdfs\t  default\t           RUNNING\t         UNDEFINED\t            80%\thttp://node03:8088/proxy/application_1619158697207_0003/\n"
                                                                +
                                                                "application_1619158697207_0002\t          HiveJob\t            MAPREDUCE\t      hive\t  default\t           RUNNING\t         UNDEFINED\t            60%\thttp://node03:8088/proxy/application_1619158697207_0002/\n"
                                                                +
                                                                "application_1619158697207_0001\t         TeraSort\t            MAPREDUCE\t      hdfs\t  default\t           RUNNING\t         UNDEFINED\t            95%\thttp://node03:8088/proxy/application_1619158697207_0001/")
                                .build();
                commandLines.add(applicationStatusCmd);

                // 2. 查看应用详情
                CommandLineItem applicationStatusDetailCmd = CommandLineItem.builder()
                                .label("查看应用详情")
                                .value("bin/yarn application -status application_1619158697207_0001" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult("Application Report : \n" +
                                                "\tApplication-Id : application_1619158697207_0001\n" +
                                                "\tApplication-Name : TeraSort\n" +
                                                "\tApplication-Type : MAPREDUCE\n" +
                                                "\tUser : hdfs\n" +
                                                "\tQueue : default\n" +
                                                "\tStart-Time : 1619159420223\n" +
                                                "\tFinish-Time : 0\n" +
                                                "\tProgress : 95%\n" +
                                                "\tState : RUNNING\n" +
                                                "\tFinal-State : UNDEFINED\n" +
                                                "\tTracking-URL : http://node03:8088/proxy/application_1619158697207_0001/\n"
                                                +
                                                "\tRPC Port : 42631\n" +
                                                "\tAM Host : node03")
                                .build();
                commandLines.add(applicationStatusDetailCmd);

                // 3. 查看应用日志
                CommandLineItem applicationLogsCmd = CommandLineItem.builder()
                                .label("查看应用日志")
                                .value("bin/yarn logs -applicationId application_1619158697207_0001" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult("Retrieving logs for container_1619158697207_0001_01_000001 and all of its children\n"
                                                +
                                                "Log Type: stderr\n" +
                                                "Log Upload Time: " + timeStr3 + "\n" +
                                                "Log Length: 1024\n" +
                                                "...\n" +
                                                timeStr1
                                                + " INFO [main] org.apache.hadoop.mapreduce.Job: The url to track the job: http://node03:8088/proxy/application_1619158697207_0001/\n"
                                                +
                                                timeStr2
                                                + " INFO [main] org.apache.hadoop.mapreduce.Job: Running job: job_1619158697207_0001\n"
                                                +
                                                "...")
                                .build();
                commandLines.add(applicationLogsCmd);

                // 4. 终止应用
                CommandLineItem killApplicationCmd = CommandLineItem.builder()
                                .label("终止应用")
                                .value("bin/yarn application -kill application_1619158697207_0001" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult("Killing application application_1619158697207_0001\n" +
                                                timeStr4
                                                + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                +
                                                "Application application_1619158697207_0001 has been killed")
                                .build();
                commandLines.add(killApplicationCmd);

                // 5. 查看队列
                CommandLineItem queueListCmd = CommandLineItem.builder()
                                .label("查看队列")
                                .value("bin/yarn queue -list" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                timeStr5 + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                                +
                                                                "Queue Information : \n" +
                                                                "Queue Name : default \n" +
                                                                "\tQueue State : RUNNING\n" +
                                                                "\tCapacity : 100.0%\n" +
                                                                "\tCurrent Capacity : 40.0%\n" +
                                                                "\tMaximum Capacity : 100.0%\n" +
                                                                "\tDefault Node Label expression : <DEFAULT_PARTITION>")
                                .build();
                commandLines.add(queueListCmd);

                // 6. 查看特定队列的应用
                CommandLineItem queueStatusCmd = CommandLineItem.builder()
                                .label("查看特定队列的应用")
                                .value("bin/yarn application -list -queue default" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                "Total number of applications (application-types: [] and states: [SUBMITTED, ACCEPTED, RUNNING]):4\n"
                                                                +
                                                                "                Application-Id\t    Application-Name\t    Application-Type\t      User\t     Queue\t             State\t       Final-State\t       Progress\t                       Tracking-URL\n"
                                                                +
                                                                "application_1619158697207_0004\t          SleepJob\t            MAPREDUCE\t      hdfs\t  default\t           RUNNING\t         UNDEFINED\t            10%\thttp://node03:8088/proxy/application_1619158697207_0004/\n"
                                                                +
                                                                "application_1619158697207_0003\t        SparkPi_1\t               SPARK\t      hdfs\t  default\t           RUNNING\t         UNDEFINED\t            80%\thttp://node03:8088/proxy/application_1619158697207_0003/\n"
                                                                +
                                                                "application_1619158697207_0002\t          HiveJob\t            MAPREDUCE\t      hive\t  default\t           RUNNING\t         UNDEFINED\t            60%\thttp://node03:8088/proxy/application_1619158697207_0002/")
                                .build();
                commandLines.add(queueStatusCmd);

                // 7. 查看节点报告
                CommandLineItem nodeListCmd = CommandLineItem.builder()
                                .label("查看节点报告")
                                .value("bin/yarn node -list -all" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                timeStr7 + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                                +
                                                                "Total Nodes:4\n" +
                                                                "         Node-Id\t     Node-State\tNode-Http-Address\tNumber-of-Running-Containers\n"
                                                                +
                                                                "     node01:45454\t        RUNNING\t      node01:8042\t                           2\n"
                                                                +
                                                                "     node02:45454\t        RUNNING\t      node02:8042\t                           5\n"
                                                                +
                                                                "     node03:45454\t        RUNNING\t      node03:8042\t                           3\n"
                                                                +
                                                                "     node04:45454\t        RUNNING\t      node04:8042\t                           0")
                                .build();
                commandLines.add(nodeListCmd);

                // 8. 查看特定节点状态
                CommandLineItem nodeStatusCmd = CommandLineItem.builder()
                                .label("查看特定节点状态")
                                .value("bin/yarn node -status node02:45454" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                timeStr8 + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                                +
                                                                "Node Report : \n" +
                                                                "\tNode-Id : node02:45454\n" +
                                                                "\tRack : /default-rack\n" +
                                                                "\tNode-State : RUNNING\n" +
                                                                "\tNode-Http-Address : node02:8042\n" +
                                                                "\tLast-Health-Update : " + timeStr8 + "\n"
                                                                +
                                                                "\tHealth-Report : \n" +
                                                                "\tContainers : 5\n" +
                                                                "\tMemory-Used : 10240MB\n" +
                                                                "\tMemory-Capacity : 16384MB\n" +
                                                                "\tCPU-Used : 8 vcores\n" +
                                                                "\tCPU-Capacity : 16 vcores\n" +
                                                                "\tNode-Labels : ")
                                .build();
                commandLines.add(nodeStatusCmd);

                // 9. 查看调度器信息
                CommandLineItem schedulerInfoCmd = CommandLineItem.builder()
                                .label("查看调度器信息")
                                .value("bin/yarn rmadmin -getGroups" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                timeStr9 + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                                +
                                                                "hdfs : hdfs hadoop\n" +
                                                                "yarn : yarn hadoop\n" +
                                                                "hive : hive hadoop")
                                .build();
                commandLines.add(schedulerInfoCmd);

                // 10. 检查YARN服务状态
                CommandLineItem checkServiceCmd = CommandLineItem.builder()
                                .label("检查YARN服务状态")
                                .value("bin/yarn rmadmin -getAllServiceState" + kerberosParams)
                                .commandPrompt(shellPrompt)
                                .commandResult(
                                                timeStr10 + " INFO client.RMProxy: Connecting to ResourceManager at node01:8032\n"
                                                                +
                                                                "rm1 : active\n" +
                                                                "rm2 : standby")
                                .build();
                commandLines.add(checkServiceCmd);

                return addFinalPrompt(commandLines, hostname);
        }

}
