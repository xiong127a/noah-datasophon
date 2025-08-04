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

import com.datasophon.dao.entity.InstallStepEntity;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 安装步骤数据访问层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface InstallStepMapper extends BaseMapper<InstallStepEntity> {

    /**
     * 根据安装类型查询安装步骤列表
     *
     * @param installType 安装类型
     * @return 安装步骤列表
     */
    default List<InstallStepEntity> selectByInstallType(String installType) {
        return this.selectListByQuery(
                QueryWrapper.create()
                        .where(InstallStepEntity::getInstallType).eq(installType));
    }

    /**
     * 根据安装类型查询安装步骤列表（整数类型）
     *
     * @param installType 安装类型（整数）
     * @return 安装步骤列表
     */
    default List<InstallStepEntity> selectByInstallType(Integer installType) {
        if (installType == null) {
            return List.of();
        }
        return selectByInstallType(installType.toString());
    }

    /**
     * 根据安装类型查询安装步骤数量
     *
     * @param installType 安装类型
     * @return 步骤数量
     */
    default long countByInstallType(String installType) {
        return this.selectCountByQuery(
                QueryWrapper.create()
                        .where(InstallStepEntity::getInstallType).eq(installType));
    }
}
