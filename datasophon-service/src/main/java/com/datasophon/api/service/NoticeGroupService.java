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

package com.datasophon.api.service;

import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.common.dto.NoticeGroupDTO;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 通知组表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public interface NoticeGroupService extends IService<NoticeGroupEntity> {

    /**
     * 获取通知组分页列表
     */
    PageResult<NoticeGroupDTO> getNoticeGroupList(String noticeGroupName, Integer page, Integer pageSize);

    /**
     * 保存通知组
     */
    NoticeGroupDTO saveNoticeGroup(NoticeGroupDTO noticeGroup);

    /**
     * 根据ID获取通知组
     */
    NoticeGroupDTO getNoticeGroupById(Integer id);

    /**
     * 更新通知组
     */
    NoticeGroupDTO updateNoticeGroup(NoticeGroupDTO noticeGroup);

    /**
     * 删除通知组
     */
    boolean deleteNoticeGroups(List<Integer> ids);

    /**
     * 获取所有通知组
     */
    List<NoticeGroupDTO> getAllNoticeGroups();

    /**
     * 校验通知组删除前是否被告警指标使用
     */
    void validateNoticeGroupBeforeDelete(List<Integer> ids);

    /**
     * 根据通知组ID列表获取通知组
     */
    List<NoticeGroupDTO> getByIds(List<Integer> ids);
}
