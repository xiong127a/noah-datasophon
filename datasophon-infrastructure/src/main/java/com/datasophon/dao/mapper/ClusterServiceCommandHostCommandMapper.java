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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

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
    default Integer getHostCommandTotalProgressByHostnameAndCommandHostId(
            @Param("hostname") String hostname,
            @Param("commandHostId") String commandHostId) {

        QueryWrapper<ClusterServiceCommandHostCommandEntity> wrapper = new QueryWrapper<>();
        // 直接指定聚合函数和别名
        wrapper.select("SUM(command_progress) AS total")
                // 确保字段名与数据库列名匹配（注意驼峰转下划线）
                .eq("hostname", hostname)
                .eq("command_host_id", commandHostId);

        Map<String, Object> result = selectMaps(wrapper).stream().findFirst().orElse(null);

        if (result != null && result.containsKey("total")) {
            Object total = result.get("total");
            return total == null ? 0 : Integer.parseInt(total.toString());
        }
        return 0;
    }
}
