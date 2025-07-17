package com.datasophon.api.service.checker.checkers.firewall.factory;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.OsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 防火墙检查器工厂类
 * 根据操作系统类型创建对应的防火墙检查器
 */
public class FirewallCheckerFactory {

    private static final Logger log = LoggerFactory.getLogger(FirewallCheckerFactory.class);

    private final List<FirewallCheckerStrategy> checkerStrategies;
    private final GenericFirewallChecker defaultChecker;

    /**
     * 通过Spring注入的构造函数
     * 
     * @param checkerStrategies 所有注册的防火墙检查器策略
     */
    public FirewallCheckerFactory(List<FirewallCheckerStrategy> checkerStrategies) {
        this.checkerStrategies = checkerStrategies;

        // 查找默认的通用检查器作为备选
        this.defaultChecker = checkerStrategies.stream()
                .filter(checker -> checker instanceof GenericFirewallChecker)
                .map(checker -> (GenericFirewallChecker) checker)
                .findFirst()
                .orElse(new GenericFirewallChecker());
    }

    /**
     * 根据操作系统类型获取对应的防火墙检查器
     * 
     * @param osInfo 操作系统信息
     * @return 对应的防火墙检查器，如果没有匹配的检查器则返回GenericFirewallChecker
     */
    public FirewallCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            log.info("操作系统信息为空，使用通用检查器");
            return defaultChecker;
        }

        // 获取操作系统发行版类型
        OsDistribution distribution = osInfo.getOsDistribution();
        String version = osInfo.getVersionId();

        // 先尝试找到完全匹配的检查器（同时匹配操作系统类型和版本前缀）
        Optional<FirewallCheckerStrategy> specificChecker = findSpecificChecker(distribution, version);
        if (specificChecker.isPresent()) {
            return specificChecker.get();
        }

        // 如果没有找到特定版本的检查器，尝试找该操作系统类型的通用检查器
        Optional<FirewallCheckerStrategy> genericOsChecker = findGenericOsChecker(distribution);
        if (genericOsChecker.isPresent()) {
            return genericOsChecker.get();
        }

        // 如果仍未找到，返回通用检查器
        log.info("未找到适配的防火墙检查器，使用通用检查器");
        return defaultChecker;
    }

    /**
     * 为兼容性提供的方法，调用getChecker
     * 
     * @param osInfo 操作系统信息
     * @return 对应的防火墙检查器
     */
    public FirewallCheckerStrategy createFirewallChecker(OsInfo osInfo) {
        return getChecker(osInfo);
    }

    /**
     * 查找特定操作系统版本的检查器
     */
    private Optional<FirewallCheckerStrategy> findSpecificChecker(OsDistribution distribution, String version) {
        if (distribution == null || version == null) {
            return Optional.empty();
        }

        return checkerStrategies.stream()
                .filter(checker -> {
                    try {
                        // 反射获取支持的操作系统类型
                        java.lang.reflect.Method getSupportedOsMethod = checker.getClass().getMethod("getSupportedOs");
                        OsDistribution supportedOs = (OsDistribution) getSupportedOsMethod.invoke(checker);

                        // 反射获取版本前缀
                        java.lang.reflect.Method getVersionPrefixMethod = checker.getClass()
                                .getMethod("getVersionPrefix");
                        String versionPrefix = (String) getVersionPrefixMethod.invoke(checker);

                        // 检查是否匹配操作系统类型和版本前缀
                        return distribution.equals(supportedOs) &&
                                versionPrefix != null &&
                                version.startsWith(versionPrefix);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();
    }

    /**
     * 查找操作系统类型的通用检查器
     */
    private Optional<FirewallCheckerStrategy> findGenericOsChecker(OsDistribution distribution) {
        if (distribution == null) {
            return Optional.empty();
        }

        return checkerStrategies.stream()
                .filter(checker -> {
                    try {
                        // 反射获取支持的操作系统类型
                        java.lang.reflect.Method getSupportedOsMethod = checker.getClass().getMethod("getSupportedOs");
                        OsDistribution supportedOs = (OsDistribution) getSupportedOsMethod.invoke(checker);

                        // 检查是否有getVersionPrefix方法，如果没有表示是通用检查器
                        boolean hasVersionPrefix;


                            hasVersionPrefix = true;


                        // 匹配操作系统类型且没有版本前缀（通用检查器）
                        return distribution.equals(supportedOs) && !hasVersionPrefix;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();
    }
}