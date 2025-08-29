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

package com.datasophon.plugins.systeminfo.strategy;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import com.datasophon.plugins.api.service.SshConnectionService;

/**
 * 操作系统信息收集策略接口
 * 定义不同操作系统的信息收集方法
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface OsInfoCollectionStrategy {
    
    /**
     * 支持的操作系统类型
     */
    OsType getSupportedOsType();
    
    /**
     * 收集系统信息
     * 
     * @param context SSH连接上下文
     * @param sshService SSH服务
     * @return 系统信息
     */
    SystemInfo collectSystemInfo(HostCheckContext context, SshConnectionService sshService);
    
    /**
     * 解析CPU信息
     */
    SystemInfo.CpuInfo parseCpuInfo(String cpuInfo, String coreCount, String cpuUsage);
    
    /**
     * 解析内存信息
     */
    SystemInfo.MemoryInfo parseMemoryInfo(String memInfo);
    
    /**
     * 解析磁盘信息
     */
    SystemInfo.DiskInfo parseDiskInfo(String diskInfo);
    
    /**
     * 解析Java信息
     */
    SystemInfo.JavaInfo parseJavaInfo(String javaVersion, String javaHome);
    
    /**
     * 解析网络信息
     */
    SystemInfo.NetworkInfo parseNetworkInfo(String interfaceInfo, String routeInfo);

}
