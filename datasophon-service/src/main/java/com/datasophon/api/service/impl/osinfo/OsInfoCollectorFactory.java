package com.datasophon.api.service.impl.osinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 操作系统信息收集器工厂
 * 根据操作系统类型返回对应的收集器实现
 */
@Component
public class OsInfoCollectorFactory {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoCollectorFactory.class);

    @Autowired
    private List<IOsInfoCollector> collectors;

    /**
     * 根据操作系统类型获取对应的收集器
     * 
     * @param osType 操作系统类型："linux" 或 "windows"
     * @return 对应的操作系统信息收集器，如果找不到则返回null
     */
    public IOsInfoCollector getCollector(String osType) {
        if (osType == null) {
            return null;
        }

        for (IOsInfoCollector collector : collectors) {
            if (osType.equalsIgnoreCase(collector.getSupportedOsType())) {
                logger.debug("找到支持{}操作系统的收集器: {}", osType, collector.getClass().getSimpleName());
                return collector;
            }
        }

        logger.warn("未找到支持{}操作系统的收集器", osType);
        return null;
    }
}