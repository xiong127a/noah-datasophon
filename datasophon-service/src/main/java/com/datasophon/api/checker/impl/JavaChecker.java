package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDK环境检查器
 * 检查Java版本是否满足要求
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class JavaChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.java.min-version:1.8}")
    private String minVersion;
    
    @Value("${datasophon.checker.java.default-path:/usr/local/jdk1.8.0_333}")
    private String defaultPath;
    
    @Value("${datasophon.checker.java.check-default-path:true}")
    private boolean checkDefaultPath;
    
    @Value("${datasophon.repair-commands.java:}")
    private String repairCommand;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    private SshConnectionService sshService;
    private static final Pattern VERSION_PATTERN = Pattern.compile("version \"([^\"]+)\"");
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    /**
     * 转换为插件API的HostCheckContext
     */
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .clusterId(context.getClusterId() != null ? context.getClusterId().toString() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "java";
    }
    
    @Override
    public String getDisplayName() {
        return "JDK环境检查";
    }
    
    @Override
    public int getPriority() {
        return 29; // 参考配置
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的JDK环境", context.getHostIp());
        
        try {
            // 检查 java 命令是否可用
            var pluginContext = toPluginContext(context);
            var result = getSshService().executeCommand(pluginContext, "java -version 2>&1");
            
            var details = new HashMap<String, Object>();
            details.put("requiredVersion", minVersion);
            
            if (!result.isSuccess() || !result.output().contains("version")) {
                // Java未安装或不可用
                var checkResult = CheckResult.failure(
                        "Java环境未配置或不可用",
                        String.format("请安装JDK %s或更高版本，推荐安装路径: %s", minVersion, defaultPath),
                        false, // 不能跳过（Java是必需的）
                        true   // 可以修复（可以自动安装）
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            // 解析Java版本
            Matcher matcher = VERSION_PATTERN.matcher(result.output());
            if (!matcher.find()) {
                return CheckResult.failure(
                        "无法解析Java版本信息",
                        "请检查Java安装是否正确",
                        false,
                        true
                );
            }
            
            var actualVersion = matcher.group(1);
            details.put("actualVersion", actualVersion);
            details.put("javaHome", System.getenv("JAVA_HOME"));
            
            // 检查版本是否满足要求
            if (!isVersionSatisfied(actualVersion, minVersion)) {
                var checkResult = CheckResult.failure(
                        String.format("Java版本过低：实际 %s，要求 %s 或更高", actualVersion, minVersion),
                        String.format("请升级到JDK %s或更高版本", minVersion),
                        false,
                        true
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            var checkResult = CheckResult.success(
                    String.format("Java环境检查通过：版本 %s", actualVersion)
            );
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查Java环境时发生异常: {}", e.getMessage(), e);
            return CheckResult.failure(
                    "检查Java环境时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    false,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的Java环境", context.getHostIp());
        
        // 写入修复开始日志
        checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                getCheckKey(), "=== 开始修复Java环境 ===");
        
        try {
            if (repairCommand == null || repairCommand.trim().isEmpty()) {
                String msg = "未配置Java修复命令";
                checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), "错误: " + msg);
                return RepairResult.builder()
                        .success(false)
                        .message(msg)
                        .build();
            }
            
            checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "执行修复脚本:\n" + repairCommand);
            
            var pluginContext = toPluginContext(context);
            var result = getSshService().executeCommand(pluginContext, repairCommand);
            
            checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "脚本输出:\n" + result.output());
            
            if (result.isSuccess()) {
                // 验证修复结果
                var verifyResult = getSshService().executeCommand(pluginContext, "java -version 2>&1");
                if (verifyResult.isSuccess() && verifyResult.output().contains("version")) {
                    String successMsg = "Java环境修复成功";
                    checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), "修复结果: 成功");
                    return RepairResult.builder()
                            .success(true)
                            .message(successMsg)
                            .build();
                } else {
                    String failMsg = "修复脚本执行成功，但Java环境验证失败";
                    checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), "修复结果: 失败 - " + failMsg);
                    return RepairResult.builder()
                            .success(false)
                            .message(failMsg)
                            .build();
                }
            } else {
                String errorMsg = "Java环境修复失败: " + result.error();
                checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), "修复结果: 失败 - " + errorMsg);
                return RepairResult.builder()
                        .success(false)
                        .message(errorMsg)
                        .build();
            }
                    
        } catch (Exception e) {
            log.error("修复Java环境时发生异常: {}", e.getMessage(), e);
            String errorMsg = "修复失败: " + e.getMessage();
            checkLogWriter.writeRepairLog(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "修复异常: " + errorMsg);
            return RepairResult.builder()
                    .success(false)
                    .message(errorMsg)
                    .build();
        }
    }
    
    /**
     * 比较版本号
     */
    private boolean isVersionSatisfied(String actual, String required) {
        try {
            // 简化版本比较逻辑
            // 将 "1.8.0_333" 转换为 "1.8"
            var actualParts = actual.split("[._]");
            var requiredParts = required.split("\\.");
            
            for (int i = 0; i < Math.min(actualParts.length, requiredParts.length); i++) {
                int actualNum = Integer.parseInt(actualParts[i]);
                int requiredNum = Integer.parseInt(requiredParts[i]);
                
                if (actualNum > requiredNum) {
                    return true;
                } else if (actualNum < requiredNum) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.warn("版本比较失败，默认返回通过: {}", e.getMessage());
            return true;
        }
    }
}

