package com.datasophon.dao.entity;

import com.datasophon.dao.entity.base.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 集群用户租户实体类
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
@Table("t_ddh_cluster_user_tenant")
public class ClusterUserTenantEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;



    /**
     * 集群id
     */
    private Long clusterId;

    private Integer userId;

    private Integer tenantId;

    @Column(ignore = true)
    private String tenantName;

}
