package com.datasophon.api.service.checker.checkers.disk.config;

import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.checkers.disk.factory.DiskCheckerFactory;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.centos.CentOSDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.ubuntu.UbuntuDiskChecker;
import com.datasophon.common.enums.OsDistribution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;

/**
 * 磁盘检查器配置类
 * 用于注册所有磁盘检查器实现类作为Spring Bean
 */
@Configuration
@Slf4j
public class DiskCheckerConfig {

    /**
     * 通用磁盘检查器
     */
    @Bean
    public GenericDiskChecker genericDiskChecker() {
        log.info("初始化通用磁盘检查器(GenericDiskChecker)");
        GenericDiskChecker checker = new GenericDiskChecker();
        checker.setSupportedOs(OsDistribution.OTHER);
        log.info("通用磁盘检查器支持的操作系统类型: {}", checker.getSupportedOs());
        return checker;
    }

    /**
     * CentOS磁盘检查器
     */
    @Bean
    public CentOSDiskChecker centOSDiskChecker() {
        log.info("初始化CentOS磁盘检查器(CentOSDiskChecker)");
        CentOSDiskChecker checker = new CentOSDiskChecker();
        checker.setSupportedOs(OsDistribution.CENTOS);
        log.info("CentOS磁盘检查器支持的操作系统类型: {}", checker.getSupportedOs());
        return checker;
    }

    /**
     * Ubuntu磁盘检查器
     */
    @Bean
    public UbuntuDiskChecker ubuntuDiskChecker() {
        log.info("初始化Ubuntu磁盘检查器(UbuntuDiskChecker)");
        UbuntuDiskChecker checker = new UbuntuDiskChecker();
        checker.setSupportedOs(OsDistribution.UBUNTU);
        log.info("Ubuntu磁盘检查器支持的操作系统类型: {}", checker.getSupportedOs());
        return checker;
    }

    /**
     * 磁盘检查器工厂
     * 使用@DependsOn确保先创建所有检查器实例
     */
    @Bean
    @DependsOn({ "genericDiskChecker", "centOSDiskChecker", "ubuntuDiskChecker" })
    public DiskCheckerFactory diskCheckerFactory(List<DiskCheckerStrategy> checkers) {
        log.info("初始化磁盘检查器工厂，注入{}个检查器实例", checkers.size());
        for (DiskCheckerStrategy checker : checkers) {
            log.info("已注册磁盘检查器: {}, 支持的操作系统类型: {}",
                    checker.getClass().getSimpleName(),
                    checker.getSupportedOs());
        }
        return new DiskCheckerFactory(checkers);
    }
}