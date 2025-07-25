package com.datasophon.dao.entity;

import com.datasophon.dao.entity.tenantResource.TenantHbaseResource;
import com.datasophon.dao.entity.tenantResource.TenantHdfsResource;
import com.datasophon.dao.entity.tenantResource.TenantHiveResource;
import com.datasophon.dao.entity.tenantResource.TenantKafkaResource;
import com.datasophon.dao.entity.tenantResource.TenantYarnResource;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Table(value = "t_ddh_cluster_tenant")
public class ClusterTenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
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
     * hdfs资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHdfsResource> hdfsResourceList;

    /**
     * yarn资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantYarnResource> yarnResourceList;

    /**
     * hive资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHiveResource> hiveResourceList;

    /**
     * hbase资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHbaseResource> hbaseResourceList;

    /**
     * kafka资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantKafkaResource> kafkaResourceList;
}

