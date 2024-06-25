package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterTenant;

public interface ClusterTenantService extends IService<ClusterTenant> {

    Result listTenant(Integer clusterId, Integer page, Integer size, String tenantName);

    Result saveOrUpdateTenant(ClusterTenant clusterTenant) throws Exception;

    Result deleteTenantById(Integer id);
}
