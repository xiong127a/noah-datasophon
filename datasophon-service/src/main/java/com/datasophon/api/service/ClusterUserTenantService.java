package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUserTenant;

public interface ClusterUserTenantService extends IService<ClusterUserTenant> {

    /**
     * 租户策略授权给当前用户
     */
    Result addUserToTenant(ClusterUserTenant clusterUserTenant);

    /**
     * 删除用户授权
     */
    Result deleteUser(Integer clusterId, String userName, String tenantName);
}
