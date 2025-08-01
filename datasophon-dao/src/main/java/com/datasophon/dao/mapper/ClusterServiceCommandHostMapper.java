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

import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Objects;

/**
 * 集群服务操作指令主机表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Mapper
public interface ClusterServiceCommandHostMapper extends BaseMapper<ClusterServiceCommandHostEntity> {

    /**
     * 获取指定命令的总进度
     *
     * @param commandId 命令ID
     * @return 总进度
     */
    default Integer getCommandHostTotalProgressByCommandId(@Param("commandId") String commandId) {
        // 使用QueryChain和流处理，获取所有命令实例然后计算总和
        List<ClusterServiceCommandHostEntity> entities = QueryChain.of(ClusterServiceCommandHostEntity.class)
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .list();

        // 使用JDK 21的Stream API处理结果
        return entities.stream()
                .map(ClusterServiceCommandHostEntity::getCommandProgress)
                .filter(Objects::nonNull)
                .mapToInt(Long::intValue)
                .sum();
    }

    /**
     * 根据命令ID分页查询
     */
    default PageResult<ClusterServiceCommandHostEntity> selectPageByCommandId(String commandId, Integer page,
            Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .orderBy(ClusterServiceCommandHostEntity::getCreateTime, false);

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<ClusterServiceCommandHostEntity> flexPage = new Page<>(page, pageSize);
        Page<ClusterServiceCommandHostEntity> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }

    /**
     * 根据命令ID统计数量
     */
    default Long countByCommandId(String commandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId);
        return this.selectCountByQuery(query);
    }

    /**
     * 根据命令ID和状态查询
     */
    default List<ClusterServiceCommandHostEntity> selectByCommandIdAndState(String commandId,
            CommandState commandState) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .and(ClusterServiceCommandHostEntity::getCommandState).eq(commandState);
        return this.selectListByQuery(query);
    }

    /**
     * 根据ID查询单个实体
     */
    default ClusterServiceCommandHostEntity selectById(String id) {
        return this.selectOneById(id);
    }

    /**
     * 插入实体
     */
    default int insert(ClusterServiceCommandHostEntity entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新实体
     */
    default int updateById(ClusterServiceCommandHostEntity entity) {
        return this.update(entity);
    }

    /**
     * 根据ID列表删除
     */
    default int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return this.deleteBatchByIds(ids);
    }

    /**
     * 查询所有命令主机
     */
    default List<ClusterServiceCommandHostEntity> selectAll() {
        QueryWrapper query = QueryWrapper.create();
        return this.selectListByQuery(query);
    }
}
