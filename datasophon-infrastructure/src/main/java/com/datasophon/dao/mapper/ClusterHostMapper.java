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

import com.datasophon.dao.entity.ClusterHostDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 集群主机表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
@Mapper
public interface ClusterHostMapper extends BaseMapper<ClusterHostDO> {

    /**
     * 根据主机名获取集群主机信息
     *
     * @param hostname 主机名
     * @return 集群主机信息
     */
    default ClusterHostDO getClusterHostByHostname(@Param("hostname") String hostname) {
        LambdaQueryWrapper<ClusterHostDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ClusterHostDO::getHostname, hostname);
        return selectOne(queryWrapper);
    }

    /**
     * 批量更新节点标签
     *
     * @param hostIds   主机ID列表（逗号分隔）
     * @param nodeLabel 节点标签
     */
    default void updateBatchNodeLabel(@Param("hostIds") String hostIds, @Param("nodeLabel") String nodeLabel) {
        // 将逗号分隔的主机ID转为List<Integer>
        List<Integer> idList = Arrays.stream(hostIds.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        LambdaUpdateWrapper<ClusterHostDO> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(ClusterHostDO::getNodeLabel, nodeLabel)
                .in(ClusterHostDO::getId, idList);

        update(updateWrapper);
    }
}
