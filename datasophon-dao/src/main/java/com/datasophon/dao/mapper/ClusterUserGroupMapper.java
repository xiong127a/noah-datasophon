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

import com.datasophon.dao.entity.ClusterUserGroup;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群用户组关联数据访问对象
 * 提供集群用户组关联的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterUserGroupMapper extends BaseMapper<ClusterUserGroup> {

    /**
     * 统计指定组的用户数量
     *
     * @param groupId 组ID
     * @return 用户数量
     */
    default long countByGroupId(@Param("groupId") Integer groupId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUserGroup::getGroupId).eq(groupId);
        return this.selectCountByQuery(query);
    }

    /**
     * 根据用户ID删除所有关联
     *
     * @param userId 用户ID
     * @return 删除的记录数
     */
    default int deleteByUserId(@Param("userId") Integer userId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUserGroup::getUserId).eq(userId);
        return this.deleteByQuery(query);
    }

    /**
     * 根据用户ID和用户组类型查询
     *
     * @param userId        用户ID
     * @param userGroupType 用户组类型
     * @return 用户组关联列表
     */
    default List<ClusterUserGroup> selectByUserIdAndType(
            @Param("userId") Integer userId,
            @Param("userGroupType") Integer userGroupType) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUserGroup::getUserId).eq(userId)
                .and(ClusterUserGroup::getUserGroupType).eq(userGroupType);
        return this.selectListByQuery(query);
    }

    /**
     * 根据组ID查询所有用户关联
     *
     * @param groupId 组ID
     * @return 用户组关联列表
     */
    default List<ClusterUserGroup> selectByGroupId(@Param("groupId") Integer groupId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterUserGroup::getGroupId).eq(groupId);
        return this.selectListByQuery(query);
    }
}
