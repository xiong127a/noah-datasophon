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

import java.util.List;
import java.util.Optional;

/**
 * SELinux检查器工厂类
 * 根据操作系统类型创建对应的SELinux检查器实例
 */
public class SELinuxCheckerFactory {

    private static final Logger log = LoggerFactory.getLogger(SELinuxCheckerFactory.class);

    private final List<SELinuxCheckerStrategy> checkerStrategies;
    private final GenericSELinuxChecker defaultChecker;

    /**
     * 通过Spring注入的构造函数
     * 
     * @param checkerStrategies 所有注册的SELinux检查器策略
     */
    public SELinuxCheckerFactory(List<SELinuxCheckerStrategy> checkerStrategies) {
        this.checkerStrategies = checkerStrategies;

        // 查找默认的通用检查器作为备选
        this.defaultChecker = checkerStrategies.stream()
                .filter(checker -> checker instanceof GenericSELinuxChecker)
                .map(checker -> (GenericSELinuxChecker) checker)
                .findFirst()
                .orElse(new GenericSELinuxChecker());
    }

    /**
     * 根据操作系统类型获取对应的SELinux检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应的SELinux检查器，如果没有匹配的检查器则返回GenericSELinuxChecker
     */
    public SELinuxCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            log.info("操作系统信息为空，使用通用检查器");
            return defaultChecker;
        }

        // 获取操作系统发行版类型
        OsDistribution distribution = osInfo.getOsDistribution();
        String version = osInfo.getVersionId();

        // 先尝试找到完全匹配的检查器（同时匹配操作系统类型和版本前缀）
        Optional<SELinuxCheckerStrategy> specificChecker = findSpecificChecker(distribution, version);
        if (specificChecker.isPresent()) {
            return specificChecker.get();
        }

        // 如果没有找到特定版本的检查器，尝试找该操作系统类型的通用检查器
        Optional<SELinuxCheckerStrategy> genericOsChecker = findGenericOsChecker(distribution);
        if (genericOsChecker.isPresent()) {
            return genericOsChecker.get();
        }

        // 如果仍未找到，返回通用检查器
        log.info("未找到适配的SELinux检查器，使用通用检查器");
        return defaultChecker;
    }

    /**
     * 为兼容性提供的方法，调用getChecker
     * 
     * @param osInfo 操作系统信息
     * @return 对应的SELinux检查器
     */
    public SELinuxCheckerStrategy createSELinuxChecker(OsInfo osInfo) {
        return getChecker(osInfo);
    }

    /**
     * 查找特定操作系统版本的检查器
     */
    private Optional<SELinuxCheckerStrategy> findSpecificChecker(OsDistribution distribution, String version) {
        if (distribution == null || version == null) {
            return Optional.empty();
        }

        return checkerStrategies.stream()
                .filter(checker -> {
                    // 检查是否匹配操作系统类型和版本前缀
                    return distribution.equals(checker.getSupportedOs()) &&
                            checker.getVersionPrefix() != null &&
                            version.startsWith(checker.getVersionPrefix());
                })
                .findFirst();
    }

    /**
     * 查找操作系统类型的通用检查器
     */
    private Optional<SELinuxCheckerStrategy> findGenericOsChecker(OsDistribution distribution) {
        if (distribution == null) {
            return Optional.empty();
        }

        return checkerStrategies.stream()
                .filter(checker -> {
                    // 匹配操作系统类型且没有版本前缀（通用检查器）
                    return distribution.equals(checker.getSupportedOs()) &&
                            checker.getVersionPrefix() == null;
                })
                .findFirst();
    }
}