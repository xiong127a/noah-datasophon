package com.datasophon.dao.mapper;

import cn.hutool.core.util.StrUtil;
import com.datasophon.dao.entity.ClusterTenant;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.ClusterTenantTableDef.CLUSTER_TENANT;

/**
 * 集群租户数据访问层
 * 使用MyBatis-Flex官方推荐的QueryWrapper方式
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterTenantMapper extends BaseMapper<ClusterTenant> {

    /**
     * 分页查询租户列表（支持租户名称模糊查询）
     */
    default Page<ClusterTenant> selectPageByClusterId(Integer clusterId, String tenantName, Integer page,
            Integer size) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CLUSTER_TENANT.CLUSTER_ID.eq(clusterId));

        if (StrUtil.isNotBlank(tenantName)) {
            queryWrapper.and(CLUSTER_TENANT.TENANT_NAME.like(tenantName));
        }

        return paginate(page, size, queryWrapper);
    }

    /**
     * 根据集群ID和租户名称获取租户
     */
    default ClusterTenant selectByClusterIdAndTenantName(Integer clusterId, String tenantName) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_TENANT.TENANT_NAME.eq(tenantName)));
    }

    /**
     * 根据集群ID获取所有租户
     */
    default List<ClusterTenant> selectByClusterId(Integer clusterId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT.CLUSTER_ID.eq(clusterId)));
    }

    /**
     * 根据集群ID和租户ID列表查询租户
     */
    default List<ClusterTenant> selectByClusterIdAndIds(Integer clusterId, List<Integer> tenantIds) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_TENANT.CLUSTER_ID.eq(clusterId))
                .and(CLUSTER_TENANT.ID.in(tenantIds)));
    }
}
