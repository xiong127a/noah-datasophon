package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

/**
 * 租户资源池
 */
@Data
public class TenantResourceCommand implements Serializable {

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
     * kafka topic
     */
    private String kafkaTopic;

    /**
     * kafka topic 容量
     */
    private String kafkaTopicCapacity;

    /**
     * kafka topic 副本数
     */
    private String kafkaTopicReplicas;

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

}
