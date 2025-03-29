package com.datasophon.api.service.checker.checkers.selinux.factory;

import com.datasophon.api.service.checker.checkers.selinux.SELinuxCheckerStrategy;
import com.datasophon.api.service.checker.checkers.selinux.generic.GenericSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.centos.CentOSSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.kylin.KylinSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.ubuntu.UbuntuSELinuxChecker;
import com.datasophon.common.model.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SELinux检查器工厂类
 * 根据操作系统类型创建对应的SELinux检查器实例
 */
public class SELinuxCheckerFactory {

    private static final Logger logger = LoggerFactory.getLogger(SELinuxCheckerFactory.class);

    /**
     * 获取适用于指定操作系统的SELinux检查器
     * 
     * @param osInfo 操作系统信息
     * @return 适用于该操作系统的SELinux检查器策略
     */
    public static SELinuxCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            logger.warn("操作系统信息为空，使用通用SELinux检查器");
            return new GenericSELinuxChecker();
        }

        OsInfo.LinuxDistribution distribution = osInfo.getDistributionType();
        String distributionName = distribution.toString();
        String osVersion = osInfo.getVersionId();

        logger.info("为操作系统 {} {} 创建SELinux检查器", distributionName, osVersion);

        // 根据操作系统类型创建对应的检查器
        switch (distribution) {
            case CENTOS:
            case REDHAT:
                logger.info("创建CentOS/RHEL SELinux检查器");
                return new CentOSSELinuxChecker();
            case KYLIN:
                logger.info("创建麒麟系统SELinux检查器");
                return new KylinSELinuxChecker();
            case UBUNTU:
            case DEBIAN:
                logger.info("创建Ubuntu/Debian SELinux检查器");
                return new UbuntuSELinuxChecker();
            default:
                logger.info("未找到适配的SELinux检查器，使用通用检查器");
                return new GenericSELinuxChecker();
        }
    }
}