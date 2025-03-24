package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.common.LinuxDistribution;
import com.datasophon.api.service.checker.common.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 防火墙检查器工厂类
 * 根据不同的操作系统类型，生成适合的防火墙检查器实现
 */
@Component
public class FirewallCheckerFactory {

    private static final Logger logger = LoggerFactory.getLogger(FirewallCheckerFactory.class);

    /**
     * 获取适合当前系统的防火墙检查器实现
     * 
     * @param osInfo 操作系统信息
     * @return 防火墙检查器实现
     */
    public FirewallChecker getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            logger.warn("无法获取操作系统信息，使用通用防火墙检查器");
            return new FirewallChecker();
        }

        logger.info("根据操作系统类型选择防火墙检查器: {}", osInfo.getFullName());

        // 根据操作系统分发版本选择对应的检查器
        String distributionId = osInfo.getDistributionId();
        if (distributionId == null) {
            logger.warn("无法获取操作系统分发版本信息，使用通用防火墙检查器");
            return new FirewallChecker();
        }

        if (distributionId.toLowerCase().contains("centos")) {
            logger.info("检测到CentOS系统，使用CentOS防火墙检查器");
            return new CentOSFirewallChecker();
        } else if (distributionId.toLowerCase().contains("ubuntu")) {
            logger.info("检测到Ubuntu系统，目前使用通用防火墙检查器");
            // 未来可以添加Ubuntu特定实现
            return new FirewallChecker();
        } else if (distributionId.toLowerCase().contains("kylin")) {
            logger.info("检测到Kylin系统，目前使用通用防火墙检查器");
            // 未来可以添加Kylin特定实现
            return new FirewallChecker();
        } else {
            logger.info("使用通用防火墙检查器");
            return new FirewallChecker();
        }
    }
}