package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResource implements Serializable {

    private static final long serialVersionUID = 8665156195475027337L;

    private Integer id;

    /**
     * 集群id
     */
    private Integer clusterId;

    /**
     * 操作的服务名称
     */
    private String serviceName;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * hdfs路径
     */
    private String hdfsPath;

    /**
     * hdfs文件配额
     */
    private String hdfsQuota;

    /**
     * hdfs空间配额
     */
    private String hdfsSpaceQuota;

    /**
     * yarn内存
     */
    private String yarnMemory;

    /**
     * yarn cpu
     */
    private String yarnCpu;

    /**
     * hive数据库名称
     */
    private String hiveDatabase;

    /**
     * hive数据库容量
     */
    private String hiveDatabaseCapacity;

    /**
     * hive存储路径
     */
    private String hiveMetastoreDir;

    /**
     * kafka topic配置
     */
    private String kafkaTopicsConfig;

    /**
     * hbase 命名空间
     */
    private String hbaseNamespace;

    /**
     * hbase 容量
     */
    private String hbaseCapacity;

    /**
     * hbase regionserver数量
     */
    private String hbaseRegionServerNum;

    /**
     * kafka配置的zk地址
     */
    private String kafkaZkAddr;

}
