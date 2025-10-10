DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.apache.hbase</groupId>
    <artifactId>hbase-client</artifactId>
    <version>2.4.12</version>
</dependency>
<dependency>
    <groupId>org.apache.hbase</groupId>
    <artifactId>hbase-common</artifactId>
    <version>2.4.12</version>
</dependency>
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-auth</artifactId>
    <version>3.3.4</version>
</dependency>
</#if>
DEPENDENCIES_END

package com.example.hbase;

/*
 * HBase Java连接示例
 * 演示如何使用Java API连接HBase并执行基本操作
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
import org.apache.hadoop.security.UserGroupInformation;
</#if>

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

public class HBaseExample {

    // 表名常量
    private static final String TABLE_NAME = "test_table";
    private static final String COLUMN_FAMILY_1 = "cf1";
    private static final String COLUMN_FAMILY_2 = "cf2";

    public static void main(String[] args) {
        System.out.println("===== HBase Java客户端示例 =====");

        // 连接参数
        String zkQuorum = "${data.getBasicInfoValue('zkQuorum', 'localhost')}";
        String zkPort = "${data.getBasicInfoValue('zkPort', '2181')}";
        String zkRootNode = "${data.getBasicInfoValue('zkRootNode', '/hbase')}";
        
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
        // Kerberos配置
        try {
            System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");
            Configuration securityConf = new Configuration();
            securityConf.set("hadoop.security.authentication", "kerberos");
            
            // 设置Kerberos认证
            UserGroupInformation.setConfiguration(securityConf);
            
            // 使用keytab登录（取消注释并替换为实际的主体和keytab文件路径）
            // UserGroupInformation.loginUserFromKeytab("user@EXAMPLE.COM", "/path/to/user.keytab");
            
            System.out.println("Kerberos认证已配置");
        } catch (Exception e) {
            System.err.println("Kerberos认证配置失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
</#if>

        // 创建HBase配置
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", zkQuorum);
        config.set("hbase.zookeeper.property.clientPort", zkPort);
        config.set("zookeeper.znode.parent", zkRootNode);
<#if data.getSecurityInfoValue('kerberos', '否') == '是'>
        config.set("hbase.security.authentication", "kerberos");
        config.set("hbase.master.kerberos.principal", "${data.getSecurityInfoValue('masterPrincipal', 'hbase/_HOST@EXAMPLE.COM')}");
        config.set("hbase.regionserver.kerberos.principal", "${data.getSecurityInfoValue('regionServerPrincipal', 'hbase/_HOST@EXAMPLE.COM')}");
</#if>

        try (Connection connection = ConnectionFactory.createConnection(config)) {
            System.out.println("成功连接到HBase");
            
            // 获取Admin对象
            try (Admin admin = connection.getAdmin()) {
                // 列出已有的表
                listTables(admin);
                
                // 表操作示例
                tableOperationsExample(admin, connection);
                
                // 数据操作示例
                dataOperationsExample(connection);
                
                // 高级功能示例
                advancedFeaturesExample(connection);
            }
            
        } catch (IOException e) {
            System.err.println("HBase操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 列出HBase中的所有表
     */
    private static void listTables(Admin admin) throws IOException {
        System.out.println("\n----- 列出HBase中的表 -----");
        TableName[] tableNames = admin.listTableNames();
        if (tableNames.length == 0) {
            System.out.println("HBase中没有表");
        } else {
            System.out.println("HBase中的表：");
            for (TableName tableName : tableNames) {
                System.out.println("  - " + tableName.getNameAsString());
            }
        }
    }
    
    /**
     * 表操作示例
     */
    private static void tableOperationsExample(Admin admin, Connection connection) throws IOException {
        System.out.println("\n----- 表操作示例 -----");
        
        // 检查表是否存在
        TableName tableName = TableName.valueOf(TABLE_NAME);
        boolean tableExists = admin.tableExists(tableName);
        System.out.println("表 " + TABLE_NAME + " 是否存在: " + tableExists);
        
        // 如果表存在，先禁用并删除
        if (tableExists) {
            System.out.println("禁用并删除已存在的表: " + TABLE_NAME);
            if (!admin.isTableDisabled(tableName)) {
                admin.disableTable(tableName);
            }
            admin.deleteTable(tableName);
            System.out.println("表 " + TABLE_NAME + " 已删除");
        }
        
        // 创建新表
        System.out.println("创建新表: " + TABLE_NAME);
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        
        // 添加列族
        HColumnDescriptor cf1 = new HColumnDescriptor(COLUMN_FAMILY_1);
        cf1.setMaxVersions(3);  // 设置保留3个版本
        tableDescriptor.addFamily(cf1);
        
        HColumnDescriptor cf2 = new HColumnDescriptor(COLUMN_FAMILY_2);
        cf2.setTimeToLive(86400);  // TTL 1天 (秒)
        tableDescriptor.addFamily(cf2);
        
        // 创建表
        admin.createTable(tableDescriptor);
        System.out.println("表 " + TABLE_NAME + " 创建成功");
        
        // 获取表描述信息
        HTableDescriptor descriptor = admin.getTableDescriptor(tableName);
        System.out.println("表 " + TABLE_NAME + " 描述信息:");
        System.out.println("  - 列族数量: " + descriptor.getColumnFamilies().length);
        for (HColumnDescriptor columnFamily : descriptor.getColumnFamilies()) {
            System.out.println("  - 列族: " + columnFamily.getNameAsString());
            System.out.println("    - 最大版本数: " + columnFamily.getMaxVersions());
            System.out.println("    - TTL: " + columnFamily.getTimeToLive() + " 秒");
        }
    }
    
    /**
     * 数据操作示例
     */
    private static void dataOperationsExample(Connection connection) throws IOException {
        System.out.println("\n----- 数据操作示例 -----");
        
        // 获取表对象
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        
        try {
            // 写入数据示例
            System.out.println("写入数据...");
            
            List<Put> puts = new ArrayList<>();
            
            // 用户1数据
            Put put1 = new Put(Bytes.toBytes("row1"));
            put1.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("name"), Bytes.toBytes("张三"));
            put1.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("age"), Bytes.toBytes("30"));
            put1.addColumn(Bytes.toBytes(COLUMN_FAMILY_2), Bytes.toBytes("email"), Bytes.toBytes("zhangsan@example.com"));
            puts.add(put1);
            
            // 用户2数据
            Put put2 = new Put(Bytes.toBytes("row2"));
            put2.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("name"), Bytes.toBytes("李四"));
            put2.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("age"), Bytes.toBytes("25"));
            put2.addColumn(Bytes.toBytes(COLUMN_FAMILY_2), Bytes.toBytes("email"), Bytes.toBytes("lisi@example.com"));
            puts.add(put2);
            
            // 用户3数据
            Put put3 = new Put(Bytes.toBytes("row3"));
            put3.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("name"), Bytes.toBytes("王五"));
            put3.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("age"), Bytes.toBytes("35"));
            put3.addColumn(Bytes.toBytes(COLUMN_FAMILY_2), Bytes.toBytes("email"), Bytes.toBytes("wangwu@example.com"));
            puts.add(put3);
            
            // 批量插入数据
            table.put(puts);
            System.out.println("成功插入3条数据");
            
            // 读取单行数据
            System.out.println("\n获取单行数据:");
            Get get = new Get(Bytes.toBytes("row1"));
            Result result = table.get(get);
            printRow(result);
            
            // 扫描表数据
            System.out.println("\n扫描表数据:");
            Scan scan = new Scan();
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result scanResult : scanner) {
                    printRow(scanResult);
                }
            }
            
            // 更新数据
            System.out.println("\n更新row1的age值:");
            Put updatePut = new Put(Bytes.toBytes("row1"));
            updatePut.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("age"), Bytes.toBytes("31"));
            table.put(updatePut);
            
            // 验证更新
            result = table.get(new Get(Bytes.toBytes("row1")));
            String newAge = Bytes.toString(result.getValue(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("age")));
            System.out.println("row1的age更新为: " + newAge);
            
            // 删除数据
            System.out.println("\n删除row3:");
            Delete delete = new Delete(Bytes.toBytes("row3"));
            table.delete(delete);
            
            // 验证删除
            boolean exists = table.exists(new Get(Bytes.toBytes("row3")));
            System.out.println("row3是否存在: " + exists);
            
            // 再次扫描表数据
            System.out.println("\n删除后的表数据:");
            try (ResultScanner scanner = table.getScanner(new Scan())) {
                for (Result scanResult : scanner) {
                    printRow(scanResult);
                }
            }
            
        } finally {
            // 关闭表
            table.close();
        }
    }
    
    /**
     * 高级功能示例
     */
    private static void advancedFeaturesExample(Connection connection) throws IOException {
        System.out.println("\n----- 高级功能示例 -----");
        
        // 获取表对象
        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);
        
        try {
            // 1. 过滤器示例
            System.out.println("\n使用过滤器进行查询:");
            
            // 创建一个值过滤器
            SingleColumnValueFilter filter = new SingleColumnValueFilter(
                Bytes.toBytes(COLUMN_FAMILY_1),
                Bytes.toBytes("age"),
                CompareFilter.CompareOp.GREATER,
                Bytes.toBytes("25")
            );
            filter.setFilterIfMissing(true);
            
            // 创建带过滤器的扫描对象
            Scan filteredScan = new Scan();
            filteredScan.setFilter(filter);
            
            System.out.println("查询年龄大于25的用户:");
            try (ResultScanner scanner = table.getScanner(filteredScan)) {
                for (Result result : scanner) {
                    printRow(result);
                }
            }
            
            // 2. 计数器示例
            System.out.println("\n计数器操作示例:");
            
            // 初始化计数器
            Put counterPut = new Put(Bytes.toBytes("counter"));
            counterPut.addColumn(Bytes.toBytes(COLUMN_FAMILY_1), Bytes.toBytes("visits"), Bytes.toBytes(0L));
            table.put(counterPut);
            
            // 递增计数器几次
            for (int i = 0; i < 5; i++) {
                table.incrementColumnValue(
                    Bytes.toBytes("counter"),
                    Bytes.toBytes(COLUMN_FAMILY_1),
                    Bytes.toBytes("visits"),
                    1L
                );
            }
            
            // 读取计数器值
            Get counterGet = new Get(Bytes.toBytes("counter"));
            Result counterResult = table.get(counterGet);
            long counterValue = Bytes.toLong(counterResult.getValue(
                Bytes.toBytes(COLUMN_FAMILY_1),
                Bytes.toBytes("visits")
            ));
            System.out.println("计数器值: " + counterValue);
            
            // 3. 行锁示例
            System.out.println("\n行锁示例:");
            
            // 创建行锁
            RowLock rowLock = table.getRowLock(Bytes.toBytes("row1"));
            try {
                System.out.println("获取row1的行锁");
                
                // 在锁定状态下更新数据
                Put lockedPut = new Put(Bytes.toBytes("row1"));
                lockedPut.addColumn(
                    Bytes.toBytes(COLUMN_FAMILY_1),
                    Bytes.toBytes("city"),
                    Bytes.toBytes("北京")
                );
                table.put(lockedPut);
                
                System.out.println("在行锁状态下更新了row1");
            } finally {
                // 释放行锁
                rowLock.release();
                System.out.println("释放了row1的行锁");
            }
            
            // 验证在锁定状态下的更新
            Get cityGet = new Get(Bytes.toBytes("row1"));
            Result cityResult = table.get(cityGet);
            String city = Bytes.toString(cityResult.getValue(
                Bytes.toBytes(COLUMN_FAMILY_1),
                Bytes.toBytes("city")
            ));
            System.out.println("row1的city值: " + city);
            
        } finally {
            // 关闭表
            table.close();
        }
    }
    
    /**
     * 打印一行数据
     */
    private static void printRow(Result result) {
        String rowKey = Bytes.toString(result.getRow());
        System.out.println("Row: " + rowKey);
        
        // 处理result中的所有cell
        NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> map = result.getMap();
        if (map == null) {
            System.out.println("  - 无数据");
            return;
        }
        
        // 遍历所有列族
        for (byte[] familyBytes : map.keySet()) {
            String family = Bytes.toString(familyBytes);
            NavigableMap<byte[], NavigableMap<Long, byte[]>> familyMap = map.get(familyBytes);
            
            // 遍历列族中的所有列限定符
            for (byte[] qualifierBytes : familyMap.keySet()) {
                String qualifier = Bytes.toString(qualifierBytes);
                NavigableMap<Long, byte[]> qualifierMap = familyMap.get(qualifierBytes);
                
                // 获取最新的值
                byte[] valueBytes = qualifierMap.firstEntry().getValue();
                String value = Bytes.toString(valueBytes);
                
                System.out.println("  - " + family + ":" + qualifier + " = " + value);
            }
        }
    }
} 