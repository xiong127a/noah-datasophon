package com.datasophon.api.service;

import com.mybatisflex.core.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterTenant;

public interface ClusterTenantService extends IService<ClusterTenant> {

    Result listTenant(Integer clusterId, Integer page, Integer size, String tenantName);

    Result saveOrUpdateTenant(ClusterTenant clusterTenant);

    Result deleteTenantById(Integer id);
}
