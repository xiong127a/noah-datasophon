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

package com.datasophon.plugins.impl.cpu;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * CPU检查器插件
 * 检查主机CPU使用率和负载
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Slf4j
@Extension
public class CpuCheckerPlugin implements HostCheckerPlugin {
    
    private static final String PLUGIN_ID = "cpu-checker";
    private static final String PLUGIN_VERSION = "1.0.0";
    
    // CPU使用率阈值配置
    private static final double CPU_WARNING_THRESHOLD = 80.0;
    private static final double CPU_CRITICAL_THRESHOLD = 95.0;
    
    // 负载平均值阈值
    private static final double LOAD_WARNING_THRESHOLD = 2.0;
    private static final double LOAD_CRITICAL_THRESHOLD = 5.0;
    
    @Override
    public Set<OsType> getSupportedOperatingSystems() {
        // 支持所有Linux系统
        return Set.of(
            OsType.CENTOS,
            OsType.RHEL,
            OsType.UBUNTU,
            OsType.DEBIAN,
            OsType.KYLIN_V10,
            OsType.KYLIN_V4
        );
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级，CPU检查是基础检查
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                log.info("【CPU插件】开始执行CPU检查，主机: {}", context.getHostIp());
                
                // 执行CPU检查 
                CpuMetrics metrics = collectCpuMetrics(context);
                
                // 分析结果
                CheckResult result = analyzeCpuMetrics(metrics, startTime);
                
                log.info("【CPU插件】CPU检查完成，主机: {}, 成功: {}", 
                        context.getHostIp(), result.isSuccess());
                
                return result;
                
            } catch (Exception e) {
                log.error("【CPU插件】CPU检查执行失败，主机: {}", context.getHostIp(), e);
                
                return CheckResult.builder()
                        .checkType("cpu-check")
                        .success(false)
                        .message("CPU检查执行异常: " + e.getMessage())
                        .error(e.getMessage())
                        .checkTime(LocalDateTime.now())
                        .build();
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        // 检查SSH连接信息
        return context.getSshConnectionInfo() != null && 
               context.getSshConnectionInfo().hasAuthenticationInfo();
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId(PLUGIN_ID)
                .name("CPU检查器")
                .version(PLUGIN_VERSION)
                .description("检查主机CPU使用率、负载平均值等指标")
                .author("任相鹏")
                .category("system")
                .supportedOs(Set.of("centos", "rhel", "ubuntu", "debian", "kylin-v10", "kylin-v4"))
                .tags(Set.of("cpu", "performance", "system"))
                .corePlugin(true)
                .build();
    }
    
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }
    
    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }
    
    /**
     * 收集CPU指标 (模拟实现)
     */
    private CpuMetrics collectCpuMetrics(HostCheckContext context) {
        try {
            log.debug("【CPU插件】开始收集CPU指标: {}", context.getHostIp());
            
            // TODO: 这里应该通过SSH连接池执行实际的系统命令
            // 暂时使用模拟数据进行演示
            
            String hostIp = context.getHostIp();
            
            // 模拟不同主机的CPU指标
            double cpuUsage = 15.0 + (hostIp.hashCode() % 30); // 15-45% 范围
            double[] loadAverage = {
                0.5 + (hostIp.hashCode() % 100) / 100.0,  // 1分钟负载
                1.0 + (hostIp.hashCode() % 150) / 100.0,  // 5分钟负载
                1.5 + (hostIp.hashCode() % 200) / 100.0   // 15分钟负载
            };
            int cpuCores = 4 + (hostIp.hashCode() % 8); // 4-12核心
            String cpuModel = "Intel(R) Xeon(R) CPU E5-2686 v4 @ 2.30GHz";
            
            return CpuMetrics.builder()
                    .cpuUsage(cpuUsage)
                    .loadAverage(loadAverage)
                    .cpuCores(cpuCores)
                    .cpuModel(cpuModel)
                    .build();
                    
        } catch (Exception e) {
            log.error("【CPU插件】收集CPU指标失败", e);
            throw new RuntimeException("收集CPU指标失败", e);
        }
    }
    
    /**
     * 分析CPU指标
     */
    private CheckResult analyzeCpuMetrics(CpuMetrics metrics, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .checkType("cpu-check")
                .checkTime(endTime);
        
        CheckResult result;
        
        // 分析CPU使用率
        if (metrics.getCpuUsage() >= CPU_CRITICAL_THRESHOLD) {
            result = resultBuilder
                    .success(false)
                    .message(String.format("CPU使用率过高: %.1f%% (临界阈值: %.1f%%)", 
                            metrics.getCpuUsage(), CPU_CRITICAL_THRESHOLD))
                    .error("CPU使用率超过临界阈值")
                    .build();
                    
        } else if (metrics.getCpuUsage() >= CPU_WARNING_THRESHOLD) {
            result = resultBuilder
                    .success(true)
                    .message(String.format("CPU使用率较高: %.1f%% (警告阈值: %.1f%%)", 
                            metrics.getCpuUsage(), CPU_WARNING_THRESHOLD))
                    .build();
                    
        } else {
            result = resultBuilder
                    .success(true)
                    .message(String.format("CPU使用率正常: %.1f%%", metrics.getCpuUsage()))
                    .build();
        }
        
        // 添加详细信息
        result.data("cpuUsage", String.valueOf(metrics.getCpuUsage()))
              .data("load1Min", String.valueOf(metrics.getLoad1Min()))
              .data("load5Min", String.valueOf(metrics.getLoad5Min()))
              .data("load15Min", String.valueOf(metrics.getLoad15Min()))
              .data("cpuCores", String.valueOf(metrics.getCpuCores()))
              .data("cpuModel", metrics.getCpuModel())
              .data("duration_ms", String.valueOf(duration));
        
        return result;
    }
}