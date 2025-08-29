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
import com.datasophon.plugins.systeminfo.strategy.OsInfoCollectionStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * Ubuntu系统信息收集策略
 * 继承CentOS策略，重写特定部分
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class UbuntuInfoCollectionStrategy extends CentOsInfoCollectionStrategy {

    @Override
    public OsType getSupportedOsType() {
        return OsType.UBUNTU;
    }

    @Override
    public SystemInfo collectSystemInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            log.debug("开始收集Ubuntu系统信息: hostIp={}", context.getHostIp());

            // 大部分逻辑与CentOS相同，只需要重写特定部分
            SystemInfo systemInfo = super.collectSystemInfo(context, sshService);
            
            // 创建新的SystemInfo，只修改osType
            return SystemInfo.builder()
                    .hostname(systemInfo.getHostname())
                    .osType(OsType.UBUNTU)
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
                    .firewallInfo(collectUbuntuFirewallInfo(context, sshService))
                    .selinuxInfo(collectUbuntuSelinuxInfo(context, sshService))
                    .build();

        } catch (Exception e) {
            log.error("收集Ubuntu系统信息失败: hostIp={}, error={}", context.getHostIp(), e.getMessage(), e);
            throw new RuntimeException("收集Ubuntu系统信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 收集Ubuntu防火墙信息
     */
    private SystemInfo.FirewallInfo collectUbuntuFirewallInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            // Ubuntu通常使用ufw
            String ufwCommand = "ufw status 2>/dev/null | head -1";
            var ufwResult = sshService.executeCommand(context, ufwCommand);
            String ufwStatus = ufwResult != null && ufwResult.isSuccess() ? 
                ufwResult.output() : "";
            
            boolean firewallActive = false;
            String firewallType = "ufw";
            
            if (ufwStatus != null && ufwStatus.contains("active")) {
                firewallActive = true;
            }

            return new SystemInfo.FirewallInfo(firewallActive, firewallType);

        } catch (Exception e) {
            log.warn("收集Ubuntu防火墙信息失败: {}", e.getMessage());
            return new SystemInfo.FirewallInfo(false, "unknown");
        }
    }

    /**
     * 收集Ubuntu SELinux信息（通常Ubuntu不使用SELinux）
     */
    private SystemInfo.SelinuxInfo collectUbuntuSelinuxInfo(HostCheckContext context, SshConnectionService sshService) {
        return new SystemInfo.SelinuxInfo(false, "Not applicable");
    }
}
