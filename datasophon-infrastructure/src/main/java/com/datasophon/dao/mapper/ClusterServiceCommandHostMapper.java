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
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 集群服务操作指令主机表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@Mapper
public interface ClusterServiceCommandHostMapper extends MPJBaseMapper<ClusterServiceCommandHostEntity> {

    /**
     * 获取指定命令的总进度
     *
     * @param commandId 命令ID
     * @return 总进度
     */
    default Integer getCommandHostTotalProgressByCommandId(@Param("commandId") String commandId) {
        QueryWrapper<ClusterServiceCommandHostEntity> wrapper = new QueryWrapper<>();
        // 直接指定聚合函数和别名，避免 Lambda 表达式解析问题
        wrapper.select("SUM(command_progress) AS total")
                .eq("command_id", commandId);  // 确保字段名与数据库列名一致（如 command_id）

        Map<String, Object> result = selectMaps(wrapper).stream().findFirst().orElse(null);

        if (result != null && result.containsKey("total")) {
            Object total = result.get("total");
            return total == null ? 0 : Integer.parseInt(total.toString());
        }
        return 0;
    }
}
