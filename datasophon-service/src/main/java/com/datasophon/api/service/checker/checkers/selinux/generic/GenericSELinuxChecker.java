package com.datasophon.api.service.checker.checkers.selinux.generic;

import com.datasophon.api.service.checker.checkers.selinux.SELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.SELinuxCheckerStrategy;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 通用SELinux检查器实现
 * 作为不同操作系统特定实现的基类
 */
public class GenericSELinuxChecker implements SELinuxCheckerStrategy {

    private static final Logger log = LoggerFactory.getLogger(GenericSELinuxChecker.class);

    /**
     * -- SETTER --
     * 设置SSH连接池管理器
     *
     */
    @Setter
    @Autowired
    protected SshConnectionPoolManager sshConnectionPoolManager;
    protected final SELinuxChecker selinuxChecker;

    @Getter
    @Setter
    private OsDistribution supportedOs;

    @Getter
    @Setter
    private String versionPrefix;

    public GenericSELinuxChecker() {
        // 创建SELinuxChecker实例
        this.selinuxChecker = new SELinuxChecker();
    }

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        try {
            cacheLog.info("==== SELinux检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 更新状态为正在检查SELinux状态
            checkItem.setMessage("正在检查SELinux状态...");

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

            // 检查SELinux状态
            cacheLog.info("检查SELinux状态...");

            // 首先检查getenforce命令是否存在
            CommandResult checkGetenforce = execCommand(session, "command -v getenforce");
            boolean hasGetenforce = checkGetenforce.isSuccess() && !checkGetenforce.getOutput().trim().isEmpty();

            String selinuxStatus = "Unknown";
            CommandResult result = null;

            if (hasGetenforce) {
                // 如果getenforce命令存在，使用它获取状态
                result = execCommand(session, "getenforce");

                if (result.isSuccess()) {
                    selinuxStatus = result.getOutput().trim();
                    cacheLog.info("SELinux当前状态(通过getenforce): " + selinuxStatus);
                } else {
                    cacheLog.warn("getenforce命令执行失败: %s", result.getErrorOrOutput());
                }
            } else {
                cacheLog.warn("getenforce命令不存在，使用替代方法检查SELinux...");
            }

            // 如果getenforce命令不存在或执行失败，尝试其他方法
            if (!hasGetenforce || !result.isSuccess()) {
                // 检查是否安装了SELinux
                CommandResult selinuxCheck = execCommand(session,
                        "ls -la /sys/fs/selinux 2>/dev/null || echo 'Not Found'");
                boolean selinuxFSExists = selinuxCheck.isSuccess() && !selinuxCheck.getOutput().contains("Not Found");

                if (!selinuxFSExists) {
                    // 如果SELinux文件系统不存在，说明SELinux未安装
                    cacheLog.info("SELinux文件系统不存在，SELinux未安装，视为Disabled状态");
                    selinuxStatus = "Disabled";
                }
            }

            // 增加sestatus命令检查以提高准确性
            CommandResult sestatusResult;
            try {
                cacheLog.info("使用sestatus命令进一步验证SELinux状态...");
                sestatusResult = execCommand(session, "sestatus 2>/dev/null || echo 'Command not found'");

                if (sestatusResult.isSuccess() && !sestatusResult.getOutput().contains("Command not found")) {
                    String sestatusOutput = sestatusResult.getOutput().trim();
                    cacheLog.info("sestatus命令输出: \n{}", sestatusOutput);

                    // 解析sestatus输出以获取状态信息
                    if (sestatusOutput.contains("SELinux status") && sestatusOutput.contains("disabled")) {
                        cacheLog.info("通过sestatus确认SELinux已禁用");
                        selinuxStatus = "Disabled";
                    } else if (sestatusOutput.contains("SELinux status") && sestatusOutput.contains("enabled")) {
                        // 进一步检查是否是permissive模式
                        if (sestatusOutput.contains("permissive")) {
                            cacheLog.info("通过sestatus确认SELinux为宽容模式(Permissive)");
                            selinuxStatus = "Permissive";
                        } else if (sestatusOutput.contains("enforcing")) {
                            cacheLog.info("通过sestatus确认SELinux为强制模式(Enforcing)");
                            selinuxStatus = "Enforcing";
                        }
                    }
                } else {
                    cacheLog.warn("sestatus命令不可用，这可能表明SELinux未安装");
                }
            } catch (Exception e) {
                cacheLog.warn("执行sestatus命令时出错: {}", e.getMessage());
            }

            // 如果状态仍然未知，检查配置文件
            if ("Unknown".equals(selinuxStatus)) {
                // 检查配置文件是否存在
                CommandResult configExists = execCommand(session,
                        "ls -la /etc/selinux/config 2>/dev/null || echo 'Not Found'");
                if (configExists.isSuccess() && !configExists.getOutput().contains("Not Found")) {
                    // 检查SELinux配置文件
                    cacheLog.info("通过配置文件检查SELinux状态...");
                    CommandResult configResult = execCommand(session,
                            "cat /etc/selinux/config 2>/dev/null | grep ^SELINUX=");

                    if (configResult.isSuccess() && !configResult.getOutput().isEmpty()) {
                        String selinuxConfig = configResult.getOutput().trim();
                        cacheLog.info("SELinux配置: " + selinuxConfig);

                        if (selinuxConfig.contains("SELINUX=disabled")) {
                            cacheLog.info("根据配置文件，SELinux被设置为禁用状态");
                            selinuxStatus = "Disabled";
                        } else if (selinuxConfig.contains("SELINUX=permissive")) {
                            cacheLog.info("根据配置文件，SELinux被设置为宽容模式");
                            selinuxStatus = "Permissive";
                        } else if (selinuxConfig.contains("SELINUX=enforcing")) {
                            cacheLog.info("根据配置文件，SELinux被设置为强制模式");
                            selinuxStatus = "Enforcing";
                        }
                    } else {
                        cacheLog.warn("无法读取SELinux配置或配置不包含SELINUX设置");
                    }
                } else {
                    cacheLog.info("SELinux配置文件不存在，视为未安装SELinux，标记为Disabled");
                    selinuxStatus = "Disabled";
                }
            }

            // 如果尝试了所有方法后状态仍未知，假设为禁用状态
            if ("Unknown".equals(selinuxStatus)) {
                cacheLog.info("无法确定SELinux状态，基于主流Linux发行版默认配置，假设SELinux已禁用");
                selinuxStatus = "Disabled";
            }

            cacheLog.info("最终确定的SELinux状态: " + selinuxStatus);

            // 检查SELinux配置文件（仅用于显示信息）
            cacheLog.info("检查SELinux配置文件...");
            CommandResult configResult = execCommand(session,
                    "cat /etc/selinux/config 2>/dev/null | grep ^SELINUX= || echo 'No config file'");
            String selinuxConfig = configResult.isSuccess() ? configResult.getOutput().trim() : "无法读取";

            if (configResult.isSuccess() && !configResult.getOutput().contains("No config file")) {
                cacheLog.info("SELinux配置: " + selinuxConfig);
            } else {
                cacheLog.info("SELinux配置文件不存在或为空");
            }

            // 更新状态为正在分析SELinux状态
            checkItem.setMessage("正在分析SELinux状态: " + selinuxStatus);

            // 判断状态
            boolean isOk = "Disabled".equalsIgnoreCase(selinuxStatus) || "Permissive".equalsIgnoreCase(selinuxStatus);

            if (isOk) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加SELinux状态信息组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());

                // 添加当前状态信息
                String statusColor = "Disabled".equalsIgnoreCase(selinuxStatus) ? HtmlStyleHelper.Colors.SUCCESS
                        : HtmlStyleHelper.Colors.CYAN;
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "SELinux当前状态", selinuxStatus, statusColor));

                // 添加配置文件信息
                if (configResult.isSuccess()) {
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "SELinux配置文件设置", selinuxConfig, HtmlStyleHelper.Colors.INFO));
                }

                // 添加命令执行结果
                detailsBuilder.append("<p><strong>命令执行结果:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "$ getenforce\n" + selinuxStatus +
                                (configResult.isSuccess()
                                        ? "\n\n$ cat /etc/selinux/config | grep ^SELINUX=\n" + selinuxConfig
                                        : "")));

                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加成功信息
                String successMsg = "Disabled".equalsIgnoreCase(selinuxStatus) ? "SELinux已完全禁用，不会干扰系统运行。"
                        : "SELinux处于宽容模式(Permissive)，会记录但不会阻止操作，可以正常运行系统。";

                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "SELinux检查通过", successMsg));

                // 如果是宽容模式，添加建议信息
                if ("Permissive".equalsIgnoreCase(selinuxStatus)) {
                    detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                            "建议完全禁用SELinux",
                            "当前SELinux处于宽容模式，建议通过修改配置完全禁用SELinux并重启系统，以避免潜在问题。"));
                }

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());
                cacheLog.info("SELinux检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加SELinux状态信息组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());

                // 添加当前状态信息
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "SELinux当前状态", selinuxStatus, HtmlStyleHelper.Colors.ERROR));

                // 添加配置文件信息
                if (configResult.isSuccess()) {
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "SELinux配置文件设置", selinuxConfig, HtmlStyleHelper.Colors.INFO));
                }

                // 添加命令执行结果
                detailsBuilder.append("<p><strong>命令执行结果:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "$ getenforce\n" + selinuxStatus +
                                (configResult.isSuccess()
                                        ? "\n\n$ cat /etc/selinux/config | grep ^SELINUX=\n" + selinuxConfig
                                        : "")));

                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加警告信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "SELinux检查未通过",
                        "SELinux处于强制模式(Enforcing)，这可能会阻止某些操作，影响系统正常运行。"));

                // 添加修复建议
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>修复建议:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li style='margin-bottom:5px'>临时禁用SELinux (重启后失效):</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo setenforce 0"));

                detailsBuilder.append("<li style='margin-bottom:5px'>永久禁用SELinux (需要重启):</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        """
                                sudo sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config
                                # 完成后重启系统
                                sudo reboot"""));
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());
                cacheLog.info("SELinux检查未通过: 当前为强制模式");
            }

        } catch (Exception e) {
            String errorMsg = "检查SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
        } finally {
            cacheLog.info("==== SELinux检查结束 ====");
        }
        return checkItem;
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        try {
            cacheLog.info("==== 开始修复SELinux配置 ====");

            // 更新状态为正在设置SELinux为宽容模式
            checkItem.setMessage("正在设置SELinux为宽容模式...");

            // 获取会话
            ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux修复: " + hostInfo.getIp();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 检查系统上是否有setenforce命令
            cacheLog.info("检查系统支持的SELinux禁用方式...");
            CommandResult checkCmd = execCommand(session, "command -v setenforce");
            boolean hasSetenforce = checkCmd.isSuccess() && !checkCmd.getOutput().trim().isEmpty();

            // 先尝试临时设置为宽容模式
            boolean tempFixSuccess;
            CommandResult setenforceResult;

            if (hasSetenforce) {
                // 使用setenforce命令（适用于CentOS, Kylin等）
                cacheLog.info("使用setenforce命令设置SELinux为宽容模式...");
                setenforceResult = execCommand(session, "setenforce 0");
            } else {
                // 尝试使用echo方法（适用于某些Ubuntu, Kylin V10等）
                cacheLog.info("尝试使用echo方法设置SELinux为宽容模式...");
                setenforceResult = execCommand(session,
                        "[ -f /sys/fs/selinux/enforce ] && echo 0 | sudo tee /sys/fs/selinux/enforce");
            }
            tempFixSuccess = setenforceResult.isSuccess();

            if (!tempFixSuccess) {
                cacheLog.error("设置SELinux临时状态失败: %s", setenforceResult.getErrorOrOutput());

                // 创建HTML详细信息构建器

                // 添加错误信息

                String detailsBuilder = HtmlStyleHelper.generateWarningAlert(
                        "临时设置SELinux失败",
                        "无法临时设置SELinux为宽容模式: " + setenforceResult.getErrorOrOutput()) +

                        // 添加手动修复指南
                        HtmlStyleHelper.beginGroup() +
                        "<p><strong>手动修复步骤:</strong></p>" +
                        "<ol style='padding-left:20px;margin-bottom:15px'>" +
                        "<li style='margin-bottom:5px'>以root权限临时禁用SELinux (选择适合的方法):</li>" +
                        HtmlStyleHelper.generateCodeBlock(
                                """
                                        # CentOS/RHEL/Kylin V4方法:
                                        sudo setenforce 0
                                        
                                        # Ubuntu/Kylin V10方法:
                                        [ -f /sys/fs/selinux/enforce ] && echo 0 | sudo tee /sys/fs/selinux/enforce""") +
                        "<li style='margin-bottom:5px'>修改配置文件永久禁用SELinux:</li>" +
                        HtmlStyleHelper.generateCodeBlock(
                                """
                                        sudo vi /etc/selinux/config
                                        
                                        # 修改以下行
                                        SELINUX=disabled""") +
                        "<li style='margin-bottom:5px'>重启系统使永久设置生效:</li>" +
                        HtmlStyleHelper.generateCodeBlock("sudo reboot") +
                        "</ol>" +
                        HtmlStyleHelper.endGroup();

                // 设置消息
                checkItem.setMessage(detailsBuilder);
                return false;
            }
            cacheLog.info("已临时设置SELinux为宽容模式");

            // 更新状态为正在修改SELinux配置文件
            checkItem.setMessage("正在修改SELinux配置文件...");

            // 检查配置文件是否存在
            CommandResult configCheck = execCommand(session, "[ -f /etc/selinux/config ] && echo 'exists'");
            boolean configExists = configCheck.isSuccess() && configCheck.getOutput().trim().equals("exists");

            boolean configFixSuccess;
            CommandResult sedResult;

            if (configExists) {
                // 修改配置文件
                cacheLog.info("修改SELinux配置文件...");

                // 先检查配置文件中是否有SELINUX=配置项
                CommandResult grepResult = execCommand(session, "grep -E '^SELINUX=' /etc/selinux/config");

                if (grepResult.isSuccess() && !grepResult.getOutput().trim().isEmpty()) {
                    // 修改已存在的配置项
                    sedResult = execCommand(session, "sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config");
                } else {
                    // 添加配置项
                    sedResult = execCommand(session, "echo 'SELINUX=disabled' | sudo tee -a /etc/selinux/config");
                }
            } else {
                // 创建配置文件
                cacheLog.info("SELinux配置文件不存在，创建新的配置文件...");
                String createConfigCmd = "mkdir -p /etc/selinux && " +
                        "echo '# This file controls the state of SELinux on the system.' | sudo tee /etc/selinux/config && "
                        +
                        "echo 'SELINUX=disabled' | sudo tee -a /etc/selinux/config && " +
                        "echo 'SELINUXTYPE=targeted' | sudo tee -a /etc/selinux/config";
                sedResult = execCommand(session, createConfigCmd);
            }
            configFixSuccess = sedResult.isSuccess();

            if (!configFixSuccess) {
                cacheLog.error("修改SELinux配置文件失败: %s", sedResult.getErrorOrOutput());

                // 创建HTML详细信息构建器

                // 添加错误和部分成功信息

                String detailsBuilder = HtmlStyleHelper.generateWarningAlert(
                        "SELinux配置文件修改失败",
                        "SELinux已临时设置为宽容模式，但无法修改配置文件进行永久设置: " + sedResult.getErrorOrOutput()) +

                        // 添加当前状态说明
                        HtmlStyleHelper.beginGroup() +
                        "<p><strong>当前SELinux状态:</strong></p>" +
                        HtmlStyleHelper.generatePropertyRow(
                                "临时状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS) +
                        HtmlStyleHelper.generatePropertyRow(
                                "永久状态", "未修改", HtmlStyleHelper.Colors.WARNING) +
                        HtmlStyleHelper.endGroup() +

                        // 添加手动修复指南
                        HtmlStyleHelper.beginGroup() +
                        "<p><strong>手动修复步骤:</strong></p>" +
                        "<ol style='padding-left:20px;margin-bottom:15px'>" +
                        "<li style='margin-bottom:5px'>以root权限修改配置文件:</li>" +
                        HtmlStyleHelper.generateCodeBlock(
                                """
                                        sudo mkdir -p /etc/selinux
                                        sudo vi /etc/selinux/config
                                        
                                        # 添加以下行
                                        SELINUX=disabled
                                        SELINUXTYPE=targeted""") +
                        "<li style='margin-bottom:5px'>重启系统使永久设置生效:</li>" +
                        HtmlStyleHelper.generateCodeBlock("sudo reboot") +
                        "</ol>" +
                        HtmlStyleHelper.endGroup();

                // 设置消息
                checkItem.setMessage(detailsBuilder);
                return false;
            }
            cacheLog.info("已修改SELinux配置文件，设置为disabled");

            // 验证配置文件修改
            CommandResult verifyResult = execCommand(session, "cat /etc/selinux/config | grep ^SELINUX=");
            if (verifyResult.isSuccess() && verifyResult.getOutput().trim().contains("SELINUX=disabled")) {
                cacheLog.info("验证配置文件修改成功: {}", verifyResult.getOutput().trim());
            } else {
                cacheLog.warn("验证配置文件修改结果不确定: {}",
                        verifyResult.isSuccess() ? verifyResult.getOutput().trim() : verifyResult.getErrorOrOutput());
            }

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加成功信息
            detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                    "SELinux已成功修复",
                    "SELinux已临时设置为宽容模式，并已修改配置文件为永久禁用状态。"));

            // 添加当前状态说明
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>当前SELinux状态:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "临时状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "永久状态", "已禁用(Disabled)", HtmlStyleHelper.Colors.SUCCESS));

            // 添加验证结果
            if (verifyResult.isSuccess()) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "配置文件验证", verifyResult.getOutput().trim(),
                        verifyResult.getOutput().trim().contains("SELINUX=disabled") ? HtmlStyleHelper.Colors.SUCCESS
                                : HtmlStyleHelper.Colors.WARNING));
            }

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加重启提示
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                    "生效须知",
                    "永久禁用SELinux的配置已生效，但需要重启系统才能完全应用。当前会话中SELinux已设置为宽容模式，不会阻止系统操作。"));

            // 设置消息
            checkItem.setMessage(detailsBuilder.toString());
            cacheLog.info("SELinux修复完成");
            return true;

        } catch (Exception e) {
            String errorMsg = "修复SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        } finally {
            cacheLog.info("==== SELinux修复结束 ====");
        }
    }

    /**
     * 执行命令并获取结果
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws InterruptedException 如果命令执行被中断
     */
    protected CommandResult execCommand(ClientSession session, String command) throws InterruptedException {
        if (selinuxChecker != null) {
            // 命令执行前记录一下当前的命令
            log.debug("执行命令: {}", command);

            // 调用SELinuxChecker的execCommand方法
            // UbuntuSELinuxChecker等子类应该已经设置了正确的日志键
            return selinuxChecker.execCommand(session, command);
        }
        // 如果没有selinuxChecker，返回错误结果
        // 根据CommandResult构造函数定义，参数顺序为：output, error, exitCode
        return new CommandResult("", "SELinux检查器未初始化", 1);
    }
}