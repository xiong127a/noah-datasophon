package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JavaEnvChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(JavaEnvChecker.class);
    private static final String MIN_JAVA_VERSION = "1.8";
    private static final String DEFAULT_JDK_PATH = "/usr/local/jdk1.8.0_333";

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== Java环境检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("最低Java版本要求: " + MIN_JAVA_VERSION);
            cacheLog.info("默认JDK路径: " + DEFAULT_JDK_PATH);
            
            // 步骤1: 检查是否有java命令
            cacheLog.info("\n步骤1: 检查系统是否存在java命令");
            logger.info("开始检查主机 {} 的Java环境", hostInfo.getHostname());
            
            String javaExistsResult = execCommand(session, "command -v java || echo 'NOT_FOUND'");
            boolean javaCommandExists = !javaExistsResult.contains("NOT_FOUND") && !javaExistsResult.startsWith("ERROR");
            
            cacheLog.info("检查结果: " + (javaCommandExists ? "找到java命令" : "未找到java命令"));

            if (javaCommandExists) {
                // 步骤2: 检查java -version是否显示为Java 8
                cacheLog.info("\n步骤2: 检查java版本是否符合要求");
                logger.info("主机 {} 存在java命令，检查java版本", hostInfo.getHostname());
                
                String javaVersionOutput = execCommand(session, "java -version 2>&1");
                if (!javaVersionOutput.startsWith("ERROR")) {
                    String version = parseJavaVersion(javaVersionOutput);
                    cacheLog.info("解析到的Java版本: " + version);
                    
                    boolean versionMeetRequirement = isVersionMeetRequirement(version, MIN_JAVA_VERSION);
                    cacheLog.info("版本检查结果: " + (versionMeetRequirement ? "符合要求" : "不符合要求"));
                    
                    if (versionMeetRequirement) {
                        cacheLog.info("\n==== Java环境检查通过 ====");
                        cacheLog.info("Java环境正常，版本: " + version);
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("Java环境正常，版本: " + version);
                        return checkItem;
                    } else {
                        cacheLog.info("java版本 " + version + " 低于要求的 " + MIN_JAVA_VERSION);
                        logger.info("主机 {} 的java版本 {} 低于要求的 {}", hostInfo.getHostname(), version, MIN_JAVA_VERSION);
                    }
                } else {
                    cacheLog.info("获取Java版本失败: " + javaVersionOutput);
                }
            }

            // 步骤3: 检查JAVA_HOME环境变量是否存在
            cacheLog.info("\n步骤3: 检查JAVA_HOME环境变量");
            logger.info("检查主机 {} 的JAVA_HOME环境变量", hostInfo.getHostname());
            
            String javaHomeResult = execCommand(session, "echo $JAVA_HOME");
            boolean javaHomeExists = !javaHomeResult.isEmpty() && !javaHomeResult.startsWith("ERROR");
            cacheLog.info("JAVA_HOME检查结果: " + (javaHomeExists ? "存在，值为: " + javaHomeResult : "不存在或为空"));
            
            if (javaHomeExists) {
                // 步骤4: 检查JAVA_HOME指向的路径是否是Java 8
                cacheLog.info("\n步骤4: 检查JAVA_HOME指向的Java是否符合要求");
                logger.info("主机 {} 存在JAVA_HOME: {}, 检查该路径下的Java版本", hostInfo.getHostname(), javaHomeResult);
                
                String javaHomeVersionCmd = "[ -f " + javaHomeResult + "/bin/java ] && " + javaHomeResult + "/bin/java -version 2>&1 || echo 'NOT_EXECUTABLE'";
                cacheLog.info("执行检查命令: " + javaHomeVersionCmd);
                
                String javaHomeVersionOutput = execCommand(session, javaHomeVersionCmd);
                boolean javaHomeExecutable = !javaHomeVersionOutput.contains("NOT_EXECUTABLE") && !javaHomeVersionOutput.startsWith("ERROR");
                cacheLog.info("JAVA_HOME下Java可执行性检查: " + (javaHomeExecutable ? "可执行" : "不可执行"));

                if (javaHomeExecutable) {
                    String version = parseJavaVersion(javaHomeVersionOutput);
                    cacheLog.info("JAVA_HOME下Java版本: " + version);
                    
                    boolean versionMeetRequirement = isVersionMeetRequirement(version, MIN_JAVA_VERSION);
                    cacheLog.info("版本检查结果: " + (versionMeetRequirement ? "符合要求" : "不符合要求"));
                    
                    if (versionMeetRequirement) {
                        cacheLog.info("\n==== Java环境检查通过 ====");
                        cacheLog.info("JAVA_HOME环境正常，版本: " + version + ", 路径: " + javaHomeResult);
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("JAVA_HOME环境正常，版本: " + version + ", 路径: " + javaHomeResult);
                        return checkItem;
                    } else {
                        cacheLog.info("JAVA_HOME指向的Java版本 " + version + " 低于要求的 " + MIN_JAVA_VERSION);
                        logger.info("主机 {} 的JAVA_HOME指向的Java版本 {} 低于要求的 {}", hostInfo.getHostname(), version, MIN_JAVA_VERSION);
                    }
                } else {
                    cacheLog.info("JAVA_HOME指向的路径不包含可执行的Java");
                }
            }

            // 步骤5: 检查/usr/local/jdk1.8.0_333目录是否存在
            cacheLog.info("\n步骤5: 检查默认JDK路径是否存在");
            logger.info("检查主机 {} 的默认JDK路径 {}", hostInfo.getHostname(), DEFAULT_JDK_PATH);
            
            String jdkPathExistsCmd = "[ -d " + DEFAULT_JDK_PATH + " ] && echo 'EXISTS' || echo 'NOT_EXISTS'";
            cacheLog.info("执行检查命令: " + jdkPathExistsCmd);
            
            String jdkPathResult = execCommand(session, jdkPathExistsCmd);
            boolean jdkPathExists = jdkPathResult.contains("EXISTS");
            cacheLog.info("默认JDK路径检查结果: " + (jdkPathExists ? "存在" : "不存在"));

            if (jdkPathExists) {
                // 步骤6: 检查该目录是否是Java 8
                cacheLog.info("\n步骤6: 检查默认JDK路径的Java版本");
                logger.info("主机 {} 存在默认JDK路径，检查该路径下的Java版本", hostInfo.getHostname());
                
                String defaultPathVersionCmd = "[ -f " + DEFAULT_JDK_PATH + "/bin/java ] && " + DEFAULT_JDK_PATH + "/bin/java -version 2>&1 || echo 'NOT_EXECUTABLE'";
                cacheLog.info("执行检查命令: " + defaultPathVersionCmd);
                
                String defaultPathVersionOutput = execCommand(session, defaultPathVersionCmd);
                boolean defaultPathExecutable = !defaultPathVersionOutput.contains("NOT_EXECUTABLE") && !defaultPathVersionOutput.startsWith("ERROR");
                cacheLog.info("默认JDK路径下Java可执行性检查: " + (defaultPathExecutable ? "可执行" : "不可执行"));

                if (defaultPathExecutable) {
                    String version = parseJavaVersion(defaultPathVersionOutput);
                    cacheLog.info("默认JDK路径下Java版本: " + version);
                    
                    boolean versionMeetRequirement = isVersionMeetRequirement(version, MIN_JAVA_VERSION);
                    cacheLog.info("版本检查结果: " + (versionMeetRequirement ? "符合要求" : "不符合要求"));
                    
                    if (versionMeetRequirement) {
                        cacheLog.info("\n==== Java环境检查通过 ====");
                        cacheLog.info("默认JDK路径正常，版本: " + version + ", 路径: " + DEFAULT_JDK_PATH);
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("默认JDK路径正常，版本: " + version + ", 路径: " + DEFAULT_JDK_PATH);
                        return checkItem;
                    } else {
                        cacheLog.info("默认JDK路径的Java版本 " + version + " 低于要求的 " + MIN_JAVA_VERSION);
                        logger.info("主机 {} 默认JDK路径的Java版本 {} 低于要求的 {}", hostInfo.getHostname(), version, MIN_JAVA_VERSION);
                    }
                } else {
                    cacheLog.info("默认JDK路径不包含可执行的Java");
                }
            }

            // 所有检查都不通过，报告Java未安装或版本不符
            cacheLog.info("\n==== Java环境检查未通过 ====");
            cacheLog.info("所有检查路径都未找到符合要求的Java 8环境");
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("未检测到符合要求的Java环境(Java 8)，请安装或配置Java 8");

        } catch (Exception e) {
            String errorMsg = "Java环境检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.info("\n==== Java环境检查出错 ====");
            cacheLog.error("错误: " + errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("Java环境检查失败: " + e.getMessage());
        } finally {
            cacheLog.info("\n==== Java环境检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 设置进度为60%
            hostInfo.setProgress(60);
            logger.info("开始修复主机 {} 的Java环境", hostInfo.getHostname());

            // 检查系统架构
            String arch = execCommand(session, "arch");
            logger.info("主机 {} 的架构为 {}", hostInfo.getHostname(), arch);

            // 检查JDK目录是否存在
            String testResult = execCommand(session, "test -d " + DEFAULT_JDK_PATH + " && echo 'success' || echo 'failed'");
            boolean exists = true;
            if (StringUtils.isNotBlank(testResult) && "failed".equals(testResult)) {
                exists = false;
            }

            // 根据不同架构安装对应的JDK
            if ("x86_64".equals(arch)) {
                if (!exists) {
                    logger.info("主机 {} 上不存在Java环境，开始安装x86_64架构的JDK", hostInfo.getHostname());
                    hostInfo.setMessage("正在安装JDK...");

                    // 上传JDK安装包
                    uploadFile(session, "/usr/local",
                            Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.X86JDK);

                    // 解压JDK安装包
                    execCommand(session, "tar -zxvf /usr/local/jdk-8u333-linux-x64.tar.gz -C /usr/local/");
                    logger.info("主机 {} 的x86_64 JDK安装完成", hostInfo.getHostname());
                }
            } else if ("aarch64".equals(arch)) {
                if (!exists) {
                    logger.info("主机 {} 上不存在Java环境，开始安装ARM架构的JDK", hostInfo.getHostname());
                    hostInfo.setMessage("正在安装JDK...");

                    // 上传JDK安装包
                    uploadFile(session, "/usr/local",
                            Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.ARMJDK);

                    // 解压JDK安装包
                    execCommand(session, "tar -zxvf /usr/local/jdk-8u333-linux-aarch64.tar.gz -C /usr/local/");
                    logger.info("主机 {} 的ARM JDK安装完成", hostInfo.getHostname());
                }
            } else {
                logger.error("主机 {} 的架构 {} 不受支持", hostInfo.getHostname(), arch);
                return false;
            }

            // 验证JDK安装是否成功
            String verifyResult = execCommand(session, "test -d " + DEFAULT_JDK_PATH + " && echo 'success' || echo 'failed'");
            if ("success".equals(verifyResult)) {
                logger.info("主机 {} 的JDK安装验证成功", hostInfo.getHostname());

                // 在需要时配置环境变量
                String configEnvResult = execCommand(session, "grep JAVA_HOME /etc/profile || echo 'not_configured'");
                if (configEnvResult.contains("not_configured")) {
                    logger.info("为主机 {} 配置JAVA_HOME环境变量", hostInfo.getHostname());

                    // 添加JAVA_HOME环境变量
                    String envCmd = "echo 'export JAVA_HOME=" + DEFAULT_JDK_PATH + "' >> /etc/profile && " +
                            "echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile";
                    execCommand(session, envCmd);

                    // 使环境变量生效
                    execCommand(session, "source /etc/profile");
                }

                return true;
            } else {
                logger.error("主机 {} 的JDK安装验证失败", hostInfo.getHostname());
                return false;
            }
        } catch (Exception e) {
            logger.error("修复主机 {} 的Java环境失败: {}", hostInfo.getHostname(), e.getMessage());
            return false;
        }
    }

    private String parseJavaVersion(String javaVersionOutput) {
        // 简单解析Java版本输出
        if (javaVersionOutput.contains("version")) {
            String[] lines = javaVersionOutput.split("\n");
            for (String line : lines) {
                if (line.contains("version")) {
                    // 提取双引号中的版本号，例如 "1.8.0_XXX"
                    int startIdx = line.indexOf("\"") + 1;
                    int endIdx = line.indexOf("\"", startIdx);
                    if (startIdx > 0 && endIdx > startIdx) {
                        String fullVersion = line.substring(startIdx, endIdx);
                        // 只返回主版本号，如 "1.8"
                        int dotIdx = fullVersion.indexOf(".", fullVersion.indexOf(".") + 1);
                        return dotIdx > 0 ? fullVersion.substring(0, dotIdx) : fullVersion;
                    }
                }
            }
        }
        return "未知";
    }

    private boolean isVersionMeetRequirement(String currentVersion, String requiredVersion) {
        try {
            if ("未知".equals(currentVersion)) {
                return false;
            }

            String[] current = currentVersion.split("\\.");
            String[] required = requiredVersion.split("\\.");

            for (int i = 0; i < Math.min(current.length, required.length); i++) {
                int c = Integer.parseInt(current[i]);
                int r = Integer.parseInt(required[i]);
                if (c > r) {
                    return true;
                } else if (c < r) {
                    return false;
                }
            }

            return current.length >= required.length;
        } catch (Exception e) {
            logger.error("版本比较失败: {} vs {}", currentVersion, requiredVersion, e);
            return false;
        }
    }

    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.JAVA_ENV;
    }

    private void uploadFile(ClientSession session,String remoteDir, String localFile) {
        try {
            // TODO: 实现文件上传逻辑
            logger.info("模拟上传文件: {} -> {}", localFile, remoteDir);
        } catch (Exception e) {
            logger.error("上传文件失败: {}", e.getMessage());
        }
    }
} 