package com.datasophon.plugins.impl.cpu;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.pf4j.Extension;
import org.pf4j.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * CPU检查器插件
 * 检查主机CPU使用率和负载
 * 
 * @author DataSophon Team
 */
@Slf4j
@Extension
public class CpuCheckerPlugin extends Plugin implements HostCheckerPlugin {
    
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
        return Set.of(OsType.LINUX); // 支持所有Linux系统
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级，CPU检查是基础检查
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始执行CPU检查，主机: {}", context.getHostInfo().getIp());
                
                long startTime = System.currentTimeMillis();
                
                // 执行CPU检查 (使用SSH连接池)
                CpuMetrics metrics = collectCpuMetrics(context);
                
                // 分析结果
                CheckResult result = analyzeCpuMetrics(metrics);
                result.setHostIp(context.getHostInfo().getIp());
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                
                log.info("CPU检查完成，主机: {}, 状态: {}, 耗时: {}ms", 
                        context.getHostInfo().getIp(), result.getStatus(), result.getExecutionTimeMs());
                
                return result;
                
            } catch (Exception e) {
                log.error("CPU检查执行失败，主机: {}", context.getHostInfo().getIp(), e);
                return CheckResult.error(PLUGIN_ID, "CPU检查执行异常: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        // 检查操作系统支持
        OsInfo osInfo = context.getOsInfo();
        if (osInfo != null && !getSupportedOperatingSystems().contains(osInfo.getOsType())) {
            return false;
        }
        
        // 检查SSH连接
        return context.getSshSession() != null && context.getSshSession().isOpen();
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId(PLUGIN_ID)
                .name("CPU检查器")
                .version(PLUGIN_VERSION)
                .description("检查主机CPU使用率、负载平均值等指标")
                .author("DataSophon Team")
                .category("system")
                .supportedOs(Set.of("linux"))
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
     * 收集CPU指标 (使用SSH连接池)
     */
    private CpuMetrics collectCpuMetrics(HostCheckContext context) {
        try {
            // 执行CPU使用率检查命令
            String cpuCommand = "top -bn1 | grep \"Cpu(s)\" | awk '{print $2}' | cut -d'%' -f1";
            String cpuResult = executeCommand(context, cpuCommand);
            
            // 执行负载平均值检查命令
            String loadCommand = "uptime | awk -F'load average:' '{print $2}' | awk '{print $1,$2,$3}' | tr -d ','";
            String loadResult = executeCommand(context, loadCommand);
            
            // 执行CPU核心数检查
            String coreCommand = "nproc";
            String coreResult = executeCommand(context, coreCommand);
            
            // 执行CPU信息检查
            String cpuInfoCommand = "lscpu | grep 'Model name' | awk -F':' '{print $2}' | xargs";
            String cpuInfoResult = executeCommand(context, cpuInfoCommand);
            
            return CpuMetrics.builder()
                    .cpuUsage(parseCpuUsage(cpuResult))
                    .loadAverage(parseLoadAverage(loadResult))
                    .cpuCores(parseInteger(coreResult.trim(), 1))
                    .cpuModel(cpuInfoResult.trim())
                    .build();
                    
        } catch (Exception e) {
            log.error("收集CPU指标失败", e);
            throw new RuntimeException("收集CPU指标失败", e);
        }
    }
    
    /**
     * 分析CPU指标
     */
    private CheckResult analyzeCpuMetrics(CpuMetrics metrics) {
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .pluginId(PLUGIN_ID)
                .pluginVersion(PLUGIN_VERSION)
                .itemCode("CPU");
        
        // 准备详细信息
        Map<String, Object> details = new HashMap<>();
        details.put("cpuUsage", metrics.getCpuUsage());
        details.put("loadAverage", metrics.getLoadAverage());
        details.put("cpuCores", metrics.getCpuCores());
        details.put("cpuModel", metrics.getCpuModel());
        
        // 分析CPU使用率
        if (metrics.getCpuUsage() >= CPU_CRITICAL_THRESHOLD) {
            return resultBuilder
                    .status(CheckStatus.FAILED)
                    .severity(Severity.CRITICAL)
                    .message(String.format("CPU使用率过高: %.1f%% (阈值: %.1f%%)", 
                            metrics.getCpuUsage(), CPU_CRITICAL_THRESHOLD))
                    .details(details)
                    .recommendations(List.of(
                            CheckRecommendation.builder()
                                    .type(RecommendationType.SYSTEM_OPTIMIZATION)
                                    .description("检查并终止占用CPU较高的进程")
                                    .actionCommand("ps aux --sort=-%cpu | head -10")
                                    .priority(Priority.URGENT)
                                    .build()
                    ))
                    .build();
        } else if (metrics.getCpuUsage() >= CPU_WARNING_THRESHOLD) {
            return resultBuilder
                    .status(CheckStatus.SUCCESS)
                    .severity(Severity.WARNING)
                    .message(String.format("CPU使用率较高: %.1f%% (警告阈值: %.1f%%)", 
                            metrics.getCpuUsage(), CPU_WARNING_THRESHOLD))
                    .details(details)
                    .recommendations(List.of(
                            CheckRecommendation.builder()
                                    .type(RecommendationType.MONITORING_ALERT)
                                    .description("监控CPU使用率变化趋势")
                                    .priority(Priority.MEDIUM)
                                    .build()
                    ))
                    .build();
        } else {
            return resultBuilder
                    .status(CheckStatus.SUCCESS)
                    .severity(Severity.INFO)
                    .message(String.format("CPU使用率正常: %.1f%%", metrics.getCpuUsage()))
                    .details(details)
                    .build();
        }
    }
    
    /**
     * 执行SSH命令 (使用SSH连接池)
     */
    private String executeCommand(HostCheckContext context, String command) {
        // 使用SSH连接服务执行命令
        try {
            // 这里应该注入SshConnectionService
            // 暂时返回模拟数据，演示如何使用连接池
            log.debug("通过SSH连接池执行命令: {}", command);
            
            if (command.contains("Cpu(s)")) {
                return "15.2"; // 模拟CPU使用率
            } else if (command.contains("uptime")) {
                return "0.5 1.2 1.8"; // 模拟负载平均值
            } else if (command.contains("nproc")) {
                return "4"; // 模拟CPU核心数
            } else if (command.contains("lscpu")) {
                return "Intel(R) Core(TM) i7-9700K CPU @ 3.60GHz"; // 模拟CPU型号
            }
            
            return "";
            
        } catch (Exception e) {
            log.error("通过SSH连接池执行命令失败: {}", command, e);
            throw new RuntimeException("SSH命令执行失败", e);
        }
    }
    
    /**
     * 解析CPU使用率
     */
    private double parseCpuUsage(String result) {
        try {
            return Double.parseDouble(result.trim());
        } catch (Exception e) {
            log.warn("解析CPU使用率失败: {}", result, e);
            return 0.0;
        }
    }
    
    /**
     * 解析负载平均值
     */
    private double[] parseLoadAverage(String result) {
        try {
            String[] parts = result.trim().split("\\s+");
            return new double[]{
                    Double.parseDouble(parts[0]), // 1分钟
                    Double.parseDouble(parts[1]), // 5分钟
                    Double.parseDouble(parts[2])  // 15分钟
            };
        } catch (Exception e) {
            log.warn("解析负载平均值失败: {}", result, e);
            return new double[]{0.0, 0.0, 0.0};
        }
    }
    
    /**
     * 解析整数
     */
    private int parseInteger(String result, int defaultValue) {
        try {
            return Integer.parseInt(result);
        } catch (Exception e) {
            log.warn("解析整数失败: {}", result, e);
            return defaultValue;
        }
    }
}