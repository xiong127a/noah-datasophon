package com.datasophon.api.service.checker.checkers.selinux.factory;

import com.datasophon.api.service.checker.checkers.selinux.SELinuxCheckerStrategy;
import com.datasophon.api.service.checker.checkers.selinux.generic.GenericSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.centos.CentOSSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.kylin.KylinSELinuxChecker;
import com.datasophon.api.service.checker.checkers.selinux.os.ubuntu.UbuntuSELinuxChecker;
import com.datasophon.api.service.checker.common.OsInfo;
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
            logger.warn("操作系统信息为null，使用通用SELinux检查器");
            return new GenericSELinuxChecker();
        }

        String distribution = osInfo.getDistribution() != null ? osInfo.getDistribution().name().toLowerCase() : "";
        String version = osInfo.getVersionId();

        logger.info("为{}{}创建SELinux检查器", distribution, version);

        // 根据操作系统分发版选择合适的检查器
        if (distribution.contains("ubuntu")) {
            logger.info("使用Ubuntu专用的SELinux检查器");
            return new UbuntuSELinuxChecker();
        } else if (distribution.contains("centos")) {
            logger.info("使用CentOS专用的SELinux检查器");
            return new CentOSSELinuxChecker();
        } else if (distribution.contains("kylin")) {
            // 麒麟系统的SELinux检查
            if (version.startsWith("4")) {
                // Kylin V4基于CentOS
                logger.info("Kylin V4基于CentOS，使用CentOS专用的SELinux检查器");
                return new CentOSSELinuxChecker();
            } else if (version.startsWith("10")) {
                // Kylin V10基于Ubuntu
                logger.info("Kylin V10基于Ubuntu，使用Ubuntu专用的SELinux检查器");
                return new UbuntuSELinuxChecker();
            } else {
                // 其他麒麟版本使用专用检查器
                logger.info("使用Kylin专用的SELinux检查器");
                return new KylinSELinuxChecker();
            }
        } else if (distribution.contains("red hat") || distribution.contains("rhel")
                || distribution.contains("redhat")) {
            // Red Hat系统使用CentOS检查器（因为它们基于相同的底层架构）
            logger.info("使用CentOS专用的SELinux检查器（适用于Red Hat系统）");
            return new CentOSSELinuxChecker();
        } else if (distribution.contains("debian")) {
            // Debian系统使用Ubuntu检查器（因为它们有类似的结构）
            logger.info("使用Ubuntu专用的SELinux检查器（适用于Debian系统）");
            return new UbuntuSELinuxChecker();
        }

        // 默认返回通用检查器
        logger.info("未找到匹配的专用检查器，使用通用SELinux检查器");
        return new GenericSELinuxChecker();
    }
}