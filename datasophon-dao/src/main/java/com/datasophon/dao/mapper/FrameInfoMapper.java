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

import com.datasophon.dao.entity.FrameInfoEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import static com.datasophon.dao.entity.table.FrameInfoEntityTableDef.FRAME_INFO_ENTITY;

/**
 * 集群框架表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Mapper
public interface FrameInfoMapper extends BaseMapper<FrameInfoEntity> {

    /**
     * 根据框架代码获取框架信息
     *
     * @param frameCode 框架代码
     * @return 框架信息实体
     */
    default FrameInfoEntity getFrameInfoByFrameCode(@Param("frameCode") String frameCode) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(FRAME_INFO_ENTITY.ALL_COLUMNS)
                .from(FRAME_INFO_ENTITY)
                .where(FRAME_INFO_ENTITY.FRAME_CODE.eq(frameCode));

        return this.selectOneByQuery(queryWrapper);
    }

}
