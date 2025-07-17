package com.datasophon.api.service.checker.checkers.firewall.config;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.checkers.firewall.factory.FirewallCheckerFactory;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS7FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOS8FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOSFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.kylin.KylinV4FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.kylin.KylinV10FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.ubuntu.UbuntuFirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.ubuntu.Ubuntu22FirewallChecker;
import com.datasophon.api.service.checker.checkers.firewall.os.ubuntu.Ubuntu24FirewallChecker;
import com.datasophon.common.enums.OsDistribution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 防火墙检查器配置类
 * 用于注册所有防火墙检查器实现类作为Spring Bean
 */
@Configuration
public class FirewallCheckerConfig {

    /**
     * 通用防火墙检查器
     */
    @Bean
    public GenericFirewallChecker genericFirewallChecker() {
        GenericFirewallChecker checker = new GenericFirewallChecker();
        checker.setSupportedOs(OsDistribution.OTHER);
        return checker;
    }

    /**
     * CentOS通用防火墙检查器
     */
    @Bean
    public CentOSFirewallChecker centOSFirewallChecker() {
        CentOSFirewallChecker checker = new CentOSFirewallChecker();
        checker.setSupportedOs(OsDistribution.CENTOS);
        return checker;
    }

    /**
     * CentOS 7防火墙检查器
     */
    @Bean
    public CentOS7FirewallChecker centOS7FirewallChecker() {
        CentOS7FirewallChecker checker = new CentOS7FirewallChecker();
        checker.setSupportedOs(OsDistribution.CENTOS);
        checker.setVersionPrefix("7");
        return checker;
    }

    /**
     * CentOS 8防火墙检查器
     */
    @Bean
    public CentOS8FirewallChecker centOS8FirewallChecker() {
        CentOS8FirewallChecker checker = new CentOS8FirewallChecker();
        checker.setSupportedOs(OsDistribution.CENTOS);
        checker.setVersionPrefix("8");
        return checker;
    }

    /**
     * Ubuntu通用防火墙检查器
     */
    @Bean
    public UbuntuFirewallChecker ubuntuFirewallChecker() {
        UbuntuFirewallChecker checker = new UbuntuFirewallChecker();
        checker.setSupportedOs(OsDistribution.UBUNTU);
        return checker;
    }

    /**
     * Ubuntu 22防火墙检查器
     */
    @Bean
    public Ubuntu22FirewallChecker ubuntu22FirewallChecker() {
        Ubuntu22FirewallChecker checker = new Ubuntu22FirewallChecker();
        checker.setSupportedOs(OsDistribution.UBUNTU);
        checker.setVersionPrefix("22");
        return checker;
    }

    /**
     * Ubuntu 24防火墙检查器
     */
    @Bean
    public Ubuntu24FirewallChecker ubuntu24FirewallChecker() {
        Ubuntu24FirewallChecker checker = new Ubuntu24FirewallChecker();
        checker.setSupportedOs(OsDistribution.UBUNTU);
        checker.setVersionPrefix("24");
        return checker;
    }

    /**
     * Kylin V4防火墙检查器
     */
    @Bean
    public KylinV4FirewallChecker kylinV4FirewallChecker() {
        KylinV4FirewallChecker checker = new KylinV4FirewallChecker();
        checker.setSupportedOs(OsDistribution.KYLIN);
        checker.setVersionPrefix("4");
        return checker;
    }

    /**
     * Kylin V10防火墙检查器
     */
    @Bean
    public KylinV10FirewallChecker kylinV10FirewallChecker() {
        KylinV10FirewallChecker checker = new KylinV10FirewallChecker();
        checker.setSupportedOs(OsDistribution.KYLIN);
        checker.setVersionPrefix("10");
        return checker;
    }

    /**
     * 防火墙检查器工厂
     */
    @Bean
    public FirewallCheckerFactory firewallCheckerFactory(List<FirewallCheckerStrategy> checkers) {
        return new FirewallCheckerFactory(checkers);
    }
}