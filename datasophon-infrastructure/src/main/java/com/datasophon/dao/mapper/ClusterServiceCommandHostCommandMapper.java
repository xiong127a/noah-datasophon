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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.base.MPJBaseMapper;

/**
 * 集群服务操作指令主机指令表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@Mapper
public interface ClusterServiceCommandHostCommandMapper extends MPJBaseMapper<ClusterServiceCommandHostCommandEntity> {

    /**
     * 获取指定主机和命令的总进度
     *
     * @param hostname      主机名
     * @param commandHostId 命令主机ID
     * @return 总进度
     */
    default Integer getHostCommandTotalProgressByHostnameAndCommandHostId(@Param("hostname") String hostname,
            @Param("commandHostId") String commandHostId) {
        // 使用原生SQL查询避免Lambda表达式导致的MyBatis解析问题
        Object result = selectObjs(Wrappers.<ClusterServiceCommandHostCommandEntity>query()
                .select("SUM(command_progress) as total")
                .eq("hostname", hostname)
                .eq("command_host_id", commandHostId))
                .stream()
                .findFirst()
                .orElse(null);

        return result == null ? 0 : ((Number) result).intValue();
    }
}
