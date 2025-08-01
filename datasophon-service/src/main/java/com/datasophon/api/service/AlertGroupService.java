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

import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.common.dto.AlertGroupDTO;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 告警组表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
public interface AlertGroupService extends IService<AlertGroupEntity> {

    /**
     * 获取告警组分页列表
     */
    PageResult<AlertGroupDTO> getAlertGroupList(Integer clusterId, String alertGroupName, Integer page,
            Integer pageSize);

    /**
     * 保存告警组
     */
    AlertGroupDTO saveAlertGroup(AlertGroupDTO alertGroup);

    /**
     * 根据ID获取告警组
     */
    AlertGroupDTO getAlertGroupById(Integer id);

    /**
     * 更新告警组
     */
    AlertGroupDTO updateAlertGroup(AlertGroupDTO alertGroup);

    /**
     * 删除告警组
     */
    boolean deleteAlertGroups(List<Integer> ids);

    /**
     * 获取所有告警组
     */
    List<AlertGroupDTO> getAllAlertGroups();

    /**
     * 校验告警组删除前是否绑定指标
     */
    void validateAlertGroupBeforeDelete(List<Integer> ids);
}
