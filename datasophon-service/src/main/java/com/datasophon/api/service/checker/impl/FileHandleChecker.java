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
            String result = execCommand(session, "ulimit -n");
            if (result.startsWith("ERROR")) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("检查失败: " + result);
                return checkItem;
            }

            int fileHandles = Integer.parseInt(result.trim());
            boolean success = fileHandles >= MIN_FILE_HANDLES;

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);
            checkItem.setMessage(success ?
                    String.format("当前最大文件句柄数: %d", fileHandles) :
                    String.format("当前最大文件句柄数(%d)小于建议值(%d)", fileHandles, MIN_FILE_HANDLES));
        } catch (Exception e) {
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查失败: " + e.getMessage());
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 修改 /etc/security/limits.conf 文件
            String cmd = String.format("grep -q '* soft nofile %d' /etc/security/limits.conf || echo '* soft nofile %d' >> /etc/security/limits.conf && " +
                            "grep -q '* hard nofile %d' /etc/security/limits.conf || echo '* hard nofile %d' >> /etc/security/limits.conf",
                    MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES);

            String result = execCommand(session, cmd);

            // 如果是CentOS/RHEL,还需要通过systemd配置
            if (isSystemdExists(session)) {
                String systemdConfig = "echo -e '[Manager]\\nDefaultLimitNOFILE=" + MIN_FILE_HANDLES + "' > /etc/systemd/system.conf.d/limits.conf";
                execCommand(session, "mkdir -p /etc/systemd/system.conf.d");
                execCommand(session, systemdConfig);
                execCommand(session, "systemctl daemon-reload");
            }

            return !result.startsWith("ERROR");
        } catch (Exception e) {
            logger.error("修复文件句柄数失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean isSystemdExists(ClientSession session) {
        String result = execCommand(session, "[ -d /etc/systemd ] && echo 'true' || echo 'false'");
        return "true".equals(result.trim());
    }

    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.FILE_HANDLE;
    }
}