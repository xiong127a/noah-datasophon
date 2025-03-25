package com.datasophon.api.service.checker.checkers.selinux;

import com.datasophon.api.service.checker.checkers.selinux.factory.SELinuxCheckerFactory;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SELinux检查器
 * 负责检查和修复主机SELinux配置
 * 支持多种Linux发行版，包括CentOS、Ubuntu和Kylin
 */
@Component
public class SELinuxChecker extends AbstractItemChecker {

        private static final Logger logger = LoggerFactory.getLogger(SELinuxChecker.class);

        @Override
        protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
                try {
                        logger.info("开始检查主机 {} 的SELinux状态", hostInfo.getHostname());
                        cacheLog.info("开始检查SELinux状态...");

                        // 更新检查项状态
                        checkItem.setMessage("正在检查SELinux状态...");

                        // 检查会话是否准备就绪
                        if (session == null) {
                                // 检查hostInfo中是否有可用的会话
                                if (!hostInfo.isSessionReady()) {
                                        String errorMsg = "SSH会话未就绪，无法执行SELinux检查: " + hostInfo.getHostname();
                                        logger.error(errorMsg);
                                        cacheLog.error(errorMsg);
                                        checkItem.setStatus(CheckItem.Status.FAILED);
                                        checkItem.setMessage(errorMsg);
                                        return checkItem;
                                }
                                // 使用hostInfo的会话
                                session = hostInfo.getExternalSession();
                        }

                        // 获取操作系统信息
                        OsInfo osInfo;
                        try {
                                osInfo = getOsInfo(hostInfo);
                                if (osInfo == null || !osInfo.isValid()) {
                                        String errorMsg = "无法获取操作系统信息，SELinux检查失败";
                                        logger.error(errorMsg);
                                        cacheLog.error(errorMsg);
                                        checkItem.setStatus(CheckItem.Status.FAILED);
                                        checkItem.setMessage(errorMsg);
                                        return checkItem;
                                }
                                hostInfo.setExternalSession(session);
                        } catch (InterruptedException e) {
                                String errorMsg = "获取操作系统信息过程被中断";
                                logger.error(errorMsg, e);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(errorMsg);
                                return checkItem;
                        }

                        logger.info("主机 {} 操作系统: {}", hostInfo.getHostname(), osInfo.getFullName());
                        cacheLog.info("操作系统信息: {}", osInfo.getFullName());

                        // 通过工厂获取对应的SELinux检查器策略
                        SELinuxCheckerStrategy strategy = SELinuxCheckerFactory.getChecker(osInfo);

                        // 执行检查
                        CheckItem result = strategy.check(hostInfo, checkItem, cacheLog);

                        // 返回检查结果
                        return result;

                } catch (Exception e) {
                        String errorMsg = "检查SELinux时发生异常: " + e.getMessage();
                        logger.error(errorMsg, e);
                        cacheLog.error(errorMsg);
                        cacheLog.error(e.getMessage());

                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("检查SELinux失败: "
                                        + (StringUtils.isBlank(e.getMessage()) ? "未知错误" : e.getMessage()));
                        return checkItem;
                } finally {
                        logger.info("完成主机 {} 的SELinux检查", hostInfo.getHostname());
                        cacheLog.info("SELinux检查完成");
                }
        }

        @Override
        protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
                try {
                        logger.info("开始修复主机 {} 的SELinux配置", hostInfo.getHostname());
                        cacheLog.info("开始修复SELinux配置...");

                        // 更新修复项状态
                        checkItem.setMessage("正在修复SELinux配置...");

                        // 检查会话是否准备就绪
                        if (session == null) {
                                // 检查hostInfo中是否有可用的会话
                                if (!hostInfo.isSessionReady()) {
                                        String errorMsg = "SSH会话未就绪，无法执行SELinux修复: " + hostInfo.getHostname();
                                        logger.error(errorMsg);
                                        cacheLog.error(errorMsg);
                                        checkItem.setStatus(CheckItem.Status.FAILED);
                                        checkItem.setMessage(errorMsg);
                                        return false;
                                }
                                // 使用hostInfo的会话
                                session = hostInfo.getExternalSession();
                        }

                        // 获取操作系统信息
                        OsInfo osInfo;
                        try {
                                osInfo = getOsInfo(hostInfo);
                                if (osInfo == null || !osInfo.isValid()) {
                                        String errorMsg = "无法获取操作系统信息，SELinux修复失败";
                                        logger.error(errorMsg);
                                        cacheLog.error(errorMsg);
                                        checkItem.setMessage(errorMsg);
                                        return false;
                                }
                        } catch (InterruptedException e) {
                                String errorMsg = "获取操作系统信息过程被中断";
                                logger.error(errorMsg, e);
                                cacheLog.error(errorMsg);
                                checkItem.setMessage(errorMsg);
                                return false;
                        }

                        logger.info("主机 {} 操作系统: {}", hostInfo.getHostname(), osInfo.getFullName());
                        cacheLog.info("操作系统信息: {}", osInfo.getFullName());

                        // 通过工厂获取对应的SELinux检查器策略
                        SELinuxCheckerStrategy strategy = SELinuxCheckerFactory.getChecker(osInfo);

                        // 执行修复
                        boolean result = false;
                        try {
                                result = strategy.fix(hostInfo, checkItem, cacheLog);
                        } catch (InterruptedException e) {
                                String errorMsg = "修复SELinux被中断: " + e.getMessage();
                                logger.error(errorMsg, e);
                                cacheLog.error(errorMsg);
                                checkItem.setMessage(errorMsg);
                                return false;
                        }

                        // 记录修复结果
                        if (result) {
                                logger.info("主机 {} SELinux修复成功", hostInfo.getHostname());
                                cacheLog.info("SELinux修复成功");
                        } else {
                                logger.warn("主机 {} SELinux修复失败", hostInfo.getHostname());
                                cacheLog.warn("SELinux修复失败");
                        }

                        return result;
                } catch (Exception e) {
                        String errorMsg = "修复SELinux时发生异常: " + e.getMessage();
                        logger.error(errorMsg, e);
                        cacheLog.error(errorMsg);
                        checkItem.setMessage(errorMsg);
                        return false;
                } finally {
                        logger.info("完成主机 {} 的SELinux修复", hostInfo.getHostname());
                        cacheLog.info("SELinux修复完成");
                }
        }

        @Override
        public ItemCode getCheckerType() {
                return ItemCode.SELINUX;
        }

        /**
         * 设置日志键
         * 公开的方法，用于子类设置日志键
         * 
         * @param clusterId 集群ID
         * @param hostname  主机名
         * @param itemId    检查项ID
         */
        public void setupLogKey(Integer clusterId, String hostname, Integer itemId) {
                setCurrentLogKey(clusterId, hostname, itemId);
        }

        /**
         * 执行命令并获取结果
         * 
         * @param session SSH会话
         * @param command 要执行的命令
         * @return 命令执行结果
         * @throws InterruptedException 如果命令执行被中断
         */
        public CommandResult execCommand(org.apache.sshd.client.session.ClientSession session, String command)
                        throws InterruptedException {
                // 确保正确设置了currentLogKey
                if (currentLogKey == null) {
                        logger.warn("执行命令时currentLogKey为null，尝试从上下文恢复日志键");

                        // 尝试从线程名中恢复日志键
                        String threadName = Thread.currentThread().getName();

                        // 检查线程名是否符合期望的格式，通常是: check-hostname-item-id
                        if (threadName != null && threadName.startsWith("check-") && threadName.contains("-item-")) {
                                try {
                                        String[] parts = threadName.split("-item-");
                                        if (parts.length == 2) {
                                                String hostnamePart = parts[0].substring("check-".length());
                                                Integer itemId = Integer.parseInt(parts[1]);

                                                // 使用当前任务的HostInfo和CheckItem对象获取集群ID和主机名
                                                Integer clusterId = null;
                                                if (this.currentHostInfo != null) {
                                                        clusterId = this.currentHostInfo.get().getClusterId();
                                                }

                                                // 设置日志键
                                                this.setCurrentLogKey(clusterId, hostnamePart, itemId);
                                                logger.info("已从线程名恢复日志键: 集群ID={}, 主机名={}, 检查项ID={}",
                                                                clusterId, hostnamePart, itemId);
                                        }
                                } catch (Exception e) {
                                        logger.warn("从线程名恢复日志键失败: {}", e.getMessage());
                                }
                        }
                }

                return super.execCommand(session, command);
        }
}