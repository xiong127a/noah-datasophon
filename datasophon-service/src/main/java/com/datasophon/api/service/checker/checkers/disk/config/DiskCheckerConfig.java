package com.datasophon.api.service.checker.checkers.disk.config;

import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.checkers.disk.factory.DiskCheckerFactory;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.centos.CentOSDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.ubuntu.UbuntuDiskChecker;
import com.datasophon.common.enums.OsDistribution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 磁盘检查器配置类
 * 用于注册所有磁盘检查器实现类作为Spring Bean
 */
@Configuration
public class DiskCheckerConfig {

    /**
     * 通用磁盘检查器
     */
    @Bean
    public GenericDiskChecker genericDiskChecker() {
        GenericDiskChecker checker = new GenericDiskChecker();
        checker.setSupportedOs(OsDistribution.OTHER);
        return checker;
    }

    /**
     * CentOS磁盘检查器
     */
    @Bean
    public CentOSDiskChecker centOSDiskChecker() {
        CentOSDiskChecker checker = new CentOSDiskChecker();
        checker.setSupportedOs(OsDistribution.CENTOS);
        return checker;
    }

    /**
     * Ubuntu磁盘检查器
     */
    @Bean
    public UbuntuDiskChecker ubuntuDiskChecker() {
        UbuntuDiskChecker checker = new UbuntuDiskChecker();
        checker.setSupportedOs(OsDistribution.UBUNTU);
        return checker;
    }

    /**
     * 磁盘检查器工厂
     */
    @Bean
    public DiskCheckerFactory diskCheckerFactory(List<DiskCheckerStrategy> checkers) {
        return new DiskCheckerFactory(checkers);
    }
}