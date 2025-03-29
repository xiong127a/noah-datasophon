package com.datasophon.api.service.checker.checkers.disk.factory;

import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.centos.CentOS7DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.centos.CentOS8DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.centos.CentOSDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.kylin.KylinDiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.kylin.KylinV10DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.kylin.KylinV4DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.ubuntu.Ubuntu22DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.ubuntu.Ubuntu24DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.os.ubuntu.UbuntuDiskChecker;
import com.datasophon.common.model.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 磁盘检查器工厂
 * 根据操作系统类型创建相应的磁盘检查器
 */
public class DiskCheckerFactory {

    private static final Logger log = LoggerFactory.getLogger(DiskCheckerFactory.class);

    /**
     * 根据操作系统信息获取合适的磁盘检查器
     * 
     * @param osInfo 操作系统信息
     * @return 磁盘检查器实例
     */
    public static DiskCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            log.warn("未获取到操作系统信息，使用通用磁盘检查器");
            return new GenericDiskChecker();
        }

        OsInfo.LinuxDistribution distribution = osInfo.getDistributionType();
        if (distribution == null) {
            log.warn("未能识别操作系统类型，使用通用磁盘检查器");
            return new GenericDiskChecker();
        }

        // 根据Linux发行版类型返回相应的磁盘检查器
        switch (distribution) {
            case CENTOS:
                log.info("检测到CentOS系统，版本: {}", osInfo.getVersionId());

                // CentOS 7
                if (osInfo.isVersion("7")) {
                    log.info("使用CentOS 7专用磁盘检查器");
                    return new CentOS7DiskChecker();
                }
                // CentOS 8
                else if (osInfo.isVersion("8")) {
                    log.info("使用CentOS 8专用磁盘检查器");
                    return new CentOS8DiskChecker();
                }
                // 其他CentOS版本
                else {
                    log.warn("未知的CentOS版本: {}, 使用通用CentOS磁盘检查器", osInfo.getVersionId());
                    return new CentOSDiskChecker();
                }

            case UBUNTU:
                log.info("检测到Ubuntu系统，版本: {}", osInfo.getVersionId());

                // Ubuntu 22.04
                if (osInfo.isVersion("22.04") || osInfo.isVersion("22")) {
                    log.info("使用Ubuntu 22专用磁盘检查器");
                    return new Ubuntu22DiskChecker();
                }
                // Ubuntu 24.04
                else if (osInfo.isVersion("24.04") || osInfo.isVersion("24")) {
                    log.info("使用Ubuntu 24专用磁盘检查器");
                    return new Ubuntu24DiskChecker();
                }
                // 其他Ubuntu版本
                else {
                    log.warn("未知的Ubuntu版本: {}, 使用通用Ubuntu磁盘检查器", osInfo.getVersionId());
                    return new UbuntuDiskChecker();
                }

            case KYLIN:
                log.info("检测到Kylin系统，版本: {}", osInfo.getVersionId());

                // Kylin V4
                if (osInfo.isVersion("4") || osInfo.getVersionId().startsWith("4.")) {
                    log.info("使用Kylin V4专用磁盘检查器");
                    return new KylinV4DiskChecker();
                }
                // Kylin V10
                else if (osInfo.isVersion("10") || osInfo.getVersionId().startsWith("10.")) {
                    log.info("使用Kylin V10专用磁盘检查器");
                    return new KylinV10DiskChecker();
                }
                // 其他Kylin版本
                else {
                    log.warn("未知的Kylin版本: {}, 使用通用Kylin磁盘检查器", osInfo.getVersionId());
                    return new KylinDiskChecker();
                }

            default:
                log.info("未找到{}操作系统专用磁盘检查器，使用通用磁盘检查器", distribution);
                return new GenericDiskChecker();
        }
    }
}