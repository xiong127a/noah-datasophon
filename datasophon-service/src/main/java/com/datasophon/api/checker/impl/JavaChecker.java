package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.service.ParcelRepositoryService;
import com.datasophon.common.dto.ParcelRepositoryDTO;
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
    
    @Value("${datasophon.checker.java.install-path:/usr/local}")
    private String installBasePath;
    
    @Value("${datasophon.checker.java.package-name:jdk/jdk-8u333-linux-x64.tar.gz}")
    private String jdkPackageName;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    @Autowired
    private ParcelRepositoryService parcelRepositoryService;
    
    @Autowired
    private com.datasophon.api.checker.RepairStepExecutor repairStepExecutor;
    
    @Autowired
    private com.datasophon.api.config.CheckerProperties checkerProperties;
    
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
                .clusterId(context.getClusterId() != null ? context.getClusterId() : null)
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
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查JDK环境");
        
        try {
            // 检查 java 命令是否可用
            var pluginContext = toPluginContext(context);
            var command = "java -version 2>&1";
            
            // 记录执行命令
            checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), command);
            
            var result = getSshService().executeCommand(pluginContext, command);
            
            // 记录命令输出
            checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), result.output());
            
            var details = new HashMap<String, Object>();
            details.put("requiredVersion", minVersion);
            
            if (!result.isSuccess() || !result.output().contains("version")) {
                // Java未安装或不可用
                String errorMsg = "Java环境未配置或不可用";
                details.put("recommendation", String.format("请安装JDK %s或更高版本，推荐安装路径: %s", minVersion, defaultPath));
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, details);
                
                var checkResult = CheckResult.failure(
                        errorMsg,
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
                String errorMsg = "无法解析Java版本信息";
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("output", result.output());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查Java安装是否正确",
                        false,
                        true
                );
            }
            
            var actualVersion = matcher.group(1);
            details.put("actualVersion", actualVersion);
            details.put("javaHome", System.getenv("JAVA_HOME"));
            
            // 记录版本解析成功
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("解析Java版本成功: %s", actualVersion), details);
            
            // 检查版本是否满足要求
            if (!isVersionSatisfied(actualVersion, minVersion)) {
                String failMsg = String.format("Java版本过低：实际 %s，要求 %s 或更高", actualVersion, minVersion);
                details.put("recommendation", String.format("请升级到JDK %s或更高版本", minVersion));
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), failMsg, details);
                
                var checkResult = CheckResult.failure(
                        failMsg,
                        String.format("请升级到JDK %s或更高版本", minVersion),
                        false,
                        true
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = String.format("Java环境检查通过：版本 %s", actualVersion);
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查Java环境时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查Java环境时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查Java环境时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    false,
                    false
            );
        }
    }
    
    /**
     * 修复Java环境
     * 
     * 注意：此方法仅执行修复操作（下载、解压、配置JDK），不包含验证逻辑。
     * 验证由框架在修复成功后自动调用 execute() 方法完成，避免重复验证。
     * 
     * @param context 主机检查上下文
     * @param params 修复参数（可选：jdkPackage - 指定JDK包路径）
     * @return RepairResult 修复结果（仅表示修复操作是否成功）
     */
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的Java环境", context.getHostIp());
        
        // 记录修复开始
        checkLogWriter.logRepairStart(context.getClusterId(), context.getHostIp(),
                getCheckKey(), "开始修复Java环境");
        
        try {
            // 获取集群关联的存储库信息
            if (context.getClusterId() == null) {
                String msg = "无法获取集群ID，无法确定存储库";
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), msg, null);
                return RepairResult.builder()
                        .success(false)
                        .message(msg)
                        .build();
            }
            
            ParcelRepositoryDTO repository = parcelRepositoryService.getClusterRepository(context.getClusterId());
            if (repository == null) {
                String msg = "未找到集群关联的存储库";
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), msg, null);
                return RepairResult.builder()
                        .success(false)
                        .message(msg)
                        .build();
            }
            
            // 记录存储库信息
            Map<String, Object> repoInfo = new HashMap<>();
            repoInfo.put("repoName", repository.getRepoName());
            repoInfo.put("repoType", repository.getRepoType());
            repoInfo.put("repoUrl", repository.getRepoUrl());
            checkLogWriter.logRepairInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "获取存储库信息成功", repoInfo);
            
            // 获取JDK包名（支持用户选择或使用配置的默认值）
            String selectedJdkPackage;
            if (params != null && params.containsKey("jdkPackage")) {
                // 用户选择的版本（高级模式）
                selectedJdkPackage = (String) params.get("jdkPackage");
                log.info("使用用户选择的JDK包: {}", selectedJdkPackage);
            } else {
                // 使用配置的默认版本
                var jdkConfig = checkerProperties.getJava().getPackages();
                var defaultJdk = jdkConfig.getAvailableVersions().stream()
                        .filter(v -> v.getVersion().equals(jdkConfig.getDefaultVersion()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未找到默认JDK版本配置: " + jdkConfig.getDefaultVersion()));
                
                // 根据架构选择文件名
                boolean isArm = params != null && Boolean.TRUE.equals(params.get("isArm"));
                String jdkFileName = defaultJdk.getFilenameForArch(isArm);
                
                // 构建完整路径：jdk/文件名
                String jdkSubDir = jdkConfig.getRepositorySubDir();
                selectedJdkPackage = jdkSubDir + jdkFileName;
                
                log.info("使用配置的默认JDK包: {} (版本: {}, ARM: {})", 
                        selectedJdkPackage, jdkConfig.getDefaultVersion(), isArm);
            }
            
            // 构造JDK包下载URL
            String jdkDownloadUrl;
            boolean isHttp = false;
            if ("http".equalsIgnoreCase(repository.getRepoType()) || "https".equalsIgnoreCase(repository.getRepoType())) {
                // HTTP/HTTPS存储库：直接拼接URL
                String baseUrl = repository.getRepoUrl();
                jdkDownloadUrl = baseUrl.endsWith("/") 
                        ? baseUrl + selectedJdkPackage 
                        : baseUrl + "/" + selectedJdkPackage;
                isHttp = true;
            } else if ("local".equalsIgnoreCase(repository.getRepoType())) {
                // 本地存储库：使用文件路径
                String basePath = repository.getRepoUrl();
                jdkDownloadUrl = basePath.endsWith("/") 
                        ? basePath + selectedJdkPackage 
                        : basePath + "/" + selectedJdkPackage;
            } else {
                String msg = "不支持的存储库类型: " + repository.getRepoType();
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), msg, repoInfo);
                return RepairResult.builder()
                        .success(false)
                        .message(msg)
                        .build();
            }
            
            // 提取JDK文件名
            String jdkFileName = selectedJdkPackage.substring(selectedJdkPackage.lastIndexOf("/") + 1);
            String tempDir = "/tmp/jdk_install_" + System.currentTimeMillis();
            
            // 记录下载信息
            Map<String, Object> downloadInfo = new HashMap<>();
            downloadInfo.put("downloadUrl", jdkDownloadUrl);
            downloadInfo.put("fileName", jdkFileName);
            downloadInfo.put("installPath", installBasePath);
            downloadInfo.put("tempDir", tempDir);
            downloadInfo.put("note", "JAVA_HOME将在解压后根据实际目录名设置");
            
            checkLogWriter.logRepairInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "准备JDK安装", downloadInfo);
            
            // 创建修复步骤列表
            // 注意：ConfigureEnvStep 会在执行时自动检测实际的JDK目录名
            java.util.List<com.datasophon.api.checker.RepairStep> steps = new java.util.ArrayList<>();
            steps.add(new com.datasophon.api.checker.steps.jdk.CreateTempDirStep(tempDir));
            steps.add(new com.datasophon.api.checker.steps.jdk.DownloadJdkStep(tempDir, jdkFileName, jdkDownloadUrl, isHttp));
            steps.add(new com.datasophon.api.checker.steps.jdk.ExtractJdkStep(tempDir, jdkFileName, installBasePath));
            steps.add(new com.datasophon.api.checker.steps.jdk.ConfigureEnvStep(installBasePath));
            steps.add(new com.datasophon.api.checker.steps.jdk.CleanupTempStep(tempDir));
            // Note: Verification is handled by framework calling execute() after successful repair
            // No need to add VerifyInstallStep here to avoid duplicate verification
            
            // 使用步骤执行器执行所有步骤
            return repairStepExecutor.executeSteps(steps, context, checkLogWriter, getCheckKey());
                    
        } catch (Exception e) {
            log.error("修复Java环境时发生异常: {}", e.getMessage(), e);
            String errorMsg = "修复失败: " + e.getMessage();
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exception", e.getMessage());
            checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), errorMsg, errorDetails);
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

