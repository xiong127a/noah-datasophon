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

import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群服务操作指令主机指令表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Mapper
public interface ClusterServiceCommandHostCommandMapper extends BaseMapper<ClusterServiceCommandHostCommandEntity> {

    /**
     * 获取指定主机和命令的总进度
     *
     * @param hostname      主机名
     * @param commandHostId 命令主机ID
     * @return 总进度
     */
    default Integer getHostCommandTotalProgressByHostnameAndCommandHostId(@Param("hostname") String hostname,
            @Param("commandHostId") String commandHostId) {
        // 使用MyBatis-Flex的QueryChain进行查询
        Object result = QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .select("SUM(command_progress) as total")
                .where(ClusterServiceCommandHostCommandEntity::getHostname).eq(hostname)
                .and(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .oneAs(Object.class);

        return result == null ? 0 : ((Number) result).intValue();
    }

    /**
     * 根据命令主机ID分页查询
     */
    default PageResult<ClusterServiceCommandHostCommandEntity> selectPageByCommandHostId(String commandHostId,
            Integer page, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .orderBy(ClusterServiceCommandHostCommandEntity::getCreateTime, false);

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<ClusterServiceCommandHostCommandEntity> flexPage = new Page<>(page, pageSize);
        Page<ClusterServiceCommandHostCommandEntity> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }

    /**
     * 根据命令ID查询主机命令列表
     */
    default List<ClusterServiceCommandHostCommandEntity> selectByCommandId(String commandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getCommandId).eq(commandId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据主机命令ID查询
     */
    default ClusterServiceCommandHostCommandEntity selectByHostCommandId(String hostCommandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getHostCommandId).eq(hostCommandId);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据主机命令ID更新
     */
    default int updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getHostCommandId).eq(hostCommand.getHostCommandId());
        return this.updateByQuery(hostCommand, query);
    }

    /**
     * 根据命令主机ID统计数量
     */
    default Long countByCommandHostId(String commandHostId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId);
        return this.selectCountByQuery(query);
    }

    /**
     * 根据命令主机ID和状态查询
     */
    default List<ClusterServiceCommandHostCommandEntity> selectByCommandHostIdAndState(String commandHostId,
            CommandState commandState) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .and(ClusterServiceCommandHostCommandEntity::getCommandState).eq(commandState);
        return this.selectListByQuery(query);
    }

    /**
     * 根据ID查询单个实体
     */
    default ClusterServiceCommandHostCommandEntity selectById(String id) {
        return this.selectOneById(id);
    }

    /**
     * 插入实体
     */
    default int insert(ClusterServiceCommandHostCommandEntity entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新实体
     */
    default int updateById(ClusterServiceCommandHostCommandEntity entity) {
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
     * 查询所有主机命令
     */
    default List<ClusterServiceCommandHostCommandEntity> selectAll() {
        QueryWrapper query = QueryWrapper.create();
        return this.selectListByQuery(query);
    }
}
