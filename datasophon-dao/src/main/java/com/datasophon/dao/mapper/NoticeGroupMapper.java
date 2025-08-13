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

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.datasophon.dao.entity.NoticeGroupEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeGroupMapper extends BaseMapper<NoticeGroupEntity> {

    /**
     * 根据通知组名称分页查询
     *
     * @param noticeGroupName 通知组名称（可为null）
     * @param offset          偏移量
     * @param pageSize        每页大小
     * @return 通知组列表
     */
    default List<NoticeGroupEntity> selectByNameWithPagination(@Param("noticeGroupName") String noticeGroupName,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize) {
        QueryWrapper query = QueryWrapper.create();

        if (noticeGroupName != null && !noticeGroupName.trim().isEmpty()) {
            query.where(NoticeGroupEntity::getNoticeGroupName).like(noticeGroupName);
        }

        return this.selectListByQuery(query.limit(offset, pageSize));
    }

    /**
     * 根据通知组名称统计总数
     *
     * @param noticeGroupName 通知组名称（可为null）
     * @return 总数
     */
    default long countByName(@Param("noticeGroupName") String noticeGroupName) {
        QueryWrapper query = QueryWrapper.create();

        if (noticeGroupName != null && !noticeGroupName.trim().isEmpty()) {
            query.where(NoticeGroupEntity::getNoticeGroupName).like(noticeGroupName);
        }

        return this.selectCountByQuery(query);
    }

    /**
     * 根据通知组名称查询重复的通知组（排除指定ID）
     *
     * @param noticeGroupName 通知组名称
     * @param excludeId       排除的ID（可为null）
     * @return 通知组列表
     */
    default List<NoticeGroupEntity> selectByNameExcludingId(@Param("noticeGroupName") String noticeGroupName,
            @Param("excludeId") Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .where(NoticeGroupEntity::getNoticeGroupName).eq(noticeGroupName);

        if (excludeId != null) {
            query.and(NoticeGroupEntity::getId).ne(excludeId);
        }

        return this.selectListByQuery(query);
    }
}
