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

import com.datasophon.dao.entity.FrameServiceEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 集群框架版本服务表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Mapper
public interface FrameServiceMapper extends BaseMapper<FrameServiceEntity> {

    /**
     * 根据框架ID查询服务列表，按排序号升序
     */
    default List<FrameServiceEntity> selectByFrameIdOrderBySortNum(Integer frameId) {
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getFrameId).eq(frameId)
                .orderBy(FrameServiceEntity::getSortNum).asc();
        return this.selectListByQuery(query);
    }

    /**
     * 根据ID列表查询
     */
    default List<FrameServiceEntity> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getId).in(ids);
        return this.selectListByQuery(query);
    }

    /**
     * 根据框架ID和服务名查询
     */
    default FrameServiceEntity selectByFrameIdAndServiceName(Integer frameId, String serviceName) {
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getFrameId).eq(frameId)
                .and(FrameServiceEntity::getServiceName).eq(serviceName);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据框架代码和服务名查询
     */
    default FrameServiceEntity selectByFrameCodeAndServiceName(String frameCode, String serviceName) {
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getFrameCode).eq(frameCode)
                .and(FrameServiceEntity::getServiceName).eq(serviceName);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据框架代码查询
     */
    default List<FrameServiceEntity> selectByFrameCode(String frameCode) {
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getFrameCode).eq(frameCode);
        return this.selectListByQuery(query);
    }

    /**
     * 根据字符串ID列表查询
     */
    default List<FrameServiceEntity> selectByStringIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getId).in(ids);
        return this.selectListByQuery(query);
    }

    /**
     * 根据ID查询单个实体
     */
    default FrameServiceEntity selectById(Integer id) {
        return this.selectOneById(id);
    }

    /**
     * 插入实体
     */
    default int insert(FrameServiceEntity entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新实体
     */
    default int updateById(FrameServiceEntity entity) {
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
     * 根据框架ID集合查询服务列表
     */
    default List<FrameServiceEntity> selectByFrameIds(java.util.Set<Integer> frameIds) {
        if (frameIds == null || frameIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(FrameServiceEntity::getFrameId).in(frameIds);
        return this.selectListByQuery(query);
    }
}
