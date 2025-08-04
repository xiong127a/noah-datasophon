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

import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.NoticeGroupUserEntityTableDef.NOTICE_GROUP_USER_ENTITY;

/**
 * 通知组-用户中间表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface NoticeGroupUserMapper extends BaseMapper<NoticeGroupUserEntity> {

    /**
     * 根据通知组ID列表删除用户关联关系
     */
    default int deleteByGroupIds(List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return deleteByQuery(QueryWrapper.create()
                .where(NOTICE_GROUP_USER_ENTITY.NOTICE_GROUP_ID.in(groupIds)));
    }

    /**
     * 根据通知组ID查询用户关联关系列表
     */
    default List<NoticeGroupUserEntity> selectByGroupId(Integer groupId) {
        return selectListByQuery(QueryWrapper.create()
                .where(NOTICE_GROUP_USER_ENTITY.NOTICE_GROUP_ID.eq(groupId)));
    }

    /**
     * 根据通知组ID列表查询用户关联关系列表
     */
    default List<NoticeGroupUserEntity> selectByGroupIds(List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of(); // JDK21现代特性：使用List.of()替代Collections.emptyList()
        }
        return selectListByQuery(QueryWrapper.create()
                .where(NOTICE_GROUP_USER_ENTITY.NOTICE_GROUP_ID.in(groupIds)));
    }
}
