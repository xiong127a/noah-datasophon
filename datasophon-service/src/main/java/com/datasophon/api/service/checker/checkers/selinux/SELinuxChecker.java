package com.datasophon.api.service.checker.checkers.selinux;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SELinuxChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(SELinuxChecker.class);

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== SELinux检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());

            // 更新状态为正在检查SELinux状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查SELinux状态...");

            // 检查SELinux状态
            cacheLog.info("检查SELinux状态...");
            CommandResult result = execCommand(session, "getenforce");

            if (!result.isSuccess()) {
                cacheLog.error("获取SELinux状态失败: %s", result.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取SELinux状态失败: " + result.getErrorOrOutput());
                return checkItem;
            }

            String selinuxStatus = result.getOutput().trim();
            cacheLog.info("SELinux当前状态: " + selinuxStatus);

            // 更新状态为正在检查SELinux配置文件
            setCheckItemMessage(hostInfo, checkItem, "正在检查SELinux配置文件...");

            // 检查SELinux配置文件
            cacheLog.info("检查SELinux配置文件...");
            CommandResult configResult = execCommand(session, "cat /etc/selinux/config | grep ^SELINUX=");

            String selinuxConfig = configResult.isSuccess() ? configResult.getOutput().trim() : "无法读取";
            if (configResult.isSuccess()) {
                cacheLog.info("SELinux配置: " + selinuxConfig);
            } else {
                cacheLog.warn("无法读取SELinux配置文件: %s", configResult.getErrorOrOutput());
            }

            // 更新状态为正在分析SELinux状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析SELinux状态: " + selinuxStatus);

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

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, true, "SELinux状态正常", detailsBuilder);

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
                        "sudo sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config\n" +
                                "# 完成后重启系统\nsudo reboot"));
                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "SELinux处于强制模式", detailsBuilder);

                cacheLog.info("SELinux检查未通过: 当前为强制模式");
            }

        } catch (Exception e) {
            String errorMsg = "检查SELinux时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== SELinux检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复SELinux配置 ====");

            // 更新状态为正在设置SELinux为宽容模式
            setCheckItemMessage(hostInfo, checkItem, "正在设置SELinux为宽容模式...");

            // 先设置为宽容模式
            cacheLog.info("设置SELinux为宽容模式...");
            CommandResult setenforceResult = execCommand(session, "setenforce 0");
            boolean tempFixSuccess = setenforceResult.isSuccess();

            if (!tempFixSuccess) {
                cacheLog.error("设置SELinux状态失败: %s", setenforceResult.getErrorOrOutput());

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "临时设置SELinux失败",
                        "无法临时设置SELinux为宽容模式: " + setenforceResult.getErrorOrOutput()));

                // 添加手动修复指南
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

                detailsBuilder.append("<li style='margin-bottom:5px'>以root权限临时禁用SELinux:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo setenforce 0"));

                detailsBuilder.append("<li style='margin-bottom:5px'>修改配置文件永久禁用SELinux:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "sudo vi /etc/selinux/config\n\n" +
                                "# 修改以下行\nSELINUX=disabled"));

                detailsBuilder.append("<li style='margin-bottom:5px'>重启系统使永久设置生效:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo reboot"));

                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "SELinux临时禁用失败", detailsBuilder);

                return false;
            }
            cacheLog.info("已临时设置SELinux为宽容模式");

            // 更新状态为正在修改SELinux配置文件
            setCheckItemMessage(hostInfo, checkItem, "正在修改SELinux配置文件...");

            // 修改配置文件
            cacheLog.info("修改SELinux配置文件...");
            String sedCmd = "sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config";
            CommandResult sedResult = execCommand(session, sedCmd);
            boolean configFixSuccess = sedResult.isSuccess();

            if (!configFixSuccess) {
                cacheLog.error("修改SELinux配置文件失败: %s", sedResult.getErrorOrOutput());

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误和部分成功信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "SELinux配置文件修改失败",
                        "SELinux已临时设置为宽容模式，但无法修改配置文件进行永久设置: " + sedResult.getErrorOrOutput()));

                // 添加当前状态说明
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>当前SELinux状态:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "临时状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "永久配置", "未修改 (重启后将恢复原状态)", HtmlStyleHelper.Colors.ERROR));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加手动修复指南
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

                detailsBuilder.append("<li style='margin-bottom:5px'>修改配置文件永久禁用SELinux:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "sudo vi /etc/selinux/config\n\n" +
                                "# 修改以下行\nSELINUX=disabled"));

                detailsBuilder.append("<li style='margin-bottom:5px'>重启系统使永久设置生效:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo reboot"));

                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, true, "SELinux已临时禁用", detailsBuilder);

                return true; // 返回true因为临时修复成功
            }
            cacheLog.info("SELinux配置文件已修改");

            // 更新状态为正在验证SELinux配置
            setCheckItemMessage(hostInfo, checkItem, "正在验证SELinux配置...");

            // 验证配置
            cacheLog.info("验证SELinux配置...");
            CommandResult verifyResult = execCommand(session, "cat /etc/selinux/config | grep ^SELINUX=");
            boolean verifySuccess = verifyResult.isSuccess();
            String newConfig = verifySuccess ? verifyResult.getOutput().trim() : "无法验证";

            if (!verifySuccess) {
                cacheLog.error("验证SELinux配置失败: %s", verifyResult.getErrorOrOutput());
            } else {
                cacheLog.info("当前SELinux配置: " + newConfig);
            }

            cacheLog.info("==== SELinux配置修复完成 ====");
            cacheLog.info("注意: 完全禁用SELinux需要重启系统才能生效");

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加修复操作信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>已完成的SELinux修复操作:</strong></p>");

            // 添加操作列表
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

            // 临时禁用操作
            detailsBuilder.append("<li style='margin-bottom:5px'>临时设置SELinux为宽容模式 (" +
                    HtmlStyleHelper.generateColoredValue("成功", HtmlStyleHelper.Colors.SUCCESS) + ")</li>");

            // 修改配置文件操作
            detailsBuilder.append("<li style='margin-bottom:5px'>修改SELinux配置文件为禁用模式 (" +
                    HtmlStyleHelper.generateColoredValue("成功", HtmlStyleHelper.Colors.SUCCESS) + ")</li>");

            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加当前状态信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>当前SELinux状态:</strong></p>");

            // 当前状态
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "当前运行状态", "宽容模式(Permissive)", HtmlStyleHelper.Colors.SUCCESS));

            // 配置文件状态
            if (verifySuccess) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "配置文件设置", newConfig, HtmlStyleHelper.Colors.SUCCESS));
            }

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加重启提示
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                    "完成禁用需要重启",
                    "SELinux已临时设置为宽容模式，配置文件已修改为禁用模式，但<strong>完全禁用需要重启系统</strong>才能生效。"));

            // 添加验证命令
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>重启后验证方法:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("getenforce"));
            detailsBuilder.append("<p>应返回: <code>Disabled</code></p>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, true, "SELinux配置已修复", detailsBuilder);

            return true;
        } catch (Exception e) {
            String errorMsg = "修复SELinux配置时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加错误信息
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "SELinux修复失败",
                    "修复SELinux配置时发生错误: " + e.getMessage()));

            // 添加手动修复指南
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

            detailsBuilder.append("<li style='margin-bottom:5px'>以root权限临时禁用SELinux:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo setenforce 0"));

            detailsBuilder.append("<li style='margin-bottom:5px'>修改配置文件永久禁用SELinux:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    "sudo vi /etc/selinux/config\n\n" +
                            "# 修改以下行\nSELINUX=disabled"));

            detailsBuilder.append("<li style='margin-bottom:5px'>重启系统使永久设置生效:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("sudo reboot"));

            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "SELinux修复失败", detailsBuilder);

            return false;
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.SELINUX;
    }
}