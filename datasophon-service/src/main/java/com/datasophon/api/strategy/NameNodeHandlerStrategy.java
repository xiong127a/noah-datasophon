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
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class NameNodeHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final String ENABLE_RACK = "enableRack";

    private static final String ENABLE_KERBEROS = "enableKerberos";

    @Override
    public void handler(Integer clusterId, List<String> hosts) {

        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nn1}", hosts.get(0));
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${nn2}", hosts.get(1));
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);

        boolean enableRack = false;
        boolean enableKerberos = false;
        Map<String, ServiceConfig> map = ProcessUtils.translateToMap(list);

        String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HDFS" + Constants.CONFIG;
        List<ServiceConfig> configs = ServiceConfigMap.get(key);

        for (ServiceConfig config : list) {
            if (ENABLE_RACK.equals(config.getName())) {
                if ((Boolean) config.getValue()) {
                    enableRack = isEnableRack(enableRack, config);
                }
            }
            if (ENABLE_KERBEROS.equals(config.getName())) {
                enableKerberos = isEnableKerberos(
                        clusterId, globalVariables, enableKerberos, config, "HDFS");
            }
        }
        List<ServiceConfig> rackConfigs = new ArrayList<>();
        if (enableRack) {
            log.info("start to add rack config");
            addConfigWithRack(globalVariables, map, configs, rackConfigs);
        } else {
            removeConfigWithRack(list, map, configs);
        }
        list.addAll(rackConfigs);

        ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
        if (enableKerberos) {
            addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
        } else {
            removeConfigWithKerberos(list, map, configs);
        }
        list.addAll(kbConfigs);
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
        if (hostname.equals(globalVariables.get("${nn2}"))) {
            log.info("set to slave namenode");
            serviceRoleInfo.setSlave(true);
            serviceRoleInfo.setSortNum(5);
        }
    }

    @Override
    public void handlerServiceRoleCheck(
            ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
        performServiceRoleCheck(roleInstanceEntity, "nMStateActor");
    }

    @Override
    public void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
        performServiceRoleCheck(roleInstanceEntity, "");
    }

    public ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        Map<String, String> globalVariable = GlobalVariables.get(roleInstanceEntity.getClusterId());
        String nn2 = globalVariable.get("${nn2}");
        String commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn1";
        if (nn2.equals(roleInstanceEntity.getHostname())) {
            commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn2";
        }
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        cmdCommand.setCommandLine(commandLine);
        return cmdCommand;
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

            // 4. 获取全局变量
            Map<String, String> globalVariables = GlobalVariables.get(clusterId);

            // 5. 获取NameNode主备节点
            String nn1 = globalVariables.get("${nn1}");
            String nn2 = globalVariables.get("${nn2}");

            // 6. 获取服务角色主机列表
            List<String> nameNodeList = getRoleHosts(clusterId, "NameNode");

            // 7. 判断是否启用了HA
            boolean enableHA = nameNodeList.size() > 1;

            // 8. 判断是否启用了Kerberos
            boolean enableKerberos = false;
            for (ServiceConfig config : serviceConfigs) {
                if ("enableKerberos".equals(config.getName())) {
                    enableKerberos = Boolean.parseBoolean(config.getValue().toString());
                    break;
                }
            }

            // 9. 获取HDFS端口，默认为8020 (RPC端口)，9870 (HTTP端口)
            String rpcPort = "8020";

            // 10. 构建HDFS URI
            String hdfsUri = "hdfs://" + nn1 + ":" + rpcPort;

            // 11. 如果启用了HA，修改URI格式
            String nameservice = globalVariables.get("${nameservice}");
            // 如果globalVariables中没有nameservice，则尝试从configMap中获取
            if (nameservice == null && enableHA) {
                Object nameserviceObj = configMap.get("dfs.nameservices");
                if (nameserviceObj != null) {
                    nameservice = nameserviceObj.toString();
                    log.info("从configMap中获取nameservice: {}", nameservice);
                }
            }

            if (enableHA && nameservice != null) {
                hdfsUri = "hdfs://" + nameservice;
            }

            // 12. WebHDFS URI（但不在界面显示）

            // 13. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("HDFS URI", hdfsUri);
            if (enableHA) {
                basicInfo.put("NameNode主节点", nn1);
                basicInfo.put("NameNode从节点", nn2);
                basicInfo.put("Nameservice", nameservice);
            } else {
                basicInfo.put("NameNode节点", nn1);
            }
            basicInfo.put("启用Kerberos", enableKerberos ? "是" : "否");

            // 14. 构建有序的基本连接信息列表
            List<Map<String, String>> basicInfoList = new ArrayList<>();

            // HDFS URI
            Map<String, String> uriItem = new HashMap<>();
            uriItem.put("label", "HDFS URI");
            uriItem.put("value", hdfsUri);
            basicInfoList.add(uriItem);

            // NameNode节点信息
            if (enableHA) {
                Map<String, String> nn1Item = new HashMap<>();
                nn1Item.put("label", "NameNode主节点");
                nn1Item.put("value", nn1);
                basicInfoList.add(nn1Item);

                Map<String, String> nn2Item = new HashMap<>();
                nn2Item.put("label", "NameNode从节点");
                nn2Item.put("value", nn2);
                basicInfoList.add(nn2Item);

                Map<String, String> nsItem = new HashMap<>();
                nsItem.put("label", "Nameservice");
                nsItem.put("value", nameservice);
                basicInfoList.add(nsItem);
            } else {
                Map<String, String> nnItem = new HashMap<>();
                nnItem.put("label", "NameNode节点");
                nnItem.put("value", nn1);
                basicInfoList.add(nnItem);
            }

            // Kerberos状态
            Map<String, String> kerberosItem = new HashMap<>();
            kerberosItem.put("label", "启用Kerberos");
            kerberosItem.put("value", enableKerberos ? "是" : "否");
            basicInfoList.add(kerberosItem);

            // 15. 生成Java代码示例
            String javaCode = generateJavaCode(hdfsUri, enableKerberos);

            // 16. 生成Python代码示例
            String pythonCode = generatePythonCode(hdfsUri, enableKerberos);

            // 17. 获取HADOOP_HOME环境变量
            String hadoopHome = globalVariables.get("${HADOOP_HOME}");

            // 18. 生成命令行示例
            List<CommandLineItem> commandLines = generateCommandLines(hadoopHome, nn1);

            // 19. 返回构建好的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(javaCode)
                    .pythonCode(pythonCode)
                    .commandLines(commandLines)
                    .serviceHome(hadoopHome)
                    .hostName(nn1)
                    .build();
        } catch (Exception e) {
            log.error("获取HDFS连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java代码示例
     *
     * @param hdfsUri        HDFS URI
     * @param enableKerberos 是否启用Kerberos
     * @return Java代码示例
     */
    private String generateJavaCode(String hdfsUri, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("// HDFS Java API 示例代码\n")
                .append("import org.apache.hadoop.conf.Configuration;\n")
                .append("import org.apache.hadoop.fs.FileSystem;\n")
                .append("import org.apache.hadoop.fs.Path;\n")
                .append("import org.apache.hadoop.fs.FSDataInputStream;\n")
                .append("import org.apache.hadoop.fs.FSDataOutputStream;\n\n")
                .append("import java.io.BufferedReader;\n")
                .append("import java.io.InputStreamReader;\n")
                .append("import java.nio.charset.StandardCharsets;\n\n")
                .append("public class HDFSExample {\n")
                .append("    public static void main(String[] args) {\n")
                .append("        try {\n")
                .append("            // 创建配置对象\n")
                .append("            Configuration conf = new Configuration();\n")
                .append("            conf.set(\"fs.defaultFS\", \"").append(hdfsUri).append("\");\n");

        // 添加Kerberos配置（如果启用）
        if (enableKerberos) {
            code.append("\n            // Kerberos 认证配置\n")
                    .append("            conf.set(\"hadoop.security.authentication\", \"kerberos\");\n")
                    .append("            conf.set(\"hadoop.security.authorization\", \"true\");\n")
                    .append("            // 需要根据实际情况配置以下参数\n")
                    .append("            // conf.set(\"dfs.namenode.kerberos.principal\", \"hdfs/_HOST@HADOOP.COM\");\n")
                    .append("            // conf.set(\"dfs.datanode.kerberos.principal\", \"hdfs/_HOST@HADOOP.COM\");\n")
                    .append("            org.apache.hadoop.security.UserGroupInformation.setConfiguration(conf);\n")
                    .append("            org.apache.hadoop.security.UserGroupInformation.loginUserFromKeytab(\"hdfs@HADOOP.COM\", \"/path/to/hdfs.keytab\");\n");
        }

        code.append("\n            // 获取文件系统对象\n")
                .append("            FileSystem fs = FileSystem.get(conf);\n\n")
                .append("            // 创建目录\n")
                .append("            Path dirPath = new Path(\"/user/example\");\n")
                .append("            if (!fs.exists(dirPath)) {\n")
                .append("                fs.mkdirs(dirPath);\n")
                .append("                System.out.println(\"目录创建成功: \" + dirPath);\n")
                .append("            }\n\n")
                .append("            // 创建文件并写入数据\n")
                .append("            Path filePath = new Path(\"/user/example/test.txt\");\n")
                .append("            FSDataOutputStream outputStream = fs.create(filePath);\n")
                .append("            outputStream.writeBytes(\"Hello, HDFS!\\n\");\n")
                .append("            outputStream.close();\n")
                .append("            System.out.println(\"文件创建成功: \" + filePath);\n\n")
                .append("            // 读取文件\n")
                .append("            FSDataInputStream inputStream = fs.open(filePath);\n")
                .append("            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));\n")
                .append("            String line;\n")
                .append("            System.out.println(\"文件内容:\");\n")
                .append("            while ((line = reader.readLine()) != null) {\n")
                .append("                System.out.println(line);\n")
                .append("            }\n")
                .append("            reader.close();\n")
                .append("            inputStream.close();\n\n")
                .append("            // 列出目录内容\n")
                .append("            System.out.println(\"目录内容:\");\n")
                .append("            for (org.apache.hadoop.fs.FileStatus status : fs.listStatus(dirPath)) {\n")
                .append("                System.out.println(status.getPath());\n")
                .append("            }\n\n")
                .append("            // 删除文件\n")
                .append("            fs.delete(filePath, false);\n")
                .append("            System.out.println(\"文件删除成功: \" + filePath);\n\n")
                .append("            // 关闭文件系统\n")
                .append("            fs.close();\n\n")
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
     * @param hdfsUri        HDFS URI
     * @param enableKerberos 是否启用Kerberos
     * @return Python代码示例
     */
    private String generatePythonCode(String hdfsUri, boolean enableKerberos) {
        StringBuilder code = new StringBuilder();
        code.append("# HDFS Python API 示例代码\n")
                .append("# 需要安装 pyarrow 或 hdfs 包\n")
                .append("# pip install pyarrow\n")
                .append("# 或者\n")
                .append("# pip install hdfs\n\n");

        // PyArrow 示例
        code.append("# 使用 PyArrow 操作 HDFS\n")
                .append("import pyarrow as pa\n")
                .append("import pyarrow.fs as fs\n\n")
                .append("# 创建 HDFS 连接\n")
                .append("hdfs = fs.HadoopFileSystem(\n")
                .append("    host=\"").append(hdfsUri.replace("hdfs://", "").split(":")[0]).append("\",\n")
                .append("    port=").append(hdfsUri.split(":").length > 2 ? hdfsUri.split(":")[2] : "8020")
                .append(",\n");

        if (enableKerberos) {
            code.append("    # Kerberos 认证配置\n")
                    .append("    kerb_ticket=None  # 使用当前登录的Kerberos票据\n");
        }

        code.append(")\n\n")
                .append("# 创建目录\n")
                .append("hdfs.create_dir('/user/example')\n")
                .append("print(\"目录创建成功: /user/example\")\n\n")
                .append("# 写入文件\n")
                .append("with hdfs.open_output_stream('/user/example/test.txt') as writer:\n")
                .append("    writer.write(b'Hello, HDFS!\\n')\n")
                .append("print(\"文件创建成功: /user/example/test.txt\")\n\n")
                .append("# 读取文件\n")
                .append("with hdfs.open_input_stream('/user/example/test.txt') as reader:\n")
                .append("    content = reader.read()\n")
                .append("    print(\"文件内容:\")\n")
                .append("    print(content.decode('utf-8'))\n\n")
                .append("# 列出目录内容\n")
                .append("file_info = hdfs.get_file_info(fs.FileSelector('/user/example'))\n")
                .append("print(\"目录内容:\")\n")
                .append("for info in file_info:\n")
                .append("    print(info.path)\n\n")
                .append("# 删除文件\n")
                .append("hdfs.delete_file('/user/example/test.txt')\n")
                .append("print(\"文件删除成功: /user/example/test.txt\")\n\n");

        // HDFS 包示例
        code.append("# 使用 hdfs 包操作 HDFS\n")
                .append("from hdfs import InsecureClient\n\n")
                .append("# 创建 HDFS 客户端\n");

        if (enableKerberos) {
            code.append("# 注意：hdfs 包不直接支持 Kerberos，建议使用 PyArrow\n")
                    .append("# 或者使用 kinit 命令先获取 Kerberos 票据\n");
        }

        code.append("client = InsecureClient('").append(hdfsUri).append("', user='hdfs')\n\n")
                .append("# 创建目录\n")
                .append("client.makedirs('/user/example')\n")
                .append("print(\"目录创建成功: /user/example\")\n\n")
                .append("# 写入文件\n")
                .append("client.write('/user/example/test.txt', data='Hello, HDFS!\\n')\n")
                .append("print(\"文件创建成功: /user/example/test.txt\")\n\n")
                .append("# 读取文件\n")
                .append("with client.read('/user/example/test.txt') as reader:\n")
                .append("    content = reader.read()\n")
                .append("    print(\"文件内容:\")\n")
                .append("    print(content.decode('utf-8'))\n\n")
                .append("# 列出目录内容\n")
                .append("files = client.list('/user/example')\n")
                .append("print(\"目录内容:\")\n")
                .append("for file in files:\n")
                .append("    print(file)\n\n")
                .append("# 删除文件\n")
                .append("client.delete('/user/example/test.txt')\n")
                .append("print(\"文件删除成功: /user/example/test.txt\")\n");

        return code.toString();
    }

    /**
     * 生成命令行示例
     *
     * @param hadoopHome     Hadoop安装目录
     * @param hostName       主机名
     * @return 命令行示例列表
     */
    private List<CommandLineItem> generateCommandLines(String hadoopHome,
                                                       String hostName) {
        List<CommandLineItem> commandLines = new ArrayList<>();

        // 命令提示符
        String serviceName = hadoopHome != null ? hadoopHome.substring(hadoopHome.lastIndexOf('/') + 1) : "hadoop";
        String shellPrompt = "[root@" + hostName + " " + serviceName + "]# ";

        // Kerberos参数

        // 1. 查看HDFS状态
        CommandLineItem statusCmd = CommandLineItem.builder()
                .label("查看HDFS状态")
                .value("bin/hdfs dfsadmin -report")
                .commandPrompt(shellPrompt)
                .commandResult("Configured Capacity: 200 GB\n" +
                        "Present Capacity: 180 GB\n" +
                        "DFS Remaining: 160 GB\n" +
                        "DFS Used: 20 GB\n" +
                        "DFS Used%: 10%\n" +
                        "Under replicated blocks: 0\n" +
                        "Blocks with corrupt replicas: 0\n" +
                        "Missing blocks: 0\n" +
                        "Missing blocks (with replication factor 1): 0\n" +
                        "...\n" +
                        "Live datanodes (3):\n" +
                        "...\n")
                .build();
        commandLines.add(statusCmd);

        // 2. 列出HDFS目录
        CommandLineItem lsCmd = CommandLineItem.builder()
                .label("列出HDFS目录")
                .value("bin/hdfs dfs -ls /")
                .commandPrompt(shellPrompt)
                .commandResult("Found 4 items\n" +
                        "drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:20 /apps\n" +
                        "drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:21 /hbase\n" +
                        "drwxrwxrwx   - hdfs supergroup          0 2023-05-15 10:22 /tmp\n" +
                        "drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:23 /user")
                .build();
        commandLines.add(lsCmd);

        // 3. 创建HDFS目录
        CommandLineItem mkdirCmd = CommandLineItem.builder()
                .label("创建HDFS目录")
                .value("bin/hdfs dfs -mkdir -p /user/example")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(mkdirCmd);

        // 4. 上传文件到HDFS
        CommandLineItem putCmd = CommandLineItem.builder()
                .label("上传文件到HDFS")
                .value("bin/hdfs dfs -put /path/to/local/file.txt /user/example/")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(putCmd);

        // 5. 从HDFS下载文件
        CommandLineItem getCmd = CommandLineItem.builder()
                .label("从HDFS下载文件")
                .value("bin/hdfs dfs -get /user/example/file.txt /tmp/")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(getCmd);

        // 6. 查看HDFS文件内容
        CommandLineItem catCmd = CommandLineItem.builder()
                .label("查看HDFS文件内容")
                .value("bin/hdfs dfs -cat /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult("Hello, HDFS!\nThis is a test file.")
                .build();
        commandLines.add(catCmd);

        // 7. 设置HDFS文件权限
        CommandLineItem chmodCmd = CommandLineItem.builder()
                .label("设置HDFS文件权限")
                .value("bin/hdfs dfs -chmod 755 /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(chmodCmd);

        // 8. 设置HDFS文件所有者
        CommandLineItem chownCmd = CommandLineItem.builder()
                .label("设置HDFS文件所有者")
                .value("bin/hdfs dfs -chown user:group /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(chownCmd);

        // 9. 查看HDFS文件详情
        CommandLineItem statCmd = CommandLineItem.builder()
                .label("查看HDFS文件详情")
                .value("bin/hdfs dfs -stat /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult("2023-05-15 10:30:45\n" +
                        "16\n" +
                        "3")
                .build();
        commandLines.add(statCmd);

        // 10. 查看HDFS文件校验和
        CommandLineItem checksumCmd = CommandLineItem.builder()
                .label("查看HDFS文件校验和")
                .value("bin/hdfs dfs -checksum /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult(
                        "/user/example/file.txt\tMD5-of-0MD5-of-512CRC32C\t000002000000000000000000b9ade8ad22cc1f47b11bc9a5e89672f64")
                .build();
        commandLines.add(checksumCmd);

        // 11. 检查HDFS文件系统一致性
        CommandLineItem fsckCmd = CommandLineItem.builder()
                .label("检查HDFS文件系统一致性")
                .value("bin/hdfs fsck /")
                .commandPrompt(shellPrompt)
                .commandResult("Connecting to namenode via http://namenode:9870/fsck?ugi=hdfs&path=%2F\n" +
                        "FSCK started by hdfs (auth:KERBEROS_SSL) from /172.18.0.2 for path / at Wed May 15 10:35:31 UTC 2023\n"
                        +
                        "Status: HEALTHY\n" +
                        " Total size:\t12345678 B\n" +
                        " Total dirs:\t123\n" +
                        " Total files:\t456\n" +
                        " Total symlinks:\t0\n" +
                        " Total blocks (validated):\t789 (avg. block size 15646 B)\n" +
                        " Minimally replicated blocks:\t789 (100.0 %)\n" +
                        " Over-replicated blocks:\t0 (0.0 %)\n" +
                        " Under-replicated blocks:\t0 (0.0 %)\n" +
                        " Mis-replicated blocks:\t0 (0.0 %)\n" +
                        " Default replication factor:\t3\n" +
                        " Average block replication:\t3.0\n" +
                        " Corrupt blocks:\t0\n" +
                        " Missing replicas:\t0 (0.0 %)\n" +
                        " Number of data-nodes:\t3\n" +
                        " Number of racks:\t1\n" +
                        "FSCK ended at Wed May 15 10:35:35 UTC 2023 in 4 milliseconds\n" +
                        "\n" +
                        "The filesystem under path '/' is HEALTHY")
                .build();
        commandLines.add(fsckCmd);

        // 12. 查看HDFS配额
        CommandLineItem quotaCmd = CommandLineItem.builder()
                .label("查看HDFS配额")
                .value("bin/hdfs dfs -count -q /user")
                .commandPrompt(shellPrompt)
                .commandResult(
                        "        none        inf            0        inf            1            3           12 /user")
                .build();
        commandLines.add(quotaCmd);

        // 13. 设置HDFS目录配额
        CommandLineItem setQuotaCmd = CommandLineItem.builder()
                .label("设置HDFS目录配额")
                .value("bin/hdfs dfsadmin -setSpaceQuota 1g /user/example")
                .commandPrompt(shellPrompt)
                .commandResult("")
                .build();
        commandLines.add(setQuotaCmd);

        // 14. 删除HDFS文件
        CommandLineItem rmCmd = CommandLineItem.builder()
                .label("删除HDFS文件")
                .value("bin/hdfs dfs -rm /user/example/file.txt")
                .commandPrompt(shellPrompt)
                .commandResult("Deleted /user/example/file.txt")
                .build();
        commandLines.add(rmCmd);

        // 15. 删除HDFS目录
        CommandLineItem rmdirCmd = CommandLineItem.builder()
                .label("删除HDFS目录")
                .value("bin/hdfs dfs -rm -r /user/example")
                .commandPrompt(shellPrompt)
                .commandResult("Deleted /user/example")
                .build();
        commandLines.add(rmdirCmd);

        return addFinalPrompt(commandLines,hostName);
    }
}
