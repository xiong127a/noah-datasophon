package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("t_ddh_cluster_tenant")
public class ClusterTenant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 集群id
     */
    private Integer clusterId;

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
}
