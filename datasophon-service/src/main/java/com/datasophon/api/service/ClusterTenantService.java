package com.datasophon.api.service;

import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterTenantEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群租户服务接口
 * 提供集群租户的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterTenantService extends IService<ClusterTenantEntity> {

    /**
     * 分页查询租户列表
     */
    PageResult<ClusterTenantDTO> listTenant(Long clusterId, Integer page, Integer size, String tenantName);

    /**
     * 保存或更新租户
     */
    ClusterTenantDTO saveOrUpdateTenant(ClusterTenantDTO clusterTenantDTO);

    /**
     * 根据ID删除租户
     */
    boolean deleteTenantById(Integer id);

    /**
     * 根据ID获取租户DTO
     */
    ClusterTenantDTO getByIdAsDto(Integer id);

    /**
     * 根据集群ID获取所有租户
     */
    List<ClusterTenantDTO> getTenantsByClusterId(Long clusterId);

    /**
     * 根据租户名称获取租户
     */
    ClusterTenantDTO getTenantByName(Long clusterId, String tenantName);

    /**
     * 更新租户
     */
    ClusterTenantDTO updateTenant(ClusterTenantDTO dto);
}
