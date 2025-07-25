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

import com.datasophon.dao.entity.ClusterZk;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.mybatisflex.core.BaseMapper;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-09-07 10:04:16
 */
@Mapper
public interface ClusterZkMapper extends BaseMapper<ClusterZk> {

    /**
     * 获取指定集群的最大myid值
     *
     * @param clusterId 集群ID
     * @return 最大myid值
     */
    @Select("SELECT MAX(myid) FROM t_ddh_cluster_zk WHERE cluster_id = #{clusterId}")
    Integer getMaxMyId(@Param("clusterId") Integer clusterId);
}
