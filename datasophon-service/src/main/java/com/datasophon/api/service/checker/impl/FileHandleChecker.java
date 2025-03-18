package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileHandleChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(FileHandleChecker.class);
    private static final int MIN_FILE_HANDLES = 65535;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("开始检查文件句柄数 - 主机: %s", hostInfo.getHostname());
            cacheLog.debug("最小建议文件句柄数: %d", MIN_FILE_HANDLES);
            cacheLog.info("正在检查系统最大文件句柄数限制...");
            
            // 执行ulimit命令获取当前最大文件句柄数
            cacheLog.debug("执行命令: ulimit -n");
            cacheLog.info("执行命令: ulimit -n 获取当前文件句柄数...");
            String result = execCommand(session, "ulimit -n");
            
            if (result.startsWith("ERROR")) {
                cacheLog.debug("命令执行失败: %s", result);
                cacheLog.error("获取文件句柄数失败: %s", result);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("检查失败: " + result);
                return checkItem;
            }

            cacheLog.debug("命令返回值: %s", result.trim());
            cacheLog.info("当前系统最大文件句柄数: %s", result.trim());
            int fileHandles = Integer.parseInt(result.trim());
            boolean success = fileHandles >= MIN_FILE_HANDLES;
            
            cacheLog.debug("当前文件句柄数: %d, 是否满足最小要求: %s", fileHandles, success ? "是" : "否");
            cacheLog.info("检查结果: 当前文件句柄数为 %d, 最小建议值为 %d", fileHandles, MIN_FILE_HANDLES);

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);
            checkItem.setMessage(success ?
                    String.format("当前最大文件句柄数: %d", fileHandles) :
                    String.format("当前最大文件句柄数(%d)小于建议值(%d)", fileHandles, MIN_FILE_HANDLES));
            
            cacheLog.debug("检查结果: %s, 消息: %s", checkItem.getStatus(), checkItem.getMessage());
            cacheLog.info("文件句柄数检查%s", success ? "通过" : "未通过");
        } catch (Exception e) {
            cacheLog.debug("检查过程异常: %s", e.getMessage());
            cacheLog.error("检查文件句柄数过程中发生异常: %s", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查失败: " + e.getMessage());
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("开始修复文件句柄数限制 - 主机: %s", hostInfo.getHostname());
            
            // 修改 /etc/security/limits.conf 文件
            String cmd = String.format("grep -q '* soft nofile %d' /etc/security/limits.conf || echo '* soft nofile %d' >> /etc/security/limits.conf && " +
                            "grep -q '* hard nofile %d' /etc/security/limits.conf || echo '* hard nofile %d' >> /etc/security/limits.conf",
                    MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES);
            
            cacheLog.debug("执行修复命令: %s", cmd);
            String result = execCommand(session, cmd);
            
            if (result.startsWith("ERROR")) {
                cacheLog.debug("修改limits.conf文件失败: %s", result);
                cacheLog.error("修改limits.conf文件失败: %s", result);
                return false;
            }
            cacheLog.debug("limits.conf文件修改结果: %s", result.isEmpty() ? "成功" : result);

            // 检查是否存在systemd
            cacheLog.debug("检查主机是否使用systemd...");
            boolean hasSystemd = isSystemdExists(session);
            cacheLog.debug("systemd检查结果: %s", hasSystemd ? "存在" : "不存在");
            
            // 如果是CentOS/RHEL,还需要通过systemd配置
            if (hasSystemd) {
                cacheLog.debug("通过systemd配置文件句柄限制...");
                
                // 创建systemd配置目录
                cacheLog.debug("创建目录: /etc/systemd/system.conf.d");
                execCommand(session, "mkdir -p /etc/systemd/system.conf.d");
                
                // 创建systemd配置文件
                String systemdConfig = "echo -e '[Manager]\\nDefaultLimitNOFILE=" + MIN_FILE_HANDLES + "' > /etc/systemd/system.conf.d/limits.conf";
                cacheLog.debug("配置systemd文件句柄限制: %s", systemdConfig);
                String systemdResult = execCommand(session, systemdConfig);
                
                if (systemdResult.startsWith("ERROR")) {
                    cacheLog.debug("配置systemd文件句柄限制失败: %s", systemdResult);
                    cacheLog.warn("配置systemd文件句柄限制失败，但不影响使用: %s", systemdResult);
                } else {
                    cacheLog.debug("systemd文件句柄限制配置成功");
                }
                
                // 重新加载systemd配置
                cacheLog.debug("重新加载systemd配置");
                String reloadResult = execCommand(session, "systemctl daemon-reload");
                cacheLog.debug("systemctl daemon-reload结果: %s", reloadResult.isEmpty() ? "成功" : reloadResult);
            }
            
            cacheLog.debug("文件句柄限制修复完成, 需要用户重新登录生效");
            return !result.startsWith("ERROR");
        } catch (Exception e) {
            logger.error("修复文件句柄数失败: {}", e.getMessage());
            cacheLog.error("修复文件句柄数失败: %s", e.getMessage());
            cacheLog.debug("修复过程异常详情: %s", e.toString());
            return false;
        }
    }

    private boolean isSystemdExists(ClientSession session) throws InterruptedException {
        cacheLog.debug("检查systemd目录是否存在...");
        String result = execCommand(session, "[ -d /etc/systemd ] && echo 'true' || echo 'false'");
        boolean exists = "true".equals(result.trim());
        cacheLog.debug("systemd目录检查结果: %s", exists ? "存在" : "不存在");
        return exists;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FILE_HANDLE;
    }
}