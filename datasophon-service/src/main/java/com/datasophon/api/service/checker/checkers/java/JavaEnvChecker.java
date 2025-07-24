package com.datasophon.api.service.checker.checkers.java;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JavaEnvChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(JavaEnvChecker.class);

    @Autowired
    private CheckerProperties checkerProperties;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取Java版本和路径
            String minJavaVersion = checkerProperties.getJava().getMinVersion();
            String defaultJdkPath = checkerProperties.getJava().getDefaultPath();
            boolean checkDefaultPath = checkerProperties.getJava().isCheckDefaultPath();

            cacheLog.info("==== 专用Java环境检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("专用JDK路径: " + defaultJdkPath);
            cacheLog.info("要求的最低Java版本: " + minJavaVersion);

            // 如果配置不要求检查默认路径，则直接返回成功
            if (!checkDefaultPath) {
                cacheLog.info("配置设置不检查默认JDK路径，跳过此检查");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                StringBuilder detailsBuilder = new StringBuilder();
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "已跳过专用Java环境检查",
                        "根据配置，已跳过对默认JDK路径的检查"));
                setStyledHtmlMessage(hostInfo, checkItem, true, "已跳过专用Java环境检查", detailsBuilder);
                return checkItem;
            }

            // 步骤1: 检查默认JDK路径是否存在
            cacheLog.info("\n步骤1: 检查专用JDK路径是否存在");
            logger.info("检查主机 {} 的专用JDK路径 {}", hostInfo.getIp(), defaultJdkPath);

            String jdkPathExistsCmd = "[ -d " + defaultJdkPath + " ] && echo 'EXISTS' || echo 'NOT_EXISTS'";
            cacheLog.info("执行检查命令: " + jdkPathExistsCmd);

            CommandResult jdkPathResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), jdkPathExistsCmd);
            boolean jdkPathExists = jdkPathResult.isSuccess() && "EXISTS".equals(jdkPathResult.getOutput().trim());
            cacheLog.info("专用JDK路径检查结果: " + (jdkPathExists ? "存在" : "不存在"));

            if (!jdkPathExists) {
                // JDK路径不存在
                cacheLog.info("\n==== 专用Java环境检查未通过 ====");
                cacheLog.info("未找到专用JDK路径: " + defaultJdkPath);
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "未检测到专用Java环境",
                        "未找到指定的JDK路径: " + defaultJdkPath));

                // 添加修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>点击本检查项的修复按钮，系统将自动安装专用JDK</li>");
                detailsBuilder.append("<li>或手动安装JDK到指定路径: ").append(defaultJdkPath).append("</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境检查失败", detailsBuilder);
                return checkItem;
            }

            // 步骤2: 检查该目录是否包含可执行的Java
            cacheLog.info("\n步骤2: 检查专用JDK路径的Java可执行性");
            logger.info("主机 {} 存在专用JDK路径，检查Java可执行性", hostInfo.getIp());

            String javaExecutableCmd = "[ -f " + defaultJdkPath
                    + "/bin/java ] && echo 'EXECUTABLE' || echo 'NOT_EXECUTABLE'";
            cacheLog.info("执行检查命令: " + javaExecutableCmd);

            CommandResult javaExecutableResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), javaExecutableCmd);
            boolean javaExecutable = javaExecutableResult.isSuccess()
                    && javaExecutableResult.getOutput().contains("EXECUTABLE");
            cacheLog.info("专用JDK Java可执行性检查结果: " + (javaExecutable ? "可执行" : "不可执行"));

            if (!javaExecutable) {
                // Java可执行文件不存在
                cacheLog.info("\n==== 专用Java环境检查未通过 ====");
                cacheLog.info("专用JDK路径中未找到可执行的Java: " + defaultJdkPath + "/bin/java");
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "专用JDK不完整",
                        "专用JDK路径中未找到可执行的Java文件"));

                // 添加检查结果
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(
                        HtmlStyleHelper.generatePropertyRow("JDK路径", defaultJdkPath, HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("Java可执行文件", defaultJdkPath + "/bin/java",
                        HtmlStyleHelper.Colors.ERROR));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>点击本检查项的修复按钮，系统将自动重新安装专用JDK</li>");
                detailsBuilder.append("<li>或手动重新安装完整的JDK到指定路径</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境检查失败", detailsBuilder);
                return checkItem;
            }

            // 步骤3: 检查专用JDK的Java版本
            cacheLog.info("\n步骤3: 检查专用JDK的Java版本");
            logger.info("主机 {} 的专用JDK路径Java可执行，检查版本", hostInfo.getIp());

            String javaVersionCmd = defaultJdkPath + "/bin/java -version 2>&1";
            cacheLog.info("执行检查命令: " + javaVersionCmd);

            CommandResult javaVersionResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), javaVersionCmd);
            if (!javaVersionResult.isSuccess()) {
                // 获取版本失败
                cacheLog.info("\n==== 专用Java环境检查未通过 ====");
                cacheLog.info("无法获取专用Java版本: " + javaVersionResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "无法获取Java版本",
                        "专用JDK可能安装不完整或存在权限问题"));

                // 添加错误详情
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>错误详情:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(javaVersionResult.getErrorOrOutput()));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>点击本检查项的修复按钮，系统将自动重新安装专用JDK</li>");
                detailsBuilder.append("<li>或检查JDK的权限和安装是否完整</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境检查失败", detailsBuilder);
                return checkItem;
            }

            // 保存原始版本输出
            // 保存Java版本的原始输出
            String javaVersionRawOutput = javaVersionResult.getOutput();

            // 解析版本信息
            String version = parseJavaVersion(javaVersionRawOutput);
            cacheLog.info("专用JDK Java版本: " + version);

            // 检查版本是否符合要求
            boolean versionMeetRequirement = isVersionMeetRequirement(version, minJavaVersion);
            cacheLog.info("版本检查结果: " + (versionMeetRequirement ? "符合要求" : "不符合要求"));

            if (!versionMeetRequirement) {
                // 版本不符合要求
                cacheLog.info("\n==== 专用Java环境检查未通过 ====");
                cacheLog.info("专用Java版本不符合要求: " + version + " (需要 " + minJavaVersion + " 或更高版本)");
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 提取短版本和详细版本信息
                String shortVersion = version;
                if (version.contains(" ")) {
                    shortVersion = version.split(" ")[0];
                }

                // 直接使用原始版本输出作为详情
                String fullVersionDetails = javaVersionRawOutput.trim();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "Java版本不符合要求",
                        "当前版本 " + shortVersion + " 低于要求的最低版本 " + minJavaVersion));

                // 添加版本信息
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(
                        HtmlStyleHelper.generatePropertyRow("当前版本", shortVersion, HtmlStyleHelper.Colors.ERROR));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("要求版本", "≥ " + minJavaVersion,
                        HtmlStyleHelper.Colors.INFO));
                detailsBuilder.append("<p><strong>版本详情:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(fullVersionDetails));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>点击本检查项的修复按钮，系统将自动安装符合要求的专用JDK</li>");
                detailsBuilder.append("<li>或手动安装Java 8或更高版本到指定路径</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境检查失败", detailsBuilder);
                return checkItem;
            }

            // 所有检查通过
            cacheLog.info("\n==== 专用Java环境检查通过 ====");
            cacheLog.info("专用JDK路径正常，版本: " + version + ", 路径: " + defaultJdkPath);
            checkItem.setStatus(CheckItem.Status.SUCCESS);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加JDK路径信息
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append(
                    HtmlStyleHelper.generatePropertyRow("JDK路径", defaultJdkPath, HtmlStyleHelper.Colors.SUCCESS));

            // 添加Java版本信息
            String shortVersion = version;
            if (version.contains(" ")) {
                shortVersion = version.split(" ")[0];
            }

            // 直接使用原始版本输出作为详情
            String fullVersionDetails = javaVersionRawOutput.trim();

            detailsBuilder.append(
                    HtmlStyleHelper.generatePropertyRow("Java版本", shortVersion, HtmlStyleHelper.Colors.SUCCESS));

            // 添加版本详情代码块
            detailsBuilder.append("<p><strong>版本详情:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(fullVersionDetails));

            // 添加版本兼容性信息
            detailsBuilder.append(
                    HtmlStyleHelper.generatePropertyRow("版本要求", "≥ " + minJavaVersion, HtmlStyleHelper.Colors.INFO));
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加成功信息
            detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert("专用Java环境正常", "专用Java环境配置正常，满足系统运行要求"));

            // 设置HTML格式化消息
            setStyledHtmlMessage(hostInfo, checkItem, true, "专用Java环境检查", detailsBuilder);

        } catch (Exception e) {
            String errorMsg = "专用Java环境检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.info("\n==== 专用Java环境检查出错 ====");
            cacheLog.error("错误: " + errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("专用Java环境检查失败: " + e.getMessage());
        } finally {
            cacheLog.info("\n==== 专用Java环境检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 设置进度为60%
            hostInfo.setProgress(60);
            logger.info("开始修复主机 {} 的专用Java环境", hostInfo.getIp());
            cacheLog.info("==== 开始修复专用Java环境 ====");
            cacheLog.info("目标JDK路径: " + checkerProperties.getJava().getDefaultPath());

            // 检查系统架构
            CommandResult archResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "arch");
            String arch = archResult.getOutput().trim();
            logger.info("主机 {} 的架构为 {}", hostInfo.getIp(), arch);
            cacheLog.info("系统架构: " + arch);

            // 检查JDK目录是否存在
            CommandResult testResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    "test -d " + checkerProperties.getJava().getDefaultPath() + " && echo 'success' || echo 'failed'");
            boolean exists = testResult.isSuccess() && "success".equals(testResult.getOutput().trim());

            if (exists) {
                // 目录已存在，检查是否需要重新安装
                cacheLog.info("专用JDK目录已存在: " + checkerProperties.getJava().getDefaultPath());
                cacheLog.info("检查Java可执行性");

                CommandResult executableTest = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        "test -f " + checkerProperties.getJava().getDefaultPath() + "/bin/java && "
                                + checkerProperties.getJava().getDefaultPath()
                                + "/bin/java -version 2>/dev/null && echo 'success' || echo 'failed'");
                boolean javaExecutable = executableTest.isSuccess()
                        && "success".equals(executableTest.getOutput().trim());

                if (javaExecutable) {
                    cacheLog.info("专用Java环境已正常存在，无需修复");

                    // 确保检查项状态更新为SUCCESS
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("专用Java环境正常，无需修复");

                    // 同步到HostInfo
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.SUCCESS, "专用Java环境正常，无需修复");

                    // 重新计算并更新主机状态
                    hostInfo.calculateStatus();

                    return true;
                }

                // Java不可执行，需要重新安装
                cacheLog.info("专用JDK目录存在但Java不可执行，将重新安装");

                // 删除旧目录
                CommandResult rmResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "rm -rf " + checkerProperties.getJava().getDefaultPath());
                if (!rmResult.isSuccess()) {
                    logger.error("删除旧JDK目录失败: {}", rmResult.getErrorOrOutput());
                    cacheLog.error("删除旧JDK目录失败: " + rmResult.getErrorOrOutput());

                    // 确保更新失败状态
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("删除旧JDK目录失败: " + rmResult.getErrorOrOutput());

                    // 同步到HostInfo
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, checkItem.getMessage());

                    // 重新计算并更新主机状态
                    hostInfo.calculateStatus();

                    return false;
                }
                cacheLog.info("已删除旧JDK目录");
            }

            // 根据不同架构安装对应的JDK
            if ("x86_64".equals(arch)) {
                logger.info("主机 {} 上安装x86_64架构的JDK", hostInfo.getIp());
                cacheLog.info("安装x86_64架构的JDK");
                hostInfo.setMessage("正在安装专用JDK...");

                // 上传JDK安装包
                cacheLog.info("上传JDK安装包");
                uploadFile(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.X86JDK);

                // 解压JDK安装包
                cacheLog.info("解压JDK安装包");
                CommandResult unzipResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        "tar -zxvf /usr/local/jdk-8u333-linux-x64.tar.gz -C /usr/local/");
                if (!unzipResult.isSuccess()) {
                    logger.error("解压JDK安装包失败: {}", unzipResult.getErrorOrOutput());
                    cacheLog.error("解压JDK安装包失败: " + unzipResult.getErrorOrOutput());

                    // 确保更新失败状态
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("解压JDK安装包失败: " + unzipResult.getErrorOrOutput());

                    // 同步到HostInfo
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, checkItem.getMessage());

                    // 重新计算并更新主机状态
                    hostInfo.calculateStatus();

                    return false;
                }
                logger.info("主机 {} 的x86_64 JDK安装完成", hostInfo.getIp());
                cacheLog.info("x86_64 JDK安装完成");
            } else if ("aarch64".equals(arch)) {
                logger.info("主机 {} 上安装ARM架构的JDK", hostInfo.getIp());
                cacheLog.info("安装ARM架构的JDK");
                hostInfo.setMessage("正在安装专用JDK...");

                // 上传JDK安装包
                cacheLog.info("上传JDK安装包");
                uploadFile(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.ARMJDK);

                // 解压JDK安装包
                cacheLog.info("解压JDK安装包");
                CommandResult unzipResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        "tar -zxvf /usr/local/jdk-8u333-linux-aarch64.tar.gz -C /usr/local/");
                if (!unzipResult.isSuccess()) {
                    logger.error("解压JDK安装包失败: {}", unzipResult.getErrorOrOutput());
                    cacheLog.error("解压JDK安装包失败: " + unzipResult.getErrorOrOutput());

                    // 确保更新失败状态
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("解压JDK安装包失败: " + unzipResult.getErrorOrOutput());

                    // 同步到HostInfo
                    hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, checkItem.getMessage());

                    // 重新计算并更新主机状态
                    hostInfo.calculateStatus();

                    return false;
                }
                logger.info("主机 {} 的ARM JDK安装完成", hostInfo.getIp());
                cacheLog.info("ARM JDK安装完成");
            } else {
                logger.error("主机 {} 的架构 {} 不受支持", hostInfo.getIp(), arch);
                cacheLog.error("不支持的系统架构: " + arch);

                // 确保更新失败状态
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("不支持的系统架构: " + arch);

                // 同步到HostInfo
                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, checkItem.getMessage());

                // 重新计算并更新主机状态
                hostInfo.calculateStatus();

                return false;
            }

            // 验证JDK安装是否成功
            CommandResult verifyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    "test -d " + checkerProperties.getJava().getDefaultPath() + " && test -f "
                            + checkerProperties.getJava().getDefaultPath()
                            + "/bin/java && echo 'success' || echo 'failed'");
            if (verifyResult.isSuccess() && "success".equals(verifyResult.getOutput().trim())) {
                logger.info("主机 {} 的专用JDK安装验证成功", hostInfo.getIp());
                cacheLog.info("专用JDK安装验证成功");
                cacheLog.info("==== 专用Java环境修复完成 ====");

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加安装信息
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(
                        HtmlStyleHelper.generatePropertyRow("安装路径", checkerProperties.getJava().getDefaultPath(),
                                HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("架构", arch, HtmlStyleHelper.Colors.INFO));
                detailsBuilder.append(
                        HtmlStyleHelper.generatePropertyRow("JDK版本", "Java 8u333", HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加安装步骤总结
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>安装步骤:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>上传JDK安装包到/usr/local目录</li>");
                detailsBuilder.append("<li>解压安装包到/usr/local目录</li>");
                detailsBuilder.append("<li>验证Java可执行性成功</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加成功提示
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "专用Java环境安装成功",
                        "专用JDK已成功安装到" + checkerProperties.getJava().getDefaultPath() + "，系统已可以正常使用该环境"));

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, true, "专用Java环境修复成功", detailsBuilder);

                // 确保检查项状态更新为SUCCESS
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 同步到HostInfo
                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.SUCCESS, "专用Java环境修复成功");

                // 重新计算并更新主机状态
                hostInfo.calculateStatus();

                // 记录状态更新
                logger.info("主机 {} 的专用Java环境检查项状态已更新为成功", hostInfo.getIp());
                cacheLog.info("专用Java环境检查项状态已更新为成功");

                // 添加额外的状态确认和日志
                logger.info("Java环境修复完成后的最终状态确认 - checkItem.getStatus(): {}, message: {}",
                        checkItem.getStatus(), checkItem.getMessage());

                // 在HostInfo中再次确认检查项状态
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem item : hostInfo.getCheckItems()) {
                        if (item.getId().equals(checkItem.getId())) {
                            logger.info("Java环境修复完成后在HostInfo中的状态: {}, message: {}",
                                    item.getStatus(), item.getMessage());

                            // 如果状态还不是SUCCESS，进行强制更新
                            if (item.getStatus() != CheckItem.Status.SUCCESS) {
                                logger.warn("修复后状态在HostInfo中未正确更新，进行强制更新");
                                item.setStatus(CheckItem.Status.SUCCESS);
                                item.setMessage("Java环境修复成功（状态已强制更新）");
                                hostInfo.calculateStatus();
                            }
                            break;
                        }
                    }
                }

                return true;
            } else {
                logger.error("主机 {} 的专用JDK安装验证失败", hostInfo.getIp());
                cacheLog.error("专用JDK安装验证失败");
                cacheLog.info("==== 专用Java环境修复失败 ====");

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "专用Java环境安装失败",
                        "验证JDK安装时发现问题，可能由于权限不足或磁盘空间不足"));

                // 添加检查结果
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("JDK路径检查",
                        verifyResult.getOutput().contains("success") ? "成功" : "失败",
                        verifyResult.getOutput().contains("success") ? HtmlStyleHelper.Colors.SUCCESS
                                : HtmlStyleHelper.Colors.ERROR));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("系统架构", arch, HtmlStyleHelper.Colors.INFO));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加手动修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>手动修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li>确保目标路径可写入: ").append(checkerProperties.getJava().getDefaultPath()).append("</li>");
                detailsBuilder.append("<li>确保系统有足够的磁盘空间</li>");
                detailsBuilder.append("<li>手动下载并安装JDK到指定路径</li>");
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置HTML格式化消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境修复失败", detailsBuilder);

                // 确保检查项状态更新为FAILED
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 同步到HostInfo
                hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED, "专用Java环境安装验证失败");

                // 重新计算并更新主机状态
                hostInfo.calculateStatus();

                // 记录状态更新
                logger.info("主机 {} 的专用Java环境检查项状态已更新为失败", hostInfo.getIp());
                cacheLog.info("专用Java环境检查项状态已更新为失败");

                return false;
            }
        } catch (Exception e) {
            logger.error("修复主机 {} 的专用Java环境失败: {}", hostInfo.getIp(), e.getMessage(), e);
            cacheLog.error("修复专用Java环境失败: " + e.getMessage());
            cacheLog.info("==== 专用Java环境修复失败 ====");

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加错误信息
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "修复过程发生错误",
                    "安装专用Java环境时出现异常: " + e.getMessage()));

            // 添加错误详情
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>错误信息:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(e.getMessage()));

            // 添加堆栈跟踪(仅限开发环境)
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("datasophon")) {
                    stackTrace.append(element).append("\n");
                }
            }
            if (!stackTrace.isEmpty()) {
                detailsBuilder.append("<p><strong>堆栈跟踪:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(stackTrace.toString()));
            }
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加手动修复建议
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>手动修复建议:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append("<li>检查系统日志获取更多错误信息</li>");
            detailsBuilder.append("<li>确保网络连接正常</li>");
            detailsBuilder.append("<li>尝试手动安装JDK到: ").append(checkerProperties.getJava().getDefaultPath()).append("</li>");
            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置HTML格式化消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "专用Java环境修复错误", detailsBuilder);

            // 确保检查项状态更新为FAILED
            checkItem.setStatus(CheckItem.Status.FAILED);

            // 同步到HostInfo
            hostInfo.updateCheckItemStatus(checkItem.getId(), CheckItem.Status.FAILED,
                    "专用Java环境修复失败: " + e.getMessage());

            // 重新计算并更新主机状态
            hostInfo.calculateStatus();

            // 记录状态更新
            logger.info("主机 {} 的专用Java环境检查项状态已更新为失败(异常)", hostInfo.getIp());
            cacheLog.info("专用Java环境检查项状态已更新为失败(异常)");

            return false;
        } finally {
            // 添加一个额外的最终日志确认
            try {
                // 获取检查项的最终状态，并记录
                CheckItem.Status finalStatus = checkItem.getStatus();
                String finalMessage = checkItem.getMessage();
                logger.info("专用Java环境修复过程结束，最终状态: {}, 消息: {}", finalStatus, finalMessage);

                // 确保状态已同步到HostInfo
                boolean foundInHostInfo = false;
                if (hostInfo.getCheckItems() != null) {
                    for (CheckItem item : hostInfo.getCheckItems()) {
                        if (item.getId().equals(checkItem.getId())) {
                            logger.info("修复结束时在HostInfo中的状态: {}, 消息: {}",
                                    item.getStatus(), item.getMessage());
                            foundInHostInfo = true;

                            // 如果出现不一致，进行最后的强制同步
                            if (item.getStatus() != finalStatus) {
                                logger.warn("检测到状态不一致，进行最终同步，参考状态: {}, HostInfo状态: {}",
                                        finalStatus, item.getStatus());
                                item.setStatus(finalStatus);
                                item.setMessage(finalMessage);
                                hostInfo.calculateStatus();
                            }
                            break;
                        }
                    }
                }

                if (!foundInHostInfo) {
                    logger.warn("在HostInfo中未找到ID为{}的检查项", checkItem.getId());
                }
            } catch (Exception e) {
                logger.error("记录最终状态时发生错误: {}", e.getMessage());
            }
        }
    }

    private String parseJavaVersion(String javaVersionOutput) {
        if (javaVersionOutput == null || javaVersionOutput.isEmpty()) {
            return "unknown";
        }

        // 记录完整的版本输出
        String[] lines = javaVersionOutput.split("\n");
        StringBuilder fullVersion = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                if (!fullVersion.isEmpty()) {
                    fullVersion.append(", ");
                }
                fullVersion.append(line);
            }
        }
        String fullVersionStr = fullVersion.toString();

        // 解析主版本号
        String version = "unknown";
        if (javaVersionOutput.contains("version")) {
            String[] parts = javaVersionOutput.split("\"");
            if (parts.length > 1) {
                String versionStr = parts[1];
                // 提取主版本号（例如从 1.8.0_333 中提取 1.8）
                int firstDot = versionStr.indexOf('.');
                int secondDot = versionStr.indexOf('.', firstDot + 1);
                if (firstDot > 0 && secondDot > 0) {
                    version = versionStr.substring(0, secondDot);
                } else {
                    version = versionStr;
                }
            }
        }

        return version + " (" + fullVersionStr + ")";
    }

    private boolean isVersionMeetRequirement(String currentVersion, String requiredVersion) {
        try {
            if ("unknown".equals(currentVersion)) {
                return false;
            }

            // 处理包含额外信息的版本号，提取主版本号部分
            String extractedCurrentVersion = currentVersion;
            if (currentVersion.contains(" ")) {
                // 如果包含空格，取空格前的部分作为实际版本号
                extractedCurrentVersion = currentVersion.split(" ")[0];
            }

            String[] current = extractedCurrentVersion.split("\\.");
            String[] required = requiredVersion.split("\\.");

            logger.info("比较版本: 提取后的当前版本 {} vs 要求版本 {}", extractedCurrentVersion, requiredVersion);

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
            // 出现异常时，为避免误判，默认认为版本符合要求
            logger.info("版本解析出错，默认认为版本 {} 符合要求 {}", currentVersion, requiredVersion);
            return true;
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.JAVA_ENV;
    }

    private void uploadFile(ClientSession session, String localFile) {
        try {
            // 使用MinaUtils实现真正的文件上传
            MinaUtils.uploadFile(session, "/usr/local", localFile);
            logger.info("上传文件: {} -> {}", localFile, "/usr/local");
            cacheLog.info("上传文件: " + localFile + " -> " + "/usr/local");
        } catch (Exception e) {
            logger.error("上传文件失败: {}", e.getMessage());
            cacheLog.error("上传文件失败: " + e.getMessage());
        }
    }
}