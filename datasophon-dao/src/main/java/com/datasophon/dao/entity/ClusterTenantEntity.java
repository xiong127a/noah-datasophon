package com.datasophon.dao.entity;

import com.datasophon.dao.entity.base.BaseEntity;
import com.datasophon.dao.entity.tenantResource.TenantHbaseResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantHdfsResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantHiveResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantKafkaResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantYarnResourceEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

/**
 * 集群租户实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(value = "t_ddh_cluster_tenant")
public class ClusterTenantEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;



    /**
     * 集群id
     */
    private Long clusterId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * hdfs资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHdfsResourceEntity> hdfsResourceList;

    /**
     * yarn资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantYarnResourceEntity> yarnResourceList;

    /**
     * hive资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHiveResourceEntity> hiveResourceList;

    /**
     * hbase资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantHbaseResourceEntity> hbaseResourceList;

    /**
     * kafka资源列表
     */
    @Column(typeHandler = JacksonTypeHandler.class)
    private List<TenantKafkaResourceEntity> kafkaResourceList;
}

