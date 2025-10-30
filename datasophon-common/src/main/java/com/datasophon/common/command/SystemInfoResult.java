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

package com.datasophon.common.command;

import com.datasophon.common.utils.ExecResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统信息结果
 * 
 * @author DataSophon Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemInfoResult extends ExecResult {
    private Long clusterId;
    private String hostname;
    private String ipAddress;
    private String cpuArchitecture;
    private String osVersion;
    private String memoryInfo;
    private String diskInfo;
    private int cpuCores;
    private String systemLoad;
    
    public SystemInfoResult() {
        super();
        setExecResult(true); // 默认设置为成功
    }
}



