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

import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 告警组表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Mapper
public interface AlertGroupMapper extends BaseMapper<AlertGroupEntity> {

    /**
     * 根据ID列表和名称查询告警组(带分页)
     */
    default PageResult<AlertGroupEntity> selectAlertGroupsByIdsWithName(List<Integer> groupIds, String alertGroupName,
            Integer page, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(AlertGroupEntity::getId).in(groupIds);

        if (StringUtils.isNotBlank(alertGroupName)) {
            query.and(AlertGroupEntity::getAlertGroupName).like(alertGroupName);
        }

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<AlertGroupEntity> flexPage = new Page<>(page, pageSize);
        Page<AlertGroupEntity> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }

    /**
     * 根据ID列表查询
     */
    default List<AlertGroupEntity> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(AlertGroupEntity::getId).in(ids);
        return this.selectListByQuery(query);
    }

    /**
     * 根据ID查询单个实体
     */
    default AlertGroupEntity selectById(Integer id) {
        return this.selectOneById(id);
    }

    /**
     * 插入实体
     */
    default int insert(AlertGroupEntity entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新实体
     */
    default int updateById(AlertGroupEntity entity) {
        return this.update(entity);
    }

    /**
     * 根据ID列表删除
     */
    default int deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return this.deleteBatchByIds(ids);
    }

    /**
     * 查询所有告警组
     */
    default List<AlertGroupEntity> selectAll() {
        QueryWrapper query = QueryWrapper.create();
        return this.selectListByQuery(query);
    }
}
