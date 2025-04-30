DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-client</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-hdfs</artifactId>
    <version>3.3.3</version>
</dependency>
DEPENDENCIES_END

/**
 * HDFS Java API 示例代码
 * 
 * 本示例展示如何使用Java API操作HDFS文件系统
 * 
 * 连接信息:
 * HDFS URI: ${data.getConnectInfoValue('hdfsUri', 'hdfs://localhost:8020')}
 * 主机: ${data.getBasicInfoValue('host', 'localhost')}
 * 端口: ${data.getBasicInfoValue('port', '8020')}
 * <#if data.getBasicInfoValue('highAvailability', 'false') == 'true'>HA模式: 已启用<#if data.getBasicInfoValue('nameservice', '') != ''>, Nameservice: ${data.getBasicInfoValue('nameservice', '')}</#if></#if>
 * <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>Kerberos认证: 已启用</#if>
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class HDFSExample {
    
    // HDFS URI配置
    private static final String HDFS_URI = "${data.getConnectInfoValue('hdfsUri', 'hdfs://localhost:8020')}";
    
    // 测试路径
    private static final String TEST_DIR = "/user/example";
    private static final String TEST_FILE = TEST_DIR + "/test.txt";
    
    public static void main(String[] args) {
        try {
            // 创建HDFS配置
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", HDFS_URI);
            
            <#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
            // Kerberos认证配置
            System.out.println("配置Kerberos认证...");
            conf.set("hadoop.security.authentication", "kerberos");
            conf.set("hadoop.security.authorization", "true");
            
            // 根据实际情况配置以下参数
            conf.set("dfs.namenode.kerberos.principal", "${data.getSecurityInfoValue('principal', 'hdfs/_HOST@EXAMPLE.COM')}");
            
            // 设置Kerberos配置
            org.apache.hadoop.security.UserGroupInformation.setConfiguration(conf);
            
            // 使用keytab登录
            // 注意: 请替换为您的keytab文件路径和主体
            org.apache.hadoop.security.UserGroupInformation.loginUserFromKeytab(
                "${data.getSecurityInfoValue('principal', 'hdfs@EXAMPLE.COM')}", 
                "${data.getSecurityInfoValue('keytab.path', '/etc/security/keytabs/hdfs.keytab')}"
            );
            System.out.println("Kerberos认证配置完成");
            </#if>
            
            // 获取文件系统实例
            System.out.println("连接到HDFS: " + HDFS_URI);
            FileSystem fs = FileSystem.get(conf);
            
            // 执行HDFS操作
            createDirectory(fs);
            writeFile(fs);
            readFile(fs);
            getFileInfo(fs);
            listDirectory(fs);
            deleteFile(fs);
            deleteDirectory(fs);
            
            // 关闭文件系统
            fs.close();
            System.out.println("HDFS操作示例完成");
            
        } catch (Exception e) {
            System.err.println("HDFS操作出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建目录
     */
    private static void createDirectory(FileSystem fs) throws IOException {
        Path dir = new Path(TEST_DIR);
        
        // 检查目录是否已存在
        if (fs.exists(dir)) {
            System.out.println("目录已存在: " + TEST_DIR);
            return;
        }
        
        // 创建目录
        boolean success = fs.mkdirs(dir);
        if (success) {
            System.out.println("目录创建成功: " + TEST_DIR);
        } else {
            System.err.println("目录创建失败: " + TEST_DIR);
        }
    }
    
    /**
     * 写入文件
     */
    private static void writeFile(FileSystem fs) throws IOException {
        Path file = new Path(TEST_FILE);
        
        // 使用输出流写入数据
        try (FSDataOutputStream outputStream = fs.create(file, true)) {
            String content = "Hello, HDFS!\n这是一个测试文件。\n" + 
                            "当前时间: " + new java.util.Date();
            outputStream.writeBytes(content);
            System.out.println("文件写入成功: " + TEST_FILE);
        }
    }
    
    /**
     * 读取文件
     */
    private static void readFile(FileSystem fs) throws IOException {
        Path file = new Path(TEST_FILE);
        
        // 检查文件是否存在
        if (!fs.exists(file)) {
            System.err.println("文件不存在: " + TEST_FILE);
            return;
        }
        
        // 使用输入流读取数据
        try (FSDataInputStream inputStream = fs.open(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            System.out.println("文件内容 (" + TEST_FILE + "):");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
    
    /**
     * 获取文件信息
     */
    private static void getFileInfo(FileSystem fs) throws IOException {
        Path file = new Path(TEST_FILE);
        
        // 检查文件是否存在
        if (!fs.exists(file)) {
            System.err.println("文件不存在: " + TEST_FILE);
            return;
        }
        
        // 获取文件状态
        FileStatus status = fs.getFileStatus(file);
        
        System.out.println("文件信息:");
        System.out.println("  路径: " + status.getPath());
        System.out.println("  大小: " + status.getLen() + " 字节");
        System.out.println("  修改时间: " + new java.util.Date(status.getModificationTime()));
        System.out.println("  权限: " + status.getPermission());
        System.out.println("  所有者: " + status.getOwner());
        System.out.println("  组: " + status.getGroup());
        System.out.println("  复制因子: " + status.getReplication());
        System.out.println("  块大小: " + status.getBlockSize() + " 字节");
    }
    
    /**
     * 列出目录内容
     */
    private static void listDirectory(FileSystem fs) throws IOException {
        Path dir = new Path(TEST_DIR);
        
        // 检查目录是否存在
        if (!fs.exists(dir)) {
            System.err.println("目录不存在: " + TEST_DIR);
            return;
        }
        
        // 获取目录内容
        FileStatus[] statuses = fs.listStatus(dir);
        
        System.out.println("目录内容 (" + TEST_DIR + "):");
        for (FileStatus status : statuses) {
            String type = status.isDirectory() ? "目录" : "文件";
            System.out.println("  " + type + ": " + status.getPath().getName() + 
                              " (" + status.getLen() + " 字节)");
        }
    }
    
    /**
     * 删除文件
     */
    private static void deleteFile(FileSystem fs) throws IOException {
        Path file = new Path(TEST_FILE);
        
        // 检查文件是否存在
        if (!fs.exists(file)) {
            System.err.println("文件不存在: " + TEST_FILE);
            return;
        }
        
        // 删除文件
        boolean success = fs.delete(file, false);
        if (success) {
            System.out.println("文件删除成功: " + TEST_FILE);
        } else {
            System.err.println("文件删除失败: " + TEST_FILE);
        }
    }
    
    /**
     * 删除目录
     */
    private static void deleteDirectory(FileSystem fs) throws IOException {
        Path dir = new Path(TEST_DIR);
        
        // 检查目录是否存在
        if (!fs.exists(dir)) {
            System.err.println("目录不存在: " + TEST_DIR);
            return;
        }
        
        // 删除目录（递归删除）
        boolean success = fs.delete(dir, true);
        if (success) {
            System.out.println("目录删除成功: " + TEST_DIR);
        } else {
            System.err.println("目录删除失败: " + TEST_DIR);
        }
    }
    
    /**
     * 高级示例: 使用追加模式写入文件
     */
    private static void appendToFile(FileSystem fs, String content) throws IOException {
        Path file = new Path(TEST_FILE);
        
        // 检查文件是否存在
        if (!fs.exists(file)) {
            System.err.println("文件不存在，无法追加: " + TEST_FILE);
            return;
        }
        
        // 使用追加模式写入数据
        try (FSDataOutputStream outputStream = fs.append(file)) {
            outputStream.writeBytes(content);
            System.out.println("内容追加成功: " + TEST_FILE);
        }
    }
    
    /**
     * 高级示例: 复制文件
     */
    private static void copyFile(FileSystem fs, String sourcePath, String destPath) throws IOException {
        Path src = new Path(sourcePath);
        Path dst = new Path(destPath);
        
        // 检查源文件是否存在
        if (!fs.exists(src)) {
            System.err.println("源文件不存在: " + sourcePath);
            return;
        }
        
        // 复制文件
        org.apache.hadoop.fs.FileUtil.copy(fs, src, fs, dst, false, new Configuration());
        System.out.println("文件复制成功: " + sourcePath + " -> " + destPath);
    }
    
    // 更多高级操作示例...
} 