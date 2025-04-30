// ZooKeeper Java 客户端示例
// 此示例展示如何使用ZooKeeper Java API连接ZooKeeper服务器

DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.5.10</version>
</dependency>
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-auth</artifactId>
    <version>3.3.3</version>
</dependency>
</#if>
DEPENDENCIES_END

package com.example.zookeeper;

/*
 * ZooKeeper Java连接示例
 */

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.client.ZKClientConfig;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperExample {

    public static void main(String[] args) {
        // 连接参数
        String connectString = "${data.getBasicInfoValue('connectString', 'localhost:2181')}";
        String deployMode = "${data.getBasicInfoValue('deployMode', '单节点模式')}";
        int sessionTimeout = 30000; // 会话超时时间（毫秒）
        
        // 安全认证配置
        boolean enableKerberos = "${data.getSecurityInfoValue('kerberos', '否')}" == "是";
        
        System.out.println("ZooKeeper连接字符串: " + connectString);
        System.out.println("部署模式: " + deployMode);
        
        try {
            // 创建ZooKeeper连接示例
            System.out.println("\n----- 创建ZooKeeper连接 -----");
            
            // 创建连接
            ZooKeeper zooKeeper = createConnection(connectString, sessionTimeout, enableKerberos);
            
            if (zooKeeper != null) {
                // 基本操作示例
                basicOperationsExample(zooKeeper);
                
                // 监听器示例
                watcherExample(zooKeeper);
                
                // 节点操作示例
                nodeOperationsExample(zooKeeper);
                
                // 关闭连接
                zooKeeper.close();
                System.out.println("ZooKeeper连接已关闭");
            }
            
        } catch (Exception e) {
            System.err.println("ZooKeeper操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建ZooKeeper连接
     */
    private static ZooKeeper createConnection(String connectString, int sessionTimeout, boolean enableKerberos) 
            throws IOException, InterruptedException {
        
        System.out.println("连接到ZooKeeper服务器...");
        
        // 连接完成信号量
        final CountDownLatch connectedSignal = new CountDownLatch(1);
        
        // 设置连接配置
        ZKClientConfig clientConfig = new ZKClientConfig();
        
        // 如果启用Kerberos，设置相关配置
        if (enableKerberos) {
            String principal = "${data.getSecurityInfoValue('principal', 'zookeeper/_HOST@EXAMPLE.COM')}";
            String keytab = "${data.getSecurityInfoValue('keytab', '/etc/security/keytabs/zookeeper.keytab')}";
            
            System.out.println("启用Kerberos认证 - 主体: " + principal);
            
            // 设置JAAS配置
            System.setProperty("java.security.auth.login.config", "jaas.conf");
            // 设置Kerberos配置
            System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");
            // 设置ZooKeeper安全机制
            clientConfig.setProperty("zookeeper.sasl.clientconfig", "Client");
        }
        
        // 创建ZooKeeper实例
        ZooKeeper zooKeeper = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("连接成功，会话已建立");
                    connectedSignal.countDown();
                }
            }
        }, clientConfig);
        
        // 等待连接建立
        if (!connectedSignal.await(10, TimeUnit.SECONDS)) {
            System.err.println("连接超时，请检查ZooKeeper服务器是否正常运行");
            zooKeeper.close();
            return null;
        }
        
        System.out.println("ZooKeeper连接成功，会话ID: " + zooKeeper.getSessionId());
        return zooKeeper;
    }
    
    /**
     * 基本操作示例
     */
    private static void basicOperationsExample(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        System.out.println("\n===== 基本操作示例 =====");
        
        // 查看ZooKeeper状态
        System.out.println("ZooKeeper状态: " + zooKeeper.getState());
        
        // 获取根节点下的所有子节点
        List<String> rootNodes = zooKeeper.getChildren("/", false);
        System.out.println("根节点下的子节点: " + rootNodes);
        
        // 创建测试目录
        String testPath = "/java_example";
        try {
            Stat existStat = zooKeeper.exists(testPath, false);
            if (existStat == null) {
                String createdPath = zooKeeper.create(testPath, "测试数据".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                System.out.println("创建节点: " + createdPath);
            } else {
                System.out.println("节点已存在: " + testPath);
            }
        } catch (Exception e) {
            System.out.println("创建节点失败: " + e.getMessage());
        }
        
        // 读取节点数据
        try {
            byte[] data = zooKeeper.getData(testPath, false, null);
            System.out.println("节点" + testPath + "数据: " + new String(data));
        } catch (Exception e) {
            System.out.println("读取节点数据失败: " + e.getMessage());
        }
        
        // 获取节点状态
        try {
            Stat stat = zooKeeper.exists(testPath, false);
            if (stat != null) {
                System.out.println("节点状态:");
                System.out.println("  创建时间: " + stat.getCtime());
                System.out.println("  修改时间: " + stat.getMtime());
                System.out.println("  数据版本: " + stat.getVersion());
                System.out.println("  子节点版本: " + stat.getCversion());
            }
        } catch (Exception e) {
            System.out.println("获取节点状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 监听器示例
     */
    private static void watcherExample(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        System.out.println("\n===== 监听器示例 =====");
        
        String watchPath = "/java_example";
        
        // 检查路径是否存在
        if (zooKeeper.exists(watchPath, false) == null) {
            System.out.println("节点不存在，创建节点: " + watchPath);
            zooKeeper.create(watchPath, "原始数据".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        
        // 设置监听器
        System.out.println("设置监听器监控数据变化 (演示用，实际应用需要在单独的线程中运行)");
        
        // 创建数据监听器
        Watcher dataWatcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeDataChanged) {
                    System.out.println("检测到数据变化: " + event.getPath());
                    try {
                        // 读取新数据并再次设置监听器
                        byte[] newData = zooKeeper.getData(event.getPath(), this, null);
                        System.out.println("新数据: " + new String(newData));
                    } catch (Exception e) {
                        System.out.println("读取变化数据失败: " + e.getMessage());
                    }
                }
            }
        };
        
        // 设置监听器
        zooKeeper.getData(watchPath, dataWatcher, null);
        
        // 更新数据以触发监听器
        System.out.println("更新数据以触发监听器");
        zooKeeper.setData(watchPath, "更新后的数据".getBytes(), -1);
        
        // 给监听器一些时间来处理事件
        Thread.sleep(1000);
    }
    
    /**
     * 节点操作示例
     */
    private static void nodeOperationsExample(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        System.out.println("\n===== 节点操作示例 =====");
        
        // 创建临时节点
        String tempPath = "/java_example/temp";
        try {
            if (zooKeeper.exists("/java_example", false) == null) {
                zooKeeper.create("/java_example", "父节点".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            
            if (zooKeeper.exists(tempPath, false) != null) {
                zooKeeper.delete(tempPath, -1);
            }
            
            String createdPath = zooKeeper.create(tempPath, "临时节点数据".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            System.out.println("创建临时节点: " + createdPath);
        } catch (Exception e) {
            System.out.println("操作临时节点失败: " + e.getMessage());
        }
        
        // 创建顺序节点
        String seqPath = "/java_example/seq";
        try {
            String createdPath = zooKeeper.create(seqPath, "顺序节点数据".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            System.out.println("创建顺序节点: " + createdPath);
        } catch (Exception e) {
            System.out.println("创建顺序节点失败: " + e.getMessage());
        }
        
        // 列出子节点
        try {
            List<String> children = zooKeeper.getChildren("/java_example", false);
            System.out.println("/java_example 的子节点: " + children);
        } catch (Exception e) {
            System.out.println("列出子节点失败: " + e.getMessage());
        }
        
        // 清理示例节点
        System.out.println("\n----- 清理示例节点 -----");
        try {
            List<String> children = zooKeeper.getChildren("/java_example", false);
            for (String child : children) {
                zooKeeper.delete("/java_example/" + child, -1);
                System.out.println("删除节点: /java_example/" + child);
            }
            zooKeeper.delete("/java_example", -1);
            System.out.println("删除节点: /java_example");
        } catch (Exception e) {
            System.out.println("清理节点失败: " + e.getMessage());
        }
    }
} 