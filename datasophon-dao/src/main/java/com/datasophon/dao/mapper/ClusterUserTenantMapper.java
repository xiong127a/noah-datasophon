package com.datasophon.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.datasophon.dao.entity.ClusterUserTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群用户租户数据访问对象
 * 提供集群用户租户关系的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterUserTenantMapper extends BaseMapper<ClusterUserTenant> {

        /**
         * 根据集群ID、用户ID和租户ID列表查询用户租户关系
         */
        default List<ClusterUserTenant> selectByClusterIdAndUserIdAndTenantIds(
                        @Param("clusterId") Integer clusterId,
                        @Param("userId") Integer userId,
                        @Param("tenantIds") List<Integer> tenantIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                                .and(ClusterUserTenant::getUserId).eq(userId)
                                .and(ClusterUserTenant::getTenantId).in(tenantIds);
                return this.selectListByQuery(query);
        }

        /**
         * 根据集群ID和用户ID查询用户的所有租户关系
         */
        default List<ClusterUserTenant> selectByClusterIdAndUserId(
                        @Param("clusterId") Integer clusterId,
                        @Param("userId") Integer userId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                                .and(ClusterUserTenant::getUserId).eq(userId);
                return this.selectListByQuery(query);
        }

        /**
         * 根据集群ID、用户ID和租户ID列表删除用户租户关系
         */
        default int deleteByClusterIdAndUserIdAndTenantIds(
                        @Param("clusterId") Integer clusterId,
                        @Param("userId") Integer userId,
                        @Param("tenantIds") List<Integer> tenantIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenant::getClusterId).eq(clusterId)
                                .and(ClusterUserTenant::getUserId).eq(userId)
                                .and(ClusterUserTenant::getTenantId).in(tenantIds);
                return this.deleteByQuery(query);
        }

        /**
         * 根据租户ID查询用户租户关系列表
         */
        default List<ClusterUserTenant> selectByTenantId(@Param("tenantId") Integer tenantId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenant::getTenantId).eq(tenantId);
                return this.selectListByQuery(query);
        }
}
