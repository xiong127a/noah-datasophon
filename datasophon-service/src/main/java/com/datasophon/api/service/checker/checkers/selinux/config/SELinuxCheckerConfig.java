package com.datasophon.api.service.checker.checkers.selinux.config;

import com.datasophon.api.service.checker.checkers.selinux.SELinuxCheckerStrategy;
import com.datasophon.api.service.checker.checkers.selinux.factory.SELinuxCheckerFactory;
import com.datasophon.api.service.checker.checkers.selinux.generic.GenericSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.centos.CentOSSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.kylin.KylinSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.ubuntu.UbuntuSELinuxChecker;
import com.datasophon.common.enums.OsDistribution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SELinux检查器配置类
 * 用于注册所有SELinux检查器实现类作为Spring Bean
 * 
 * @author 63588
 */
@Configuration
public class SELinuxCheckerConfig {

    /**
     * 通用SELinux检查器
     */
    @Bean
    public GenericSELinuxChecker genericSELinuxChecker(SELinuxCheckerFactory selinuxCheckerFactory) {
        GenericSELinuxChecker checker = new GenericSELinuxChecker(selinuxCheckerFactory);
        checker.setSupportedOs(OsDistribution.OTHER);
        return checker;
    }

    /**
     * CentOS SELinux检查器
     */
    @Bean
    public CentOSSELinuxChecker centOSSELinuxChecker(SELinuxCheckerFactory selinuxCheckerFactory) {
        CentOSSELinuxChecker checker = new CentOSSELinuxChecker(selinuxCheckerFactory);
        checker.setSupportedOs(OsDistribution.CENTOS);
        return checker;
    }

    /**
     * Kylin SELinux检查器
     */
    @Bean
    public KylinSELinuxChecker kylinSELinuxChecker(SELinuxCheckerFactory selinuxCheckerFactory) {
        KylinSELinuxChecker checker = new KylinSELinuxChecker(selinuxCheckerFactory);
        checker.setSupportedOs(OsDistribution.KYLIN);
        return checker;
    }

    /**
     * Ubuntu SELinux检查器
     */
    @Bean
    public UbuntuSELinuxChecker ubuntuSELinuxChecker(SELinuxCheckerFactory selinuxCheckerFactory) {
        UbuntuSELinuxChecker checker = new UbuntuSELinuxChecker(selinuxCheckerFactory);
        checker.setSupportedOs(OsDistribution.UBUNTU);
        return checker;
    }

    /**
     * SELinux检查器工厂
     */
    @Bean
    public SELinuxCheckerFactory selinuxCheckerFactory(List<SELinuxCheckerStrategy> checkers) {
        return new SELinuxCheckerFactory(checkers);
    }
}