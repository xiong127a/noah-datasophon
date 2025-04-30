DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-client</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-yarn-client</artifactId>
    <version>3.3.3</version>
</dependency>
DEPENDENCIES_END

package com.example.yarn;

/*
 * YARN（ResourceManager）Java连接示例
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

public class YarnExample {

    public static void main(String[] args) {
        try {
            // 连接参数
            String rmAddress = "${data.getBasicInfoValue('connectString', 'localhost:8032')}";
            String webAddress = "${data.getBasicInfoValue('webAddress', 'http://localhost:8088')}";
            String deployMode = "${data.getBasicInfoValue('deployMode', '单节点模式')}";
            boolean enableHA = deployMode.contains("高可用");
            String schedulerType = "${data.getBasicInfoValue('schedulerType', 'Capacity Scheduler')}";
            
            // 安全认证配置
            boolean enableKerberos = "${data.getSecurityInfoValue('kerberos', '否')}".equals("是");
            String principal = "${data.getSecurityInfoValue('principal', '')}";
            String keytabPath = "${data.getSecurityInfoValue('keytab', '')}";

            System.out.println("YARN ResourceManager连接示例");
            System.out.println("ResourceManager地址: " + rmAddress);
            System.out.println("Web UI地址: " + webAddress);
            System.out.println("部署模式: " + deployMode);
            System.out.println("调度器类型: " + schedulerType);
            System.out.println("启用Kerberos: " + enableKerberos);
            
            // 根据部署模式选择连接方式
            if (enableHA) {
                System.out.println("\n----- 使用高可用模式连接 -----");
                haConnection(rmAddress, enableKerberos, principal, keytabPath);
            } else {
                System.out.println("\n----- 使用单节点模式连接 -----");
                singleNodeConnection(rmAddress, enableKerberos, principal, keytabPath);
            }
            
        } catch (Exception e) {
            System.err.println("YARN连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 单节点模式连接示例
     */
    private static void singleNodeConnection(String rmAddress, boolean enableKerberos, 
                                           String principal, String keytabPath) throws Exception {
        // 创建YARN配置
        Configuration conf = createConfiguration(rmAddress, false, enableKerberos,
                                                principal, keytabPath);
        
        // 在启用Kerberos的情况下创建安全连接
        if (enableKerberos) {
            performSecureOperation(conf, principal, keytabPath);
        } else {
            performYarnOperations(conf);
        }
    }
    
    /**
     * 高可用模式连接示例
     */
    private static void haConnection(String rmAddress, boolean enableKerberos,
                                   String principal, String keytabPath) throws Exception {
        // 创建YARN高可用配置
        Configuration conf = createConfiguration(rmAddress, true, enableKerberos, 
                                                principal, keytabPath);
        
        // 在启用Kerberos的情况下创建安全连接
        if (enableKerberos) {
            performSecureOperation(conf, principal, keytabPath);
        } else {
            performYarnOperations(conf);
        }
    }
    
    /**
     * 创建YARN配置
     */
    private static Configuration createConfiguration(String rmAddress, boolean isHA, 
                                                  boolean enableKerberos, String principal,
                                                  String keytabPath) {
        Configuration conf = new YarnConfiguration();
        
        // 设置ResourceManager地址
        if (isHA) {
            // HA配置
            conf.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);
            
            // 解析RM地址，假设格式为: rm1=host1:port,rm2=host2:port
            String[] rmNodes = rmAddress.split(",");
            if (rmNodes.length >= 2) {
                // 设置RM IDs
                String rmIds = Arrays.stream(rmNodes)
                    .map(node -> node.split("=")[0])
                    .reduce((a, b) -> a + "," + b)
                    .orElse("rm1,rm2");
                conf.set(YarnConfiguration.RM_HA_IDS, rmIds);
                
                // 设置RM地址
                for (String node : rmNodes) {
                    String[] parts = node.split("=");
                    if (parts.length == 2) {
                        String rmId = parts[0];
                        String address = parts[1];
                        conf.set(YarnConfiguration.RM_ADDRESS + "." + rmId, address);
                    }
                }
            } else {
                // 回退到非HA模式
                conf.set(YarnConfiguration.RM_ADDRESS, rmAddress);
            }
        } else {
            // 非HA模式配置
            conf.set(YarnConfiguration.RM_ADDRESS, rmAddress);
        }
        
        // Kerberos配置
        if (enableKerberos) {
            conf.set("hadoop.security.authentication", "kerberos");
            conf.set("hadoop.security.authorization", "true");
            if (principal != null && !principal.isEmpty()) {
                conf.set("yarn.resourcemanager.principal", principal);
            }
        }
        
        return conf;
    }
    
    /**
     * 在启用Kerberos的情况下执行安全操作
     */
    private static void performSecureOperation(Configuration conf, String principal, 
                                             String keytabPath) throws Exception {
        UserGroupInformation.setConfiguration(conf);
        
        if (principal != null && !principal.isEmpty() && keytabPath != null && !keytabPath.isEmpty()) {
            System.out.println("使用Kerberos认证: Principal=" + principal + ", Keytab=" + keytabPath);
            UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
            
            // 使用安全上下文执行操作
            UserGroupInformation.getLoginUser().doAs(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    performYarnOperations(conf);
                    return null;
                }
            });
        } else {
            System.out.println("Kerberos配置不完整，尝试使用当前用户执行");
            performYarnOperations(conf);
        }
    }
    
    /**
     * 执行YARN操作
     */
    private static void performYarnOperations(Configuration conf) throws IOException, YarnException {
        // 创建YARN客户端
        try (YarnClient yarnClient = YarnClient.createYarnClient()) {
            yarnClient.init(conf);
            yarnClient.start();
            
            System.out.println("\n===== YARN集群信息 =====");
            YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
            System.out.println("集群节点数: " + metrics.getNumNodeManagers());
            
            List<NodeReport> nodeReports = yarnClient.getNodeReports();
            System.out.println("\n----- 节点状态 -----");
            for (NodeReport report : nodeReports) {
                System.out.println("节点ID: " + report.getNodeId());
                System.out.println("  状态: " + report.getNodeState());
                System.out.println("  机架: " + report.getRackName());
                System.out.println("  内存总量: " + report.getCapability().getMemory() + " MB");
                System.out.println("  vCores数量: " + report.getCapability().getVirtualCores());
            }
            
            // 获取队列信息
            System.out.println("\n----- 队列信息 -----");
            QueueInfo rootQueue = yarnClient.getQueueInfo("root");
            printQueueInfo(rootQueue, "");
            
            // 获取应用列表
            System.out.println("\n----- 应用列表 -----");
            List<ApplicationReport> applications = yarnClient.getApplications();
            System.out.println("正在运行的应用数量: " + applications.size());
            
            int count = 0;
            for (ApplicationReport report : applications) {
                System.out.println("应用ID: " + report.getApplicationId());
                System.out.println("  名称: " + report.getName());
                System.out.println("  类型: " + report.getApplicationType());
                System.out.println("  状态: " + report.getYarnApplicationState());
                System.out.println("  用户: " + report.getUser());
                System.out.println("  跟踪URL: " + report.getTrackingUrl());
                System.out.println("  启动时间: " + new Date(report.getStartTime()));
                System.out.println();
                
                // 只显示前5个应用
                if (++count >= 5) {
                    System.out.println("... 省略其他应用 ...");
                    break;
                }
            }
            
            // 创建新应用示例（仅显示示例代码，不实际执行）
            showApplicationSubmissionExample();
        }
    }
    
    /**
     * 递归打印队列信息
     */
    private static void printQueueInfo(QueueInfo queue, String indent) {
        System.out.println(indent + "队列: " + queue.getQueueName());
        System.out.println(indent + "  状态: " + queue.getQueueState());
        System.out.println(indent + "  容量百分比: " + queue.getCapacity() * 100 + "%");
        System.out.println(indent + "  已使用容量: " + queue.getUsedCapacity() * 100 + "%");
        
        if (queue.getChildQueues() != null && !queue.getChildQueues().isEmpty()) {
            for (QueueInfo childQueue : queue.getChildQueues()) {
                printQueueInfo(childQueue, indent + "  ");
            }
        }
    }
    
    /**
     * 应用提交示例（仅展示代码，不实际执行）
     */
    private static void showApplicationSubmissionExample() {
        System.out.println("\n===== 应用提交示例代码 =====");
        System.out.println("// 创建应用提交上下文");
        System.out.println("YarnClientApplication app = yarnClient.createApplication();");
        System.out.println("ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();");
        System.out.println("appContext.setApplicationName(\"示例应用\");");
        System.out.println();
        
        System.out.println("// 设置容器规格");
        System.out.println("Resource resource = Resource.newInstance(1024, 1);");
        System.out.println("ContainerLaunchContext container = Records.newRecord(ContainerLaunchContext.class);");
        System.out.println("container.setCommands(Collections.singletonList(\"echo 'Hello YARN' && sleep 300\"));");
        System.out.println();
        
        System.out.println("// 设置应用主容器");
        System.out.println("appContext.setResource(resource);");
        System.out.println("appContext.setAMContainerSpec(container);");
        System.out.println();
        
        System.out.println("// 提交应用");
        System.out.println("ApplicationId appId = appContext.getApplicationId();");
        System.out.println("System.out.println(\"提交应用: \" + appId);");
        System.out.println("yarnClient.submitApplication(appContext);");
    }
} 