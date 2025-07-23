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
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Objects;

/**
 * 集群服务操作指令主机表
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
}
