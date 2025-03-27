package com.datasophon.api.service.checker.checkers.selinux.os.ubuntu;

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
 * Ubuntu系统专用的SELinux检查器实现
 * 适用于Ubuntu 22/24等版本
 */
public class UbuntuSELinuxChecker extends GenericSELinuxChecker {

    private static final Logger log = LoggerFactory.getLogger(UbuntuSELinuxChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("使用Ubuntu专用的SELinux检查器...");

        try {
            // 获取会话
            ClientSession session = hostInfo.getExternalSession();
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux检查: " + hostInfo.getIp();
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
                selinuxChecker.setupLogKey(clusterId, hostInfo.getIp(), checkItem.getId());
            }

            // 优先检查AppArmor状态
            cacheLog.info("在Ubuntu系统上检查AppArmor状态（Ubuntu默认使用AppArmor而非SELinux）...");
            CommandResult apparmorStatusResult = execCommand(session,
                    "command -v aa-status && aa-status || echo 'AppArmor未安装'");

            String apparmorStatus = apparmorStatusResult.isSuccess() ? apparmorStatusResult.getOutput().trim() : "未知";
            boolean apparmorEnabled = apparmorStatus.contains("apparmor module is loaded") ||
                    apparmorStatus.contains("profiles are loaded");

            if (apparmorEnabled) {
                cacheLog.info("检测到Ubuntu系统上AppArmor已启用，这是Ubuntu的默认安全模块");
            }

            // 检查SELinux工具是否安装
            cacheLog.info("检查Ubuntu系统上SELinux相关工具是否安装...");
            CommandResult selinuxUtilsResult = execCommand(session, "dpkg -l | grep -E 'selinux-utils|selinux-basics'");

            // 检查相关内核模块
            cacheLog.info("检查SELinux内核模块...");
            CommandResult selinuxModuleResult = execCommand(session, "lsmod | grep selinux || echo '未加载SELinux模块'");

            // 如果没有安装SELinux工具，则直接通过检查
            if (!selinuxUtilsResult.isSuccess() || selinuxUtilsResult.getOutput().trim().isEmpty()) {
                cacheLog.info("Ubuntu系统上未安装SELinux工具包，检查通过。");
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加状态信息
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "SELinux状态", "未安装", HtmlStyleHelper.Colors.SUCCESS));

                // 添加AppArmor信息
                if (apparmorEnabled) {
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "AppArmor状态", "已启用", HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                            "Ubuntu安全说明",
                            "Ubuntu默认使用AppArmor作为安全模块，而非SELinux。AppArmor已正常启用，无需安装或配置SELinux。"));
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
                        "SELinux检查通过", "Ubuntu系统上未安装SELinux，不会干扰系统运行。" +
                                (apparmorEnabled ? " 系统已使用Ubuntu默认的AppArmor安全模块。" : "")));

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());
                return checkItem;
            }

            // 如果安装了SELinux工具但模块未加载，可能是安装但未启用
            boolean selinuxModuleLoaded = selinuxModuleResult.isSuccess() &&
                    !selinuxModuleResult.getOutput().trim().contains("未加载SELinux模块");

            if (!selinuxModuleLoaded) {
                cacheLog.info("Ubuntu系统上已安装SELinux工具包，但SELinux模块未加载，SELinux有效状态为禁用。");
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加状态信息
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
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
                        "Ubuntu系统上虽已安装SELinux工具，但模块未加载，SELinux处于有效禁用状态。" +
                                (apparmorEnabled ? " 系统已使用Ubuntu默认的AppArmor安全模块。" : "")));

                // 添加可选建议
                detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                        "建议操作",
                        "如无特殊需求，建议在Ubuntu系统上保持SELinux未加载状态，避免与AppArmor产生冲突。若需完全移除SELinux工具包，可执行: sudo apt purge selinux-utils selinux-basics"));

                // 设置消息
                checkItem.setMessage(detailsBuilder.toString());
                return checkItem;
            }

            // 如果安装了SELinux工具且模块已加载，则执行标准检查
            cacheLog.info("检测到Ubuntu系统上安装了SELinux且模块已加载，执行标准检查...");
            return super.check(hostInfo, checkItem, cacheLog);

        } catch (Exception e) {
            String errorMsg = "检查Ubuntu系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("使用Ubuntu专用的SELinux修复方法...");

        try {
            // 获取会话
            ClientSession session = hostInfo.getExternalSession();
            if (session == null) {
                String errorMsg = "SSH会话未就绪，无法执行SELinux修复: " + hostInfo.getIp();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 确保设置日志键
            if (selinuxChecker != null) {
                // 获取集群ID
                Integer clusterId = hostInfo.getClusterId();
                selinuxChecker.setupLogKey(clusterId, hostInfo.getIp(), checkItem.getId());
            }

            // 先检查SELinux工具是否安装
            cacheLog.info("检查Ubuntu系统上SELinux工具是否安装...");
            CommandResult selinuxUtilsResult = execCommand(session, "dpkg -l | grep -E 'selinux-utils|selinux-basics'");

            // 如果没有安装SELinux工具，则无需修复
            if (!selinuxUtilsResult.isSuccess() || selinuxUtilsResult.getOutput().trim().isEmpty()) {
                cacheLog.info("Ubuntu系统上未安装SELinux工具，无需修复。");

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加状态信息
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "无需修复", "Ubuntu系统上未安装SELinux工具，SELinux已处于禁用状态，不需要进行任何修复操作。"));

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
            cacheLog.info("检测到Ubuntu系统上SELinux模块已加载，使用通用修复方法...");

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

        } catch (Exception e) {
            String errorMsg = "修复Ubuntu系统上的SELinux时发生错误: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }
    }
}