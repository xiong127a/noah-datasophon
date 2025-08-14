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
import com.datasophon.common.enums.CommandState;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Objects;

/**
 * 集群服务命令主机数据访问对象
 * 提供集群服务命令主机的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterServiceCommandHostMapper extends BaseMapper<ClusterServiceCommandHostEntity> {

    /**
     * 获取指定命令的总进度
     *
     * @param commandId 命令ID
     * @return 总进度
     */
    default Integer getCommandHostTotalProgressByCommandId(@Param("commandId") Long commandId) {
        // SQL逻辑迁移到DAO层，使用QueryWrapper
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId);
        List<ClusterServiceCommandHostEntity> entities = this.selectListByQuery(query);

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
    default Long countByCommandId(Long commandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId);
        return this.selectCountByQuery(query);
    }

    /**
     * 根据命令ID和状态查询
     */
    default List<ClusterServiceCommandHostEntity> selectByCommandIdAndState(Long commandId,
            CommandState commandState) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .and(ClusterServiceCommandHostEntity::getCommandState).eq(commandState);
        return this.selectListByQuery(query);
    }

    /**
     * 根据命令ID查询所有主机命令
     */
    default List<ClusterServiceCommandHostEntity> selectByCommandId(Long commandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据命令主机ID获取命令主机实体
     */
    default ClusterServiceCommandHostEntity getCommandHostByCommandHostId(Long commandHostId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostEntity::getId).eq(commandHostId);
        return this.selectOneByQuery(query);
    }

    /**
     * 更新命令主机进度
     */
    default void updateCommandHostProgress(Long commandHostId, Long progress) {
        ClusterServiceCommandHostEntity entity = getCommandHostByCommandHostId(commandHostId);
        if (entity != null) {
            entity.setCommandProgress(progress);
            this.update(entity);
        }
    }

    /**
     * 更新命令主机状态
     */
    default void updateCommandHostState(Long commandHostId, CommandState commandState) {
        ClusterServiceCommandHostEntity entity = getCommandHostByCommandHostId(commandHostId);
        if (entity != null) {
            entity.setCommandState(commandState);
            this.update(entity);
        }
    }
}
