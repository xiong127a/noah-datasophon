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

package com.datasophon.common.model;

import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

@Data
public class RollingRestartInfo {

    /**
     * 每个批次启动几个实例
     */
    private Integer batchCount = 1;

    /**
     * 每个批次执行，间隔时间
     */
    private Integer batchSeparationInSeconds = 120;

    /**
     * 失败节点容错数量，比如 设置为2，在启动的时候，如果累计有>2个实例重启失败,那么后续的实体重启任务停止
     */
    private Integer taskFailureTolerance = 0;


    /**
     * @param rollingParam "1,120,0"
     * @return
     */
    public static RollingRestartInfo parse(String rollingParam) {
        if (StringUtils.isEmpty(rollingParam) || !rollingParam.contains(",")) {
            return null;
        }
        String[] split = rollingParam.split(",");
        RollingRestartInfo rollingRestartInfo = new RollingRestartInfo();
        rollingRestartInfo.setBatchCount(Integer.parseInt(Optional.ofNullable(split[0]).orElse("1")));
        rollingRestartInfo.setBatchSeparationInSeconds(Integer.parseInt(Optional.ofNullable(split[1]).orElse("120")));
        rollingRestartInfo.setTaskFailureTolerance(Integer.parseInt(Optional.ofNullable(split[2]).orElse("0")));
        return rollingRestartInfo;
    }
}
