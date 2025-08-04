package com.datasophon.api.service;

import com.datasophon.dao.entity.ClusterUserTenant;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群用户租户服务接口
 * 提供集群用户租户关系的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterUserTenantService extends IService<ClusterUserTenant> {

    /**
     * 租户策略授权给当前用户
     */
    void addUserToTenant(Integer clusterId, Integer userId, String tenantIds);

    /**
     * 删除用户授权
     */
    void deleteUser(Integer clusterId, Integer userId, String tenantIds);

    /**
     * 获取授权列表
     */
    List<ClusterUserTenant> getListByUserId(Integer clusterId, Integer userId);

    /**
     * 根据租户ID获取用户租户关系列表
     */
    List<ClusterUserTenant> getListByTenantId(Integer tenantId);
}
