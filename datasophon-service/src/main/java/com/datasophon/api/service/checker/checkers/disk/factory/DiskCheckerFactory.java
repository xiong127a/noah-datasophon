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
import com.datasophon.common.enums.LinuxDistribution;
import com.datasophon.common.model.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 磁盘检查器工厂
 * 根据操作系统类型创建相应的磁盘检查器
 */
public class DiskCheckerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DiskCheckerFactory.class);

    /**
     * 根据操作系统类型获取对应的磁盘检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应的磁盘检查器
     */
    public static DiskCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            logger.warn("操作系统信息为空，使用通用磁盘检查器");
            return new GenericDiskChecker();
        }

        LinuxDistribution distribution = osInfo.getDistributionType();
        String osVersion = osInfo.getVersionId();

        logger.info("为操作系统 {} {} 创建磁盘检查器", distribution, osVersion);

        // 使用switch语句根据操作系统类型创建对应的检查器
        if (distribution != null) {
            switch (distribution) {
                case CENTOS:
                case REDHAT:
                    // CentOS使用CentOS专用检查器
                    return new CentOSDiskChecker();
                case KYLIN:
                    // Kylin使用Kylin专用检查器
                    return new KylinDiskChecker();
                case UBUNTU:
                case DEBIAN:
                    // Ubuntu使用Ubuntu专用检查器
                    return new UbuntuDiskChecker();
                default:
                    // 其他操作系统使用通用检查器
                    logger.info("未找到适配的磁盘检查器，使用通用检查器");
                    return new GenericDiskChecker();
            }
        }

        // 操作系统类型为空，使用通用检查器
        logger.info("操作系统类型未识别，使用通用磁盘检查器");
        return new GenericDiskChecker();
    }
}