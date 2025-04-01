package com.datasophon.api.service.checker.checkers.firewall.factory;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS7FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS8FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOSFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.ubuntu.UbuntuFirewallChecker;
import com.datasophon.common.enums.LinuxDistribution;
import com.datasophon.common.model.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 防火墙检查器工厂类
 * 根据操作系统类型创建对应的防火墙检查器
 */
public class FirewallCheckerFactory {

    private static final Logger log = LoggerFactory.getLogger(FirewallCheckerFactory.class);

    /**
     * 根据操作系统类型获取对应的防火墙检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应的防火墙检查器，如果没有匹配的检查器则返回GenericFirewallChecker
     */
    public static FirewallCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            return new GenericFirewallChecker();
        }

        // 获取操作系统发行版类型
        LinuxDistribution distribution = osInfo.getDistributionType();

        // 使用switch语句根据操作系统类型选择对应的检查器
        if (distribution != null) {
            switch (distribution) {
                case CENTOS:
                case REDHAT:
                    return createCentOSFirewallChecker(osInfo.getVersionId());
                case UBUNTU:
                case DEBIAN:
                    return createUbuntuFirewallChecker(osInfo.getVersionId());
                case KYLIN:
                    return createKylinFirewallChecker(osInfo.getVersionId());
                default:
                    log.info("未找到适配的防火墙检查器，使用通用检查器");
                    return new GenericFirewallChecker();
            }
        }

        log.info("未找到适配的防火墙检查器，使用通用检查器");
        return new GenericFirewallChecker();
    }

    /**
     * 为兼容性提供的方法，调用getChecker
     * 
     * @param osInfo 操作系统信息
     * @return 对应的防火墙检查器
     */
    public static FirewallCheckerStrategy createFirewallChecker(OsInfo osInfo) {
        return getChecker(osInfo);
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