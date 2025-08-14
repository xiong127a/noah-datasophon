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

import com.datasophon.common.enums.CommandState;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.time.LocalDateTime;

/**
 * 集群服务命令数据访问对象
 * 提供集群服务命令的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterServiceCommandMapper extends BaseMapper<ClusterServiceCommandEntity> {

    /**
     * 根据命令ID查询命令实体
     */
    default ClusterServiceCommandEntity selectByCommandId(String commandId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandEntity::getCommandId).eq(commandId);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据服务实例ID和命令类型查询最新命令
     */
    default ClusterServiceCommandEntity selectLatestByServiceInstanceIdAndCommandType(Long serviceInstanceId,
            int commandType) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceCommandEntity::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceCommandEntity::getCommandType).eq(commandType)
                .orderBy(ClusterServiceCommandEntity::getCreateTime).desc()
                .limit(1);
        return this.selectOneByQuery(query);
    }

    /**
     * 更新命令进度
     */
    default void updateCommandProgress(String commandId, long progress) {
        ClusterServiceCommandEntity entity = selectByCommandId(commandId);
        if (entity != null) {
            entity.setCommandProgress(progress);
            this.update(entity);
        }
    }

    /**
     * 更新命令状态和结束时间
     */
    default void updateCommandStateAndEndTime(String commandId, CommandState commandState, LocalDateTime endTime) {
        ClusterServiceCommandEntity entity = selectByCommandId(commandId);
        if (entity != null) {
            entity.setCommandState(commandState);
            entity.setEndTime(endTime);
            this.update(entity);
        }
    }
}
