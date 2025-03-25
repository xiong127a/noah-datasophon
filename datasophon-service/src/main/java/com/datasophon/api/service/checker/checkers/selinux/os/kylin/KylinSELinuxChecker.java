package com.datasophon.api.service.checker.checkers.selinux.os.kylin;

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
 * 麒麟操作系统专用的SELinux检查器实现
 * 适用于Kylin V4、V10等版本
 */
public class KylinSELinuxChecker extends GenericSELinuxChecker {

    private static final Logger log = LoggerFactory.getLogger(KylinSELinuxChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("使用麒麟操作系统专用的SELinux检查器...");

        try {
            // 获取会话
            ClientSession session = hostInfo.getExternalSession();
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux检查: " + hostInfo.getHostname();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            // 确保设置日志键
            if (selinuxChecker != null) {
                // 获取集群ID
                Integer clusterId = hostInfo.getClusterId();
                selinuxChecker.setupLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            }

            // 检查麒麟版本
            cacheLog.info("检查麒麟版本信息...");
            CommandResult versionResult = execCommand(session, "cat /etc/*-release | grep -i 'kylin\\|version'");

            String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知版本";
            cacheLog.info("麒麟系统版本信息: {}", versionInfo);

            boolean isKylinV4 = versionInfo.toLowerCase().contains("kylin") && versionInfo.contains("4");
            boolean isKylinV10 = versionInfo.toLowerCase().contains("kylin") && versionInfo.contains("10");

            // 判断麒麟版本，V4基于CentOS，V10基于Ubuntu
            if (isKylinV4) {
                cacheLog.info("检测到麒麟V4系统，基于CentOS架构");

                // 使用CentOS方式检查SELinux
                return checkCentOSBasedKylin(hostInfo, checkItem, cacheLog, session, versionInfo);
            } else if (isKylinV10) {
                cacheLog.info("检测到麒麟V10系统，基于Ubuntu架构");

                // 使用Ubuntu方式检查SELinux
                return checkUbuntuBasedKylin(hostInfo, checkItem, cacheLog, session, versionInfo);
            } else {
                cacheLog.info("无法确定麒麟版本或非标准麒麟版本，使用通用检查方法");
                // 使用通用方法
                return super.check(hostInfo, checkItem, cacheLog);
            }

        } catch (Exception e) {
            String errorMsg = "检查麒麟系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }
    }

    /**
     * 检查基于CentOS的麒麟V4系统
     */
    private CheckItem checkCentOSBasedKylin(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog,
            ClientSession session, String versionInfo) throws InterruptedException {
        // 使用CentOS方式检查SELinux
        cacheLog.info("使用CentOS方式检查麒麟V4系统SELinux状态...");

        // 1. 使用sestatus命令获取详细信息
        cacheLog.info("使用sestatus命令检查SELinux详细状态...");
        CommandResult sestatusResult = execCommand(session, "sestatus");

        // 2. 检查SELinux配置文件
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

            // 尝试使用getenforce命令
            CommandResult getenforceResult = execCommand(session, "getenforce");
            if (getenforceResult.isSuccess()) {
                String output = getenforceResult.getOutput().trim();
                cacheLog.info("getenforce命令输出: {}", output);

                if ("Disabled".equalsIgnoreCase(output)) {
                    selinuxStatus = "disabled";
                } else if ("Enforcing".equalsIgnoreCase(output)) {
                    selinuxStatus = "enabled";
                    selinuxMode = "enforcing";
                } else if ("Permissive".equalsIgnoreCase(output)) {
                    selinuxStatus = "enabled";
                    selinuxMode = "permissive";
                }
            }
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
                    "系统类型", "麒麟V4 (基于CentOS)", HtmlStyleHelper.Colors.INFO));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "SELinux状态", selinuxStatus,
                    isDisabled ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.INFO));

            if (!isDisabled) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "SELinux模式", selinuxMode,
                        isPermissive ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR));
            }

            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "配置文件设置", configPolicy,
                    "disabled".equalsIgnoreCase(configPolicy) ? HtmlStyleHelper.Colors.SUCCESS
                            : "permissive".equalsIgnoreCase(configPolicy) ? HtmlStyleHelper.Colors.INFO
                                    : HtmlStyleHelper.Colors.WARNING));

            detailsBuilder.append(HtmlStyleHelper.endGroup());

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
    }

    /**
     * 检查基于Ubuntu的麒麟V10系统
     */
    private CheckItem checkUbuntuBasedKylin(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog,
            ClientSession session, String versionInfo) throws InterruptedException {
        // 使用Ubuntu方式检查SELinux
        cacheLog.info("使用Ubuntu方式检查麒麟V10系统SELinux状态...");

        // 优先检查AppArmor状态
        cacheLog.info("在麒麟V10系统上检查AppArmor状态（基于Ubuntu的麒麟V10默认使用AppArmor而非SELinux）...");
        CommandResult apparmorStatusResult = execCommand(session,
                "command -v aa-status && aa-status || echo 'AppArmor未安装'");

        String apparmorStatus = apparmorStatusResult.isSuccess() ? apparmorStatusResult.getOutput().trim() : "未知";
        boolean apparmorEnabled = apparmorStatus.contains("apparmor module is loaded") ||
                apparmorStatus.contains("profiles are loaded");

        if (apparmorEnabled) {
            cacheLog.info("检测到麒麟V10系统上AppArmor已启用");
        }

        // 检查SELinux工具是否安装
        cacheLog.info("检查麒麟V10系统上SELinux相关工具是否安装...");
        CommandResult selinuxUtilsResult = execCommand(session, "dpkg -l | grep -E 'selinux-utils|selinux-basics'");

        // 检查相关内核模块
        cacheLog.info("检查SELinux内核模块...");
        CommandResult selinuxModuleResult = execCommand(session, "lsmod | grep selinux || echo '未加载SELinux模块'");

        // 如果没有安装SELinux工具，则直接通过检查
        if (!selinuxUtilsResult.isSuccess() || selinuxUtilsResult.getOutput().trim().isEmpty()) {
            cacheLog.info("麒麟V10系统上未安装SELinux工具包，检查通过。");
            checkItem.setStatus(CheckItem.Status.SUCCESS);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加状态信息
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "系统类型", "麒麟V10 (基于Ubuntu)", HtmlStyleHelper.Colors.INFO));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "SELinux状态", "未安装", HtmlStyleHelper.Colors.SUCCESS));

            // 添加AppArmor信息
            if (apparmorEnabled) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "AppArmor状态", "已启用", HtmlStyleHelper.Colors.INFO));
                detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                        "麒麟V10安全说明",
                        "麒麟V10系统默认使用AppArmor作为安全模块，而非SELinux。AppArmor已正常启用，无需安装或配置SELinux。"));
            }

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加命令执行结果
            detailsBuilder.append("<p><strong>命令执行结果:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    "$ dpkg -l | grep -E 'selinux-utils|selinux-basics'\n" +
                            (selinuxUtilsResult.isSuccess() ? selinuxUtilsResult.getOutput().trim()
                                    : "未找到SELinux工具包")));

            // 添加成功信息
            detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                    "SELinux检查通过", "麒麟V10系统上未安装SELinux，不会干扰系统运行。" +
                            (apparmorEnabled ? " 系统已使用默认的AppArmor安全模块。" : "")));

            // 设置消息
            checkItem.setMessage(detailsBuilder.toString());
            return checkItem;
        }

        // 如果安装了SELinux工具但模块未加载，可能是安装但未启用
        boolean selinuxModuleLoaded = selinuxModuleResult.isSuccess() &&
                !selinuxModuleResult.getOutput().trim().contains("未加载SELinux模块");

        if (!selinuxModuleLoaded) {
            cacheLog.info("麒麟V10系统上已安装SELinux工具包，但SELinux模块未加载，SELinux有效状态为禁用。");
            checkItem.setStatus(CheckItem.Status.SUCCESS);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加状态信息
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "系统类型", "麒麟V10 (基于Ubuntu)", HtmlStyleHelper.Colors.INFO));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "SELinux工具", "已安装", HtmlStyleHelper.Colors.INFO));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "SELinux模块", "未加载", HtmlStyleHelper.Colors.SUCCESS));
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "SELinux状态", "有效禁用", HtmlStyleHelper.Colors.SUCCESS));

            // 添加AppArmor信息
            if (apparmorEnabled) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "AppArmor状态", "已启用", HtmlStyleHelper.Colors.INFO));
            }

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加命令执行结果
            detailsBuilder.append("<p><strong>命令执行结果:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    "$ dpkg -l | grep -E 'selinux-utils|selinux-basics'\n" +
                            selinuxUtilsResult.getOutput().trim() + "\n\n" +
                            "$ lsmod | grep selinux\n" +
                            selinuxModuleResult.getOutput().trim()));

            // 添加成功信息
            detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                    "SELinux检查通过",
                    "麒麟V10系统上虽已安装SELinux工具，但模块未加载，SELinux处于有效禁用状态。" +
                            (apparmorEnabled ? " 系统已使用默认的AppArmor安全模块。" : "")));

            // 添加可选建议
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                    "建议操作",
                    "如无特殊需求，建议在麒麟V10系统上保持SELinux未加载状态，避免与AppArmor产生冲突。若需完全移除SELinux工具包，可执行: sudo apt purge selinux-utils selinux-basics"));

            // 设置消息
            checkItem.setMessage(detailsBuilder.toString());
            return checkItem;
        }

        // 如果安装了SELinux工具且模块已加载，则执行标准检查
        cacheLog.info("检测到麒麟V10系统上安装了SELinux且模块已加载，执行标准检查...");
        return super.check(hostInfo, checkItem, cacheLog);
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("使用麒麟操作系统专用的SELinux修复方法...");

        try {
            // 获取会话
            ClientSession session = hostInfo.getExternalSession();
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux修复: " + hostInfo.getHostname();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 确保设置日志键
            if (selinuxChecker != null) {
                // 获取集群ID
                Integer clusterId = hostInfo.getClusterId();
                selinuxChecker.setupLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
            }

            // 检查麒麟版本
            cacheLog.info("检查麒麟版本信息...");
            CommandResult versionResult = execCommand(session, "cat /etc/*-release | grep -i 'kylin\\|version'");

            String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知版本";
            cacheLog.info("麒麟系统版本信息: {}", versionInfo);

            boolean isKylinV4 = versionInfo.toLowerCase().contains("kylin") && versionInfo.contains("4");
            boolean isKylinV10 = versionInfo.toLowerCase().contains("kylin") && versionInfo.contains("10");

            // 根据麒麟版本选择修复方法
            if (isKylinV4) {
                cacheLog.info("检测到麒麟V4系统，使用CentOS方式修复SELinux...");

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

                boolean configFixed = sedResult.isSuccess();

                // 验证配置修改
                if (configFixed) {
                    cacheLog.info("验证SELinux配置文件修改...");
                    CommandResult verifyResult = execCommand(session, "grep '^SELINUX=' /etc/selinux/config");

                    boolean configModified = verifyResult.isSuccess() &&
                            verifyResult.getOutput().trim().contains("SELINUX=disabled");

                    if (!configModified) {
                        cacheLog.warn("配置文件验证失败，可能未正确修改: {}",
                                verifyResult.isSuccess() ? verifyResult.getOutput().trim()
                                        : verifyResult.getErrorOrOutput());
                        configFixed = false;
                    }
                }

                if (!configFixed) {
                    cacheLog.error("修改SELinux配置文件失败");

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    // 添加警告信息
                    detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                            "SELinux配置修改失败",
                            "无法修改SELinux配置文件，请手动修改。"));

                    // 添加手动修复指南
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());
                    detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
                    detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                    detailsBuilder.append("<li style='margin-bottom:5px'>编辑SELinux配置文件:</li>");
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo vi /etc/selinux/config"));
                    detailsBuilder.append("<li style='margin-bottom:5px'>将SELINUX=行修改为:</li>");
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("SELINUX=disabled"));
                    detailsBuilder.append("<li style='margin-bottom:5px'>保存并重启系统:</li>");
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo reboot"));
                    detailsBuilder.append("</ol>");
                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 设置消息
                    checkItem.setMessage(detailsBuilder.toString());
                    return false;
                }

                // 修复成功
                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加成功信息
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "SELinux修复完成",
                        "麒麟V4系统上的SELinux已临时设置为宽容模式，并已将配置文件修改为禁用状态。"));

                // 添加当前状态
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>当前SELinux状态:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "临时状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "永久状态", "已设置为禁用", HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加重启通知
                detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                        "重要提示",
                        "SELinux配置修改需要重启系统后才能完全生效。在重启前，系统将以宽容模式(Permissive)运行SELinux。"));

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());

                return true;

            } else if (isKylinV10) {
                cacheLog.info("检测到麒麟V10系统，使用Ubuntu方式修复SELinux...");

                // 检查SELinux工具是否安装
                cacheLog.info("检查麒麟V10系统上SELinux工具是否安装...");
                CommandResult selinuxUtilsResult = execCommand(session,
                        "dpkg -l | grep -E 'selinux-utils|selinux-basics'");

                // 如果没有安装SELinux工具，则无需修复
                if (!selinuxUtilsResult.isSuccess() || selinuxUtilsResult.getOutput().trim().isEmpty()) {
                    cacheLog.info("麒麟V10系统上未安装SELinux工具，无需修复。");

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    // 添加状态信息
                    detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                            "无需修复", "麒麟V10系统上未安装SELinux工具，SELinux已处于禁用状态，不需要进行任何修复操作。"));

                    // 设置消息
                    checkItem.setMessage(detailsBuilder.toString());
                    return true;
                }

                // 检查SELinux模块是否加载
                cacheLog.info("检查SELinux内核模块是否加载...");
                CommandResult selinuxModuleResult = execCommand(session, "lsmod | grep selinux || echo '未加载SELinux模块'");
                boolean selinuxModuleLoaded = selinuxModuleResult.isSuccess() &&
                        !selinuxModuleResult.getOutput().trim().contains("未加载SELinux模块");

                // 如果已安装工具但模块未加载，可选择卸载工具包
                if (!selinuxModuleLoaded) {
                    cacheLog.info("已安装SELinux工具但模块未加载，SELinux处于有效禁用状态，可选择卸载工具包。");

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                            "SELinux已有效禁用",
                            "SELinux模块未加载，已处于有效禁用状态。工具包已安装但不会影响系统运行。"));

                    // 添加可选操作
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());
                    detailsBuilder.append("<p><strong>可选操作:</strong></p>");
                    detailsBuilder.append("<p>您可以选择完全移除SELinux工具包，或保持当前状态。</p>");
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                            "# 移除SELinux工具包\nsudo apt purge selinux-utils selinux-basics"));
                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 设置消息
                    checkItem.setMessage(detailsBuilder.toString());
                    return true;
                }

                // 如果模块已加载，使用通用方法修复
                cacheLog.info("检测到麒麟V10系统上SELinux模块已加载，使用通用修复方法...");

                // 先尝试Ubuntu特定的禁用方法
                cacheLog.info("尝试使用Ubuntu特定方法禁用SELinux...");

                // 1. 禁用启动参数
                cacheLog.info("修改GRUB启动参数以禁用SELinux...");
                CommandResult grubResult = execCommand(session,
                        "sudo grep -q 'selinux=0' /etc/default/grub || " +
                                "sudo sed -i 's/GRUB_CMDLINE_LINUX=\"/GRUB_CMDLINE_LINUX=\"selinux=0 /' /etc/default/grub");

                boolean grubSuccess = grubResult.isSuccess();
                if (grubSuccess) {
                    // 更新grub配置
                    cacheLog.info("更新GRUB配置...");
                    execCommand(session, "sudo update-grub");
                } else {
                    cacheLog.warn("修改GRUB启动参数失败，将使用通用方法继续修复");
                }

                // 接着使用通用方法完成剩余修复
                return super.fix(hostInfo, checkItem, cacheLog);

            } else {
                cacheLog.info("无法确定麒麟版本或非标准麒麟版本，使用通用修复方法");
                return super.fix(hostInfo, checkItem, cacheLog);
            }

        } catch (Exception e) {
            String errorMsg = "修复麒麟系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }
    }
}