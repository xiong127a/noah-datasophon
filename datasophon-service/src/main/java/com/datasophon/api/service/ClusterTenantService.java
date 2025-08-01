package com.datasophon.api.service;


import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.ClusterTenant;

public interface ClusterTenantService {

    Result listTenant(Integer clusterId, Integer page, Integer size, String tenantName);

    Result saveOrUpdateTenant(ClusterTenant clusterTenant);

    Result deleteTenantById(Integer id);
}
