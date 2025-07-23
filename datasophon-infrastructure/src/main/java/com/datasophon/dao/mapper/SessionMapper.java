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

import com.datasophon.dao.entity.SessionEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.BaseMapper;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-16 11:40:00
 */
@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {

    /**
     * 根据用户ID和IP查询会话
     *
     * @param id 用户ID
     * @param ip IP地址
     * @return 会话信息
     */
    default SessionEntity queryByUserIdAndIp(@Param("userId") Integer id, @Param("ip") String ip) {
        return QueryChain.of(SessionEntity.class)
                .where(SessionEntity::getUserId).eq(id)
                .and(SessionEntity::getIp).eq(ip)
                .one();
    }

    /**
     * 根据用户ID查询所有会话
     *
     * @param id 用户ID
     * @return 会话列表
     */
    default List<SessionEntity> queryByUserId(@Param("userId") Integer id) {
        return QueryChain.of(SessionEntity.class)
                .where(SessionEntity::getUserId).eq(id)
                .list();
    }

    /**
     * 插入会话信息
     *
     * @param session 会话实体
     */
    default void insertSession(SessionEntity session) {
        insert(session);
    }
}
