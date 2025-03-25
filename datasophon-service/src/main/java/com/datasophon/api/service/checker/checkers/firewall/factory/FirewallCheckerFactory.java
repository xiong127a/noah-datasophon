package com.datasophon.api.service.checker.checkers.firewall.factory;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS7FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS8FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOSFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.ubuntu.UbuntuFirewallChecker;
import com.datasophon.api.service.checker.common.LinuxDistribution;
import com.datasophon.api.service.checker.common.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 防火墙检查器工厂类
 * 根据操作系统类型创建对应的防火墙检查器
 */
public class FirewallCheckerFactory {

    private static final Logger log = LoggerFactory.getLogger(FirewallCheckerFactory.class);

    /**
     * 创建防火墙检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应系统类型的防火墙检查器
     */
    public static FirewallCheckerStrategy createFirewallChecker(OsInfo osInfo) {
        if (osInfo == null) {
            log.warn("操作系统信息为空，使用通用防火墙检查器");
            return new GenericFirewallChecker();
        }

        LinuxDistribution distribution = osInfo.getDistribution();
        String osVersion = osInfo.getVersionId();

        log.info("为操作系统 {} {} 创建防火墙检查器", distribution, osVersion);

        // 根据操作系统类型创建对应的检查器
        switch (distribution) {
            case CENTOS:
                return createCentOSFirewallChecker(osVersion);
            case UBUNTU:
                return createUbuntuFirewallChecker(osVersion);
            case KYLIN:
                return createKylinFirewallChecker(osVersion);
            default:
                log.info("未找到适配的防火墙检查器，使用通用检查器");
                return new GenericFirewallChecker();
        }
    }

    /**
     * 创建CentOS系统的防火墙检查器
     */
    private static FirewallCheckerStrategy createCentOSFirewallChecker(String version) {
        if (version != null) {
            if (version.startsWith("7")) {
                log.info("创建CentOS 7防火墙检查器");
                return new CentOS7FirewallChecker();
            } else if (version.startsWith("8")) {
                log.info("创建CentOS 8防火墙检查器");
                return new CentOS8FirewallChecker();
            }
        }

        log.info("创建通用CentOS防火墙检查器");
        return new CentOSFirewallChecker();
    }

    /**
     * 创建Ubuntu系统的防火墙检查器
     */
    private static FirewallCheckerStrategy createUbuntuFirewallChecker(String version) {
        // 目前只有通用Ubuntu防火墙检查器，未来可以添加针对特定版本的实现
        log.info("创建Ubuntu防火墙检查器");
        return new UbuntuFirewallChecker();
    }

    /**
     * 创建麒麟系统的防火墙检查器
     */
    private static FirewallCheckerStrategy createKylinFirewallChecker(String version) {
        // 目前使用CentOS防火墙检查器，因为麒麟系统基于CentOS
        log.info("为麒麟系统创建基于CentOS的防火墙检查器");
        return new CentOSFirewallChecker();
    }
}