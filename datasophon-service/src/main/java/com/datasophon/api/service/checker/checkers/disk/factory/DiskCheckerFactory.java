package com.datasophon.api.service.checker.checkers.disk.factory;

import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.OsInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 磁盘检查器工厂
 * 根据操作系统类型创建对应的磁盘检查器
 */
@Slf4j
public class DiskCheckerFactory {

    private final List<DiskCheckerStrategy> diskCheckers;

    private final Map<String, DiskCheckerStrategy> checkerCache = new ConcurrentHashMap<>();
    private final GenericDiskChecker genericDiskChecker;

    public DiskCheckerFactory(List<DiskCheckerStrategy> diskCheckers, List<DiskCheckerStrategy> checkers) {
        this.diskCheckers = diskCheckers;
        this.genericDiskChecker = checkers.stream()
                .filter(checker -> checker instanceof GenericDiskChecker)
                .map(checker -> (GenericDiskChecker) checker)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到GenericDiskChecker实例"));
    }

    /**
     * 获取磁盘检查器
     *
     * @param osInfo 操作系统信息
     * @return 磁盘检查器实例
     */
    public DiskCheckerStrategy getChecker(OsInfo osInfo) {
        if (osInfo == null) {
            log.warn("操作系统信息为空，使用通用检查器");
            return genericDiskChecker;
        }

        // 获取操作系统分发版本类型
        OsDistribution osDistribution = osInfo.getOsDistribution();
        log.debug("获取到操作系统类型: {}", osDistribution);

        // 遍历所有检查器，找到支持当前操作系统的检查器
        for (DiskCheckerStrategy checker : diskCheckers) {
            if (checker.getSupportedOs() != null &&
                    checker.getSupportedOs().equals(osDistribution)) {
                log.info("使用{}检查器检查磁盘", checker.getClass().getSimpleName());
                return checker;
            }
        }

        // 如果没有找到精确匹配的检查器，尝试更一般的类型匹配
        // 例如，如果系统是CENTOS7但没有找到CENTOS7检查器，试着找CENTOS检查器
        for (DiskCheckerStrategy checker : diskCheckers) {
            if (checker.getSupportedOs() != null &&
                    osDistribution.toString().startsWith(checker.getSupportedOs().toString())) {
                log.info("使用{}检查器检查磁盘(基本类型匹配)", checker.getClass().getSimpleName());
                return checker;
            }
        }

        // 如果没有找到匹配的检查器，使用通用检查器
        log.warn("未找到支持{}的磁盘检查器，使用通用检查器", osDistribution);
        return genericDiskChecker;
    }

    /**
     * 清除检查器缓存
     */
    public void clearCache() {
        checkerCache.clear();
        log.debug("已清除磁盘检查器缓存");
    }
}