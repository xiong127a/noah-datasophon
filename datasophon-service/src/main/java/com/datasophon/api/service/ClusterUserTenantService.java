package com.datasophon.api.service;


import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.ClusterUserTenant;

public interface ClusterUserTenantService {

    /**
     * 租户策略授权给当前用户
     */
    Result addUserToTenant(Integer clusterId, Integer userId, String tenantIds);

    /**
     * 删除用户授权
     */
    Result deleteUser(Integer clusterId, Integer userId, String tenantIds);

    /**
     * 获取授权列表
     */
    Result getListByUserId(Integer clusterId, Integer userId);
}
