package com.datasophon.api.service.checker.checkers.selinux.os.centos;

import com.datasophon.api.service.checker.checkers.selinux.generic.GenericSELinuxChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS系统专用的SELinux检查器实现
 * 适用于CentOS 7/8等版本
 * @author 63588
 */
public class CentOSSELinuxChecker extends GenericSELinuxChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOSSELinuxChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("使用CentOS专用的SELinux检查器...");

        try {
            // 获取会话
            ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux检查: " + hostInfo.getIp();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            // 确保设置日志键
            // 获取集群ID
            Integer clusterId = hostInfo.getClusterId();
            selinuxChecker.setupLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 检查CentOS版本
            cacheLog.info("检查CentOS版本...");
            CommandResult versionResult = execCommand(session,
                    "cat /etc/centos-release 2>/dev/null || cat /etc/redhat-release");

            String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知版本";
            cacheLog.info("系统版本信息: {}", versionInfo);

            boolean isCentOS7 = versionInfo.contains("7.");
            boolean isCentOS8 = versionInfo.contains("8.");

            if (isCentOS7) {
                cacheLog.info("检测到CentOS 7系统");
            } else if (isCentOS8) {
                cacheLog.info("检测到CentOS 8系统");
            } else {
                cacheLog.info("检测到其他CentOS/RHEL系统，使用通用检查方法");
            }

            // CentOS专用的SELinux检查
            // 1. 使用sestatus命令获取详细信息
            cacheLog.info("使用sestatus命令检查SELinux详细状态...");
            CommandResult sestatusResult = execCommand(session, "sestatus");

            // 检查SELinux配置文件
            cacheLog.info("检查SELinux配置文件...");
            CommandResult configResult = execCommand(session, "cat /etc/selinux/config | grep -E '^SELINUX='");

            // 分析sestatus输出，获取SELinux状态和模式
            String selinuxStatus = "Unknown";
            String selinuxMode = "Unknown";
            String configPolicy = "Unknown";

            if (sestatusResult.isSuccess()) {
                String sestatusOutput = sestatusResult.getOutput().trim();

                // 解析状态行
                if (sestatusOutput.contains("SELinux status:")) {
                    String[] lines = sestatusOutput.split("\n");
                    for (String line : lines) {
                        if (line.contains("SELinux status:")) {
                            selinuxStatus = line.split("SELinux status:")[1].trim();
                        } else if (line.contains("Current mode:")) {
                            selinuxMode = line.split("Current mode:")[1].trim();
                        }
                    }
                }

                cacheLog.info("SELinux状态: {}, 当前模式: {}", selinuxStatus, selinuxMode);
            } else {
                cacheLog.warn("sestatus命令执行失败: {}", sestatusResult.getErrorOrOutput());
            }

            // 解析配置文件
            if (configResult.isSuccess()) {
                String configOutput = configResult.getOutput().trim();
                if (configOutput.contains("SELINUX=")) {
                    configPolicy = configOutput.split("SELINUX=")[1].trim();
                }
                cacheLog.info("SELinux配置文件设置: {}", configPolicy);
            } else {
                cacheLog.warn("无法读取SELinux配置: {}", configResult.getErrorOrOutput());
            }

            // 判断检查结果
            boolean isDisabled = "disabled".equalsIgnoreCase(selinuxStatus);
            boolean isPermissive = "permissive".equalsIgnoreCase(selinuxMode);

            if (isDisabled || isPermissive) {
                // SELinux已禁用或处于宽容模式，检查通过
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加状态信息
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "SELinux状态", selinuxStatus,
                        isDisabled ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.INFO));

                if (!isDisabled) {
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "SELinux模式", selinuxMode,
                            HtmlStyleHelper.Colors.SUCCESS));
                }

                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "配置文件设置", configPolicy,
                        "disabled".equalsIgnoreCase(configPolicy) ? HtmlStyleHelper.Colors.SUCCESS
                                : "permissive".equalsIgnoreCase(configPolicy) ? HtmlStyleHelper.Colors.INFO
                                        : HtmlStyleHelper.Colors.WARNING));

                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加系统版本信息
                if (isCentOS7 || isCentOS8) {
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "系统版本", versionInfo, HtmlStyleHelper.Colors.INFO));
                }

                // 添加命令执行结果
                detailsBuilder.append("<p><strong>命令执行结果:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "$ sestatus\n" + (sestatusResult.isSuccess() ? sestatusResult.getOutput().trim() : "命令执行失败") +
                                "\n\n$ cat /etc/selinux/config | grep -E '^SELINUX='\n" +
                                (configResult.isSuccess() ? configResult.getOutput().trim() : "命令执行失败")));

                // 添加成功信息
                String successMessage = isDisabled ? "SELinux已完全禁用，不会干扰系统运行。"
                        : "SELinux处于宽容模式(Permissive)，会记录但不会阻止操作，可以正常运行系统。";

                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "SELinux检查通过", successMessage));

                // 如果是宽容模式，添加建议
                if (isPermissive) {
                    detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                            "建议完全禁用SELinux",
                            "当前SELinux处于宽容模式，建议修改配置文件设置为disabled并重启系统，以完全禁用SELinux。"));
                }

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());
                return checkItem;
            } else {
                // 使用通用检查方法进行进一步检查
                cacheLog.info("SELinux状态需要进一步检查，使用通用检查方法...");
                return super.check(hostInfo, checkItem, cacheLog);
            }

        } catch (Exception e) {
            String errorMsg = "检查CentOS系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("使用CentOS专用的SELinux修复方法...");

        try {
            // 获取会话
            ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux修复: " + hostInfo.getIp();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 确保设置日志键
            // 获取集群ID
            Integer clusterId = hostInfo.getClusterId();
            selinuxChecker.setupLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 检查CentOS版本
            cacheLog.info("检查CentOS版本...");
            CommandResult versionResult = execCommand(session,
                    "cat /etc/centos-release 2>/dev/null || cat /etc/redhat-release");

            String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知版本";
            cacheLog.info("系统版本信息: {}", versionInfo);

            // CentOS专用的修复方法
            cacheLog.info("使用CentOS专用方法修复SELinux...");

            // 临时设置SELinux为宽容模式
            cacheLog.info("临时设置SELinux为宽容模式...");
            CommandResult setenforceResult = execCommand(session, "setenforce 0");

            if (!setenforceResult.isSuccess()) {
                cacheLog.warn("临时设置SELinux失败: {}", setenforceResult.getErrorOrOutput());
                // 继续尝试修改配置文件
            } else {
                cacheLog.info("成功临时设置SELinux为宽容模式");
            }

            // 永久禁用SELinux - 修改配置文件
            cacheLog.info("修改SELinux配置文件以永久禁用...");
            CommandResult sedResult = execCommand(session,
                    "sudo sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config");

            if (!sedResult.isSuccess()) {
                cacheLog.error("修改SELinux配置文件失败: {}", sedResult.getErrorOrOutput());

                // 创建HTML详细信息构建器

                // 添加警告信息

                String detailsBuilder = HtmlStyleHelper.generateWarningAlert(
                        "SELinux配置修改失败",
                        "无法修改SELinux配置文件，请手动修改。") +

                // 添加手动修复指南
                        HtmlStyleHelper.beginGroup() +
                        "<p><strong>手动修复步骤:</strong></p>" +
                        "<ol style='padding-left:20px;margin-bottom:15px'>" +
                        "<li style='margin-bottom:5px'>编辑SELinux配置文件:</li>" +
                        HtmlStyleHelper.generateCodeBlock("sudo vi /etc/selinux/config") +
                        "<li style='margin-bottom:5px'>将SELINUX=行修改为:</li>" +
                        HtmlStyleHelper.generateCodeBlock("SELINUX=disabled") +
                        "<li style='margin-bottom:5px'>保存并重启系统:</li>" +
                        HtmlStyleHelper.generateCodeBlock("sudo reboot") +
                        "</ol>" +
                        HtmlStyleHelper.endGroup();

                // 设置消息
                checkItem.setMessage(detailsBuilder);
                return false;
            }

            // 验证配置修改
            cacheLog.info("验证SELinux配置文件修改...");
            CommandResult verifyResult = execCommand(session, "grep '^SELINUX=' /etc/selinux/config");

            boolean configModified = verifyResult.isSuccess() &&
                    verifyResult.getOutput().trim().contains("SELINUX=disabled");

            if (!configModified) {
                cacheLog.warn("配置文件验证失败，可能未正确修改: {}",
                        verifyResult.isSuccess() ? verifyResult.getOutput().trim() : verifyResult.getErrorOrOutput());
            }

            // 创建HTML详细信息构建器

            // 添加成功信息

            String detailsBuilder = HtmlStyleHelper.generateSuccessAlert(
                    "SELinux修复完成",
                    "SELinux已临时设置为宽容模式，并已将配置文件修改为禁用状态。") +

            // 添加当前状态
                    HtmlStyleHelper.beginGroup() +
                    "<p><strong>当前SELinux状态:</strong></p>" +
                    HtmlStyleHelper.generatePropertyRow(
                            "临时状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS)
                    +
                    HtmlStyleHelper.generatePropertyRow(
                            "永久状态", configModified ? "已设置为禁用" : "配置修改未验证",
                            configModified ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.WARNING)
                    +
                    HtmlStyleHelper.endGroup() +

                    // 添加重启通知
                    HtmlStyleHelper.generateNoteAlert(
                            "重要提示",
                            "SELinux配置修改需要重启系统后才能完全生效。在重启前，系统将以宽容模式(Permissive)运行SELinux。");

            // 设置消息
            checkItem.setMessage(detailsBuilder);

            return true;
        } catch (Exception e) {
            String errorMsg = "修复CentOS系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }
    }
}