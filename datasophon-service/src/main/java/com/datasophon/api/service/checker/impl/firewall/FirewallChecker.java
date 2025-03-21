package com.datasophon.api.service.checker.impl.firewall;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.model.OSInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirewallChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(FirewallChecker.class);
    
    // 防火墙检查器工厂
    private FirewallCheckerFactory firewallCheckerFactory;
    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 防火墙检查开始 ====");
            cacheLog.info("检查系统防火墙状态...");
            
            // 更新状态为正在检查防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙状态...");
            
            // 获取操作系统信息
            OSInfo osInfo = detectOSInfo(hostInfo);
            
            if (osInfo == null) {
                String errorMsg = "无法获取操作系统信息，检查失败";
                logger.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, errorMsg);
                return checkItem;
            }
            
            cacheLog.info("获取到主机操作系统信息: {}", osInfo);
            
            // 初始化防火墙检查器工厂
            firewallCheckerFactory = createFirewallCheckerFactory();
            
            // 获取适用的防火墙检查器
            IFirewallChecker firewallChecker = firewallCheckerFactory.getChecker(hostInfo, osInfo);
            if (firewallChecker == null) {
                String errorMsg = "没有找到适用于当前系统的防火墙检查器: " + osInfo.getDistro() + " " + osInfo.getFullVersion();
                logger.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, errorMsg);
                return checkItem;
            }
            
            cacheLog.info("使用防火墙检查器: {}", firewallChecker.getClass().getSimpleName());
            
            // 检查防火墙状态
            FirewallCheckResult result = firewallChecker.checkFirewallState(null);
            
            if (!result.isSuccess()) {
                cacheLog.warn("检查防火墙状态失败: {}", result.getErrorMessage());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "检查防火墙状态失败: " + result.getErrorMessage());
                return checkItem;
            }
            
            // 判断防火墙状态并更新检查项
            if (result.isEnabled()) {
                cacheLog.info("防火墙状态: 已启用");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "防火墙正在运行，建议关闭");
                
                // 尝试获取更多配置信息
                try {
                    if (osInfo.isRedHatFamily()) {
                        CommandResult firewallConfig = execCommand(session, "firewall-cmd --list-all 2>/dev/null || echo '防火墙配置信息不可用'");
                        if (firewallConfig.isSuccess()) {
                            cacheLog.info("当前防火墙配置信息:");
                            cacheLog.info(firewallConfig.getOutput());
                        }
                    } else if (osInfo.isDebianFamily()) {
                        CommandResult firewallConfig = execCommand(session, "ufw status verbose 2>/dev/null || echo '防火墙配置信息不可用'");
                        if (firewallConfig.isSuccess()) {
                            cacheLog.info("当前防火墙配置信息:");
                            cacheLog.info(firewallConfig.getOutput());
                        }
                    }
                } catch (Exception e) {
                    cacheLog.warn("获取防火墙配置信息失败: {}", e.getMessage());
                }
            } else {
                cacheLog.info("防火墙状态: 已禁用");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "防火墙已关闭");
            }
            
        } catch (Exception e) {
            String errorMsg = "防火墙检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: {}", errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== 防火墙检查完成 ====");
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复防火墙配置 ====");
            
            // 更新状态为正在检查防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙当前状态...");
            
            // 获取操作系统信息
            OSInfo osInfo = detectOSInfo(hostInfo);
            
            if (osInfo == null) {
                String errorMsg = "无法获取操作系统信息，修复失败";
                logger.error(errorMsg);
                cacheLog.error(errorMsg);
                setCheckItemMessage(hostInfo, checkItem, errorMsg);
                return false;
            }
            
            cacheLog.info("获取到主机操作系统信息: {}", osInfo);
            
            // 初始化防火墙检查器工厂（如果为空）
            if (firewallCheckerFactory == null) {
                firewallCheckerFactory = createFirewallCheckerFactory();
            }
            
            // 获取适用的防火墙检查器
            IFirewallChecker firewallChecker = firewallCheckerFactory.getChecker(hostInfo, osInfo);
            if (firewallChecker == null) {
                String errorMsg = "没有找到适用于当前系统的防火墙检查器: " + osInfo.getDistro() + " " + osInfo.getFullVersion();
                logger.error(errorMsg);
                cacheLog.error(errorMsg);
                setCheckItemMessage(hostInfo, checkItem, errorMsg);
                return false;
            }
            
            cacheLog.info("使用防火墙检查器: {}", firewallChecker.getClass().getSimpleName());
            
            // 先检查防火墙状态
            FirewallCheckResult checkResult = firewallChecker.checkFirewallState(null);
            
            // 如果检查失败并且错误信息包含服务不存在，则直接返回成功
            if (!checkResult.isSuccess() && checkResult.getErrorMessage().contains("服务不存在")) {
                cacheLog.info("防火墙服务未安装，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "防火墙服务未安装，无需修复");
                return true;
            }
            
            // 如果防火墙已经关闭，则直接返回成功
            if (checkResult.isSuccess() && !checkResult.isEnabled()) {
                cacheLog.info("防火墙已关闭，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "防火墙已关闭，无需修复");
                return true;
            }
            
            // 更新状态为正在关闭防火墙
            setCheckItemMessage(hostInfo, checkItem, "正在关闭防火墙...");
            
            // 关闭防火墙
            cacheLog.info("正在关闭防火墙...");
            CommandResult fixResult = firewallChecker.fixFirewallState(false);
            if (!fixResult.isSuccess()) {
                cacheLog.error("关闭防火墙失败: {}", fixResult.getErrorOrOutput());
                setCheckItemMessage(hostInfo, checkItem, "关闭防火墙失败: " + fixResult.getErrorOrOutput());
                return false;
            }
            
            cacheLog.info("关闭防火墙完成");
            
            // 更新状态为正在验证防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在验证防火墙状态...");
            
            // 再次检查确认防火墙已关闭
            cacheLog.info("验证防火墙状态...");
            FirewallCheckResult verifyResult = firewallChecker.checkFirewallState(null);
            
            if (!verifyResult.isSuccess()) {
                cacheLog.warn("验证防火墙状态失败: {}", verifyResult.getErrorMessage());
                setCheckItemMessage(hostInfo, checkItem, "验证防火墙状态失败，请手动检查");
                return false;
            }
            
            if (verifyResult.isEnabled()) {
                cacheLog.warn("警告: 防火墙服务可能未成功关闭，请手动检查");
                setCheckItemMessage(hostInfo, checkItem, "警告: 防火墙服务可能未成功关闭，请手动检查");
                return false;
            }
            
            cacheLog.info("验证成功: 防火墙已关闭");
            setCheckItemMessage(hostInfo, checkItem, "防火墙已成功关闭并禁用自启动");
            cacheLog.info("==== 防火墙配置修复完成 ====");
            return true;
            
        } catch (Exception e) {
            String errorMsg = "防火墙配置修复失败: " + e.getMessage();
            logger.error(errorMsg);
            cacheLog.error("错误: {}", errorMsg);
            setCheckItemMessage(hostInfo, checkItem, "防火墙配置修复失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建防火墙检查器工厂
     */
    private FirewallCheckerFactory createFirewallCheckerFactory() {
        // 创建SSH命令执行器
        SSHCommandExecutorImpl sshCommandExecutor = new SSHCommandExecutorImpl(session);
        
        // 创建防火墙检查器工厂
        return new FirewallCheckerFactory(sshCommandExecutor);
    }
    
    /**
     * 检测操作系统信息
     * 如果AbstractItemChecker中没有getOSInfo方法，则使用此方法进行本地检测
     */
    private OSInfo detectOSInfo(HostInfo hostInfo) {
        OSInfo osInfo = new OSInfo();
        osInfo.setHostname(hostInfo.getHostname());
        
        try {
            // 先尝试检测是否为CentOS
            CommandResult result = execCommand(session, "cat /etc/redhat-release 2>/dev/null || echo ''");
            if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                String output = result.getOutput().toLowerCase();
                osInfo.setFamily("RedHat");
                
                if (output.contains("centos")) {
                    osInfo.setDistro("CentOS");
                    // 提取版本号
                    if (output.contains("release 7")) {
                        osInfo.setMajorVersion(7);
                        osInfo.setFullVersion("7");
                    } else if (output.contains("release 8")) {
                        osInfo.setMajorVersion(8);
                        osInfo.setFullVersion("8");
                    }
                } else if (output.contains("red hat")) {
                    osInfo.setDistro("RHEL");
                    if (output.contains("release 7")) {
                        osInfo.setMajorVersion(7);
                        osInfo.setFullVersion("7");
                    } else if (output.contains("release 8")) {
                        osInfo.setMajorVersion(8);
                        osInfo.setFullVersion("8");
                    }
                }
                
                return osInfo;
            }
            
            // 检查Ubuntu/Debian/Kylin
            result = execCommand(session, "cat /etc/os-release 2>/dev/null || echo ''");
            if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                String output = result.getOutput().toLowerCase();
                
                if (output.contains("id=ubuntu")) {
                    osInfo.setFamily("Debian");
                    osInfo.setDistro("Ubuntu");
                    // 提取版本号
                    if (output.contains("version_id=\"22.")) {
                        osInfo.setMajorVersion(22);
                        osInfo.setFullVersion("22.04");
                    } else if (output.contains("version_id=\"24.")) {
                        osInfo.setMajorVersion(24);
                        osInfo.setFullVersion("24.04");
                    }
                } else if (output.contains("id=debian")) {
                    osInfo.setFamily("Debian");
                    osInfo.setDistro("Debian");
                } else if (output.contains("id=kylin") || output.contains("id=\"kylin\"")) {
                    osInfo.setFamily("RedHat");
                    osInfo.setDistro("Kylin");
                    // 提取版本号
                    if (output.contains("v4") || output.contains("v4")) {
                        osInfo.setMajorVersion(4);
                        osInfo.setFullVersion("V4");
                    } else if (output.contains("v10") || output.contains("v10")) {
                        osInfo.setMajorVersion(10);
                        osInfo.setFullVersion("V10");
                    }
                }
                
                return osInfo;
            }
            
            // 如果都无法确定，返回默认值
            osInfo.setFamily("Linux");
            osInfo.setDistro("Unknown");
            osInfo.setFullVersion("Unknown");
            
        } catch (Exception e) {
            logger.error("检测操作系统信息失败: {}", e.getMessage());
            osInfo.setFamily("Linux");
            osInfo.setDistro("Unknown");
            osInfo.setFullVersion("Unknown");
        }
        
        return osInfo;
    }
    
    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
} 