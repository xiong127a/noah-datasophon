package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
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
                checkItem.setMessage("获取SELinux状态失败: " + result.getErrorOrOutput());
                return checkItem;
            }

            String selinuxStatus = result.getOutput().trim();
            cacheLog.info("SELinux当前状态: " + selinuxStatus);

            // 更新状态为正在检查SELinux配置文件
            setCheckItemMessage(hostInfo, checkItem, "正在检查SELinux配置文件...");

            // 检查SELinux配置文件
            cacheLog.info("检查SELinux配置文件...");
            CommandResult configResult = execCommand(session, "cat /etc/selinux/config | grep ^SELINUX=");

            if (configResult.isSuccess()) {
                cacheLog.info("SELinux配置: " + configResult.getOutput().trim());
            } else {
                cacheLog.warn("无法读取SELinux配置文件: %s", configResult.getErrorOrOutput());
            }

            // 更新状态为正在分析SELinux状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析SELinux状态: " + selinuxStatus);

            // 判断状态
            if ("Disabled".equalsIgnoreCase(selinuxStatus) || "Permissive".equalsIgnoreCase(selinuxStatus)) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("SELinux已禁用或处于宽容模式: " + selinuxStatus);
                cacheLog.info("SELinux检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SELinux处于强制模式(Enforcing)，需要禁用或设置为宽容模式");
                cacheLog.info("SELinux检查未通过: 当前为强制模式");
            }

        } catch (Exception e) {
            String errorMsg = "检查SELinux时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
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

            if (!setenforceResult.isSuccess()) {
                cacheLog.error("设置SELinux状态失败: %s", setenforceResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("已临时设置SELinux为宽容模式");

            // 更新状态为正在修改SELinux配置文件
            setCheckItemMessage(hostInfo, checkItem, "正在修改SELinux配置文件...");

            // 修改配置文件
            cacheLog.info("修改SELinux配置文件...");
            String sedCmd = "sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config";
            CommandResult sedResult = execCommand(session, sedCmd);

            if (!sedResult.isSuccess()) {
                cacheLog.error("修改SELinux配置文件失败: %s", sedResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("SELinux配置文件已修改");

            // 更新状态为正在验证SELinux配置
            setCheckItemMessage(hostInfo, checkItem, "正在验证SELinux配置...");

            // 验证配置
            cacheLog.info("验证SELinux配置...");
            CommandResult verifyResult = execCommand(session, "cat /etc/selinux/config | grep ^SELINUX=");

            if (!verifyResult.isSuccess()) {
                cacheLog.error("验证SELinux配置失败: %s", verifyResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("当前SELinux配置: " + verifyResult.getOutput().trim());

            cacheLog.info("==== SELinux配置修复完成 ====");
            cacheLog.info("注意: 完全禁用SELinux需要重启系统才能生效");

            // 更新状态为修复完成
            setCheckItemMessage(hostInfo, checkItem, "SELinux配置已修复，完全禁用需要重启系统");

            return true;
        } catch (Exception e) {
            String errorMsg = "修复SELinux配置时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            return false;
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.SELINUX;
    }
} 