package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUserTenant;

public interface ClusterUserTenantService extends IService<ClusterUserTenant> {

    Result addUserToTenant(ClusterUserTenant clusterUserTenant);

}
