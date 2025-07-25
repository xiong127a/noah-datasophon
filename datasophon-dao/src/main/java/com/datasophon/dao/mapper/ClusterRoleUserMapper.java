/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群角色用户中间表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@Mapper
public interface ClusterRoleUserMapper extends BaseMapper<ClusterRoleUserEntity> {

    /**
     * 获取指定集群的所有管理员
     *
     * @param clusterId 集群ID
     * @return 管理员用户列表
     */
    default List<UserInfoEntity> getAllClusterManagerByClusterId(@Param("clusterId") Integer clusterId) {
        return QueryChain.of(ClusterRoleUserEntity.class)
                .select("u.*")
                .from("t_ddh_cluster_role_user c") // 给表起别名
                .leftJoin("t_ddh_user_info u").on("u.id = c.user_id")
                .where("c.cluster_id = #{clusterId}", clusterId)
                .listAs(UserInfoEntity.class);
    }
}
