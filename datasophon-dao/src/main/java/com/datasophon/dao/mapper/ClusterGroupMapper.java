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

import com.datasophon.dao.entity.ClusterGroup;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 集群组数据访问对象
 * 提供集群组的数据库操作
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterGroupMapper extends BaseMapper<ClusterGroup> {

    /**
     * 根据集群ID和组名查询集群组
     *
     * @param clusterId 集群ID
     * @param groupName 组名
     * @return 集群组列表
     */
    default List<ClusterGroup> selectByClusterIdAndGroupName(Integer clusterId, String groupName) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterGroup::getClusterId).eq(clusterId)
                .and(ClusterGroup::getGroupName).eq(groupName);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询集群组
     *
     * @param clusterId 集群ID
     * @return 集群组列表
     */
    default List<ClusterGroup> selectByClusterId(Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterGroup::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 分页查询集群组
     *
     * @param clusterId 集群ID
     * @param groupName 组名（可选）
     * @param page      页码
     * @param pageSize  页大小
     * @return 分页结果
     */
    default PageResult<ClusterGroup> selectPageByClusterIdAndGroupName(Integer clusterId, String groupName,
            Integer page, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterGroup::getClusterId).eq(clusterId);

        if (StringUtils.isNotBlank(groupName)) {
            query.and(ClusterGroup::getGroupName).like("%" + groupName + "%");
        }

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<ClusterGroup> flexPage = new Page<>(page, pageSize);
        Page<ClusterGroup> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }
}
