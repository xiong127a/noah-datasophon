package com.datasophon.api.service.checker.checkers.selinux.factory;

import com.datasophon.api.service.checker.checkers.selinux.SELinuxCheckerStrategy;
import com.datasophon.api.service.checker.checkers.selinux.generic.GenericSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.centos.CentOSSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.kylin.KylinSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.ubuntu.UbuntuSELinuxChecker;
import com.datasophon.common.enums.OsDistribution;
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
     * 根据操作系统类型创建SELinux检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应的SELinux检查器
     */
    public static SELinuxCheckerStrategy createSELinuxChecker(OsInfo osInfo) {
        if (osInfo == null) {
            logger.warn("操作系统信息为空，使用通用SELinux检查器");
            return new GenericSELinuxChecker();
        }

        OsDistribution distribution = osInfo.getOsDistribution();
        String osVersion = osInfo.getVersionId();

        logger.info("为操作系统 {} {} 创建SELinux检查器", distribution, osVersion);

        // 使用switch语句根据操作系统类型创建对应的检查器
        if (distribution != null) {
            switch (distribution) {
                case CENTOS:
                case REDHAT:
                    // CentOS使用CentOS专用检查器
                    return new CentOSSELinuxChecker();
                case FEDORA:
                    // Fedora使用CentOS专用检查器，因为它们有相同的SELinux实现
                    logger.info("为Fedora创建SELinux检查器（基于CentOS实现）");
                    return new CentOSSELinuxChecker();
                case KYLIN:
                    // Kylin使用Kylin专用检查器
                    return new KylinSELinuxChecker();
                case UBUNTU:
                case DEBIAN:
                    // Ubuntu使用Ubuntu专用检查器
                    return new UbuntuSELinuxChecker();
                default:
                    // 其他操作系统使用通用检查器
                    logger.info("未找到适配的SELinux检查器，使用通用检查器");
                    return new GenericSELinuxChecker();
            }
        }

        // 操作系统类型为空，使用通用检查器
        logger.info("操作系统类型未识别，使用通用SELinux检查器");
        return new GenericSELinuxChecker();
    }

    /**
     * 获取SELinux检查器（别名方法）
     * 与createSELinuxChecker功能相同，提供给使用getChecker名称的代码调用
     * 
     * @param osInfo 操作系统信息
     * @return 对应的SELinux检查器
     */
    public static SELinuxCheckerStrategy getChecker(OsInfo osInfo) {
        return createSELinuxChecker(osInfo);
    }
}