package com.datasophon.dao.mapper;

import cn.hutool.core.util.StrUtil;
import com.datasophon.dao.entity.ClusterTenantEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.ClusterTenantEntityTableDef.CLUSTER_TENANT_ENTITY;

/**
 * 集群租户数据访问层
 * 使用MyBatis-Flex官方推荐的QueryWrapper方式
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterTenantMapper extends BaseMapper<ClusterTenantEntity> {

    /**
     * 分页查询租户列表（支持租户名称模糊查询）
     */
    default Page<ClusterTenantEntity> selectPageByClusterId(Long clusterId, String tenantName, Integer page,
                                                            Integer size) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CLUSTER_TENANT_ENTITY.CLUSTER_ID.eq(clusterId));

        if (StrUtil.isNotBlank(tenantName)) {
            queryWrapper.and(CLUSTER_TENANT_ENTITY.TENANT_NAME.like(tenantName));
        }

        return paginate(page, size, queryWrapper);
    }

    /**
     * 根据集群ID和租户名称获取租户
     */
    default ClusterTenantEntity selectByClusterIdAndTenantName(Long clusterId, String tenantName) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT_ENTITY.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_TENANT_ENTITY.TENANT_NAME.eq(tenantName)));
    }

    /**
     * 根据集群ID获取所有租户
     */
    default List<ClusterTenantEntity> selectByClusterId(Long clusterId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT_ENTITY.CLUSTER_ID.eq(clusterId)));
    }

    /**
     * 根据集群ID和租户ID列表查询租户
     */
    default List<ClusterTenantEntity> selectByClusterIdAndIds(Long clusterId, List<Long> tenantIds) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT_ENTITY.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_TENANT_ENTITY.ID.in(tenantIds)));
    }
}
