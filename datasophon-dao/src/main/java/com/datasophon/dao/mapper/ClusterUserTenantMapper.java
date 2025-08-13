package com.datasophon.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.datasophon.dao.entity.ClusterUserTenantEntity;
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
public interface ClusterUserTenantMapper extends BaseMapper<ClusterUserTenantEntity> {

        /**
         * 根据集群ID、用户ID和租户ID列表查询用户租户关系
         */
        default List<ClusterUserTenantEntity> selectByClusterIdAndUserIdAndTenantIds(
                        @Param("clusterId") Long clusterId,
                        @Param("userId") Integer userId,
                        @Param("tenantIds") List<Integer> tenantIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenantEntity::getClusterId).eq(clusterId)
                                .and(ClusterUserTenantEntity::getUserId).eq(userId)
                                .and(ClusterUserTenantEntity::getTenantId).in(tenantIds);
                return this.selectListByQuery(query);
        }

        /**
         * 根据集群ID和用户ID查询用户的所有租户关系
         */
        default List<ClusterUserTenantEntity> selectByClusterIdAndUserId(
                        @Param("clusterId") Long clusterId,
                        @Param("userId") Integer userId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenantEntity::getClusterId).eq(clusterId)
                                .and(ClusterUserTenantEntity::getUserId).eq(userId);
                return this.selectListByQuery(query);
        }

        /**
         * 根据集群ID、用户ID和租户ID列表删除用户租户关系
         */
        default int deleteByClusterIdAndUserIdAndTenantIds(
                        @Param("clusterId") Long clusterId,
                        @Param("userId") Integer userId,
                        @Param("tenantIds") List<Integer> tenantIds) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenantEntity::getClusterId).eq(clusterId)
                                .and(ClusterUserTenantEntity::getUserId).eq(userId)
                                .and(ClusterUserTenantEntity::getTenantId).in(tenantIds);
                return this.deleteByQuery(query);
        }

        /**
         * 根据租户ID查询用户租户关系列表
         */
        default List<ClusterUserTenantEntity> selectByTenantId(@Param("tenantId") Integer tenantId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterUserTenantEntity::getTenantId).eq(tenantId);
                return this.selectListByQuery(query);
        }
}
