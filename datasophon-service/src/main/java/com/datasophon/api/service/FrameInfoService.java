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

import com.datasophon.common.dto.FrameInfoDTO;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群框架表服务接口
 * 继承IService提供基础CRUD操作，返回DTO进行数据传输
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface FrameInfoService extends IService<FrameInfoEntity> {

    /**
     * 获取所有集群框架信息（包含服务列表）
     */
    List<FrameInfoDTO> getAllClusterFrame();

    /**
     * 根据框架代码获取框架信息
     */
    FrameInfoDTO getFrameInfoByFrameCode(String frameCode);

    /**
     * 保存框架信息
     */
    FrameInfoDTO saveFrameInfo(FrameInfoDTO frameInfoDTO);

    /**
     * 更新框架信息
     */
    FrameInfoDTO updateFrameInfo(FrameInfoDTO frameInfoDTO);

    /**
     * 根据ID获取框架信息
     */
    FrameInfoDTO getFrameInfoById(Long id);

    /**
     * 批量删除框架信息
     */
    boolean removeFrameInfoByIds(List<Integer> ids);

    /**
     * 获取所有框架信息（不包含服务列表）
     */
    List<FrameInfoDTO> getAllFrameInfos();
}
