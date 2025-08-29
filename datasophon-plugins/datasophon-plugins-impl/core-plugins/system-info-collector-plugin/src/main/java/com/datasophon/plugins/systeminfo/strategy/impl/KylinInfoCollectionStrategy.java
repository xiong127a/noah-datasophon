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

package com.datasophon.plugins.systeminfo.strategy.impl;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Kylin麒麟系统信息收集策略
 * 继承CentOS策略，麒麟系统与CentOS相似
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class KylinInfoCollectionStrategy extends CentOsInfoCollectionStrategy {

    @Override
    public OsType getSupportedOsType() {
        return OsType.KYLIN;
    }

    @Override
    public SystemInfo collectSystemInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            log.debug("开始收集Kylin系统信息: hostIp={}", context.getHostIp());

            // 麒麟系统与CentOS非常相似，大部分逻辑相同
            SystemInfo systemInfo = super.collectSystemInfo(context, sshService);
            
            // 创建新的SystemInfo，只修改osType
            return SystemInfo.builder()
                    .hostname(systemInfo.getHostname())
                    .osType(OsType.KYLIN)
                    .osVersion(systemInfo.getOsVersion())
                    .kernelVersion(systemInfo.getKernelVersion())
                    .cpuArchitecture(systemInfo.getCpuArchitecture())
                    .cpuCoreCount(systemInfo.getCpuCoreCount())
                    .cpuModelName(systemInfo.getCpuModelName())
                    .totalMemoryMB(systemInfo.getTotalMemoryMB())
                    .freeMemoryMB(systemInfo.getFreeMemoryMB())
                    .totalSwapMB(systemInfo.getTotalSwapMB())
                    .freeSwapMB(systemInfo.getFreeSwapMB())
                    .javaInfo(systemInfo.getJavaInfo())
                    .firewallInfo(systemInfo.getFirewallInfo())  // 麒麟与CentOS防火墙相同
                    .selinuxInfo(systemInfo.getSelinuxInfo())    // 麒麟与CentOS SELinux相同
                    .build();

        } catch (Exception e) {
            log.error("收集Kylin系统信息失败: hostIp={}, error={}", context.getHostIp(), e.getMessage(), e);
            throw new RuntimeException("收集Kylin系统信息失败: " + e.getMessage(), e);
        }
    }
}
