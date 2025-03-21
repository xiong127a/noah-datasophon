package com.datasophon.api.service.checker.impl.firewall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.service.checker.impl.firewall.impl.CentOSFirewallChecker;
import com.datasophon.api.service.checker.impl.firewall.impl.KylinFirewallChecker;
import com.datasophon.api.service.checker.impl.firewall.impl.UbuntuFirewallChecker;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OSInfo;

/**
 * 防火墙检查器工厂
 * 根据操作系统类型创建不同的防火墙检查器
 */
public class FirewallCheckerFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(FirewallCheckerFactory.class);
    
    // 注册的防火墙检查器列表
    private final List<IFirewallChecker> checkers = new ArrayList<>();
    
    // 缓存主机与对应的检查器
    private final Map<String, IFirewallChecker> hostCheckerCache = new ConcurrentHashMap<>();
    
    // SSH命令执行器
    private final SSHCommandExecutor sshCommandExecutor;
    
    /**
     * 初始化工厂并注册所有防火墙检查器
     */
    public FirewallCheckerFactory(SSHCommandExecutor sshCommandExecutor) {
        this.sshCommandExecutor = sshCommandExecutor;
        registerCheckers();
    }
    
    /**
     * 注册所有可用的防火墙检查器
     */
    private void registerCheckers() {
        // 注册CentOS防火墙检查器
        checkers.add(new CentOSFirewallChecker(sshCommandExecutor));
        
        // 注册Ubuntu防火墙检查器
        checkers.add(new UbuntuFirewallChecker(sshCommandExecutor));
        
        // 注册麒麟防火墙检查器
        checkers.add(new KylinFirewallChecker(sshCommandExecutor));
        
        logger.info("已注册{}个防火墙检查器", checkers.size());
    }
    
    /**
     * 获取适用于指定主机的防火墙检查器
     * @param hostInfo 主机信息
     * @param osInfo 操作系统信息
     * @return 适用的防火墙检查器，如果没有找到适用的检查器，则返回null
     */
    public IFirewallChecker getChecker(HostInfo hostInfo, OSInfo osInfo) {
        if (hostInfo == null) {
            logger.warn("主机信息为空，无法获取防火墙检查器");
            return null;
        }
        
        String hostname = hostInfo.getHostname();
        
        // 首先尝试从缓存中获取
        IFirewallChecker cachedChecker = hostCheckerCache.get(hostname);
        if (cachedChecker != null) {
            logger.debug("从缓存中获取主机{}的防火墙检查器: {}", hostname, cachedChecker.getClass().getSimpleName());
            return cachedChecker;
        }
        
        if (osInfo == null) {
            logger.warn("主机{}的操作系统信息为空，无法选择适当的防火墙检查器", hostname);
            return null;
        }
        
        // 遍历所有检查器，找到第一个适用的
        for (IFirewallChecker checker : checkers) {
            if (checker.isApplicable(osInfo)) {
                logger.info("为主机{}({})选择防火墙检查器: {}", 
                    hostname, osInfo.getDistro() + " " + osInfo.getFullVersion(), 
                    checker.getClass().getSimpleName());
                
                // 缓存检查器
                hostCheckerCache.put(hostname, checker);
                return checker;
            }
        }
        
        logger.warn("没有找到适用于主机{}({})的防火墙检查器", 
            hostname, osInfo.getDistro() + " " + osInfo.getFullVersion());
        return null;
    }
    
    /**
     * 清除主机的防火墙检查器缓存
     */
    public void clearCheckerCache(String hostname) {
        if (hostname != null && !hostname.isEmpty()) {
            hostCheckerCache.remove(hostname);
            logger.debug("已清除主机{}的防火墙检查器缓存", hostname);
        }
    }
    
    /**
     * SSH命令执行器实现
     * 用于通过SSH执行远程命令
     */
    public interface SSHCommandExecutor extends CentOSFirewallChecker.CommandExecutor, 
        UbuntuFirewallChecker.CommandExecutor, KylinFirewallChecker.CommandExecutor {
        
        /**
         * 执行SSH命令
         * @param command 要执行的命令
         * @return 命令执行结果
         */
        @Override
        CommandResult execute(String command) throws Exception;
    }
} 