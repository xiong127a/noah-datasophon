package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.ItemChecker;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 检查器工厂类
 * 根据ItemCode获取对应的检查器实现
 */
@Component
public class ItemCheckerFactory {
    private static final Logger logger = LoggerFactory.getLogger(ItemCheckerFactory.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final Map<ItemCode, ItemChecker> checkerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        logger.info("初始化检查器工厂...");
        
        // 获取所有ItemChecker实现
        Map<String, ItemChecker> checkers = applicationContext.getBeansOfType(ItemChecker.class);
        
        for (ItemChecker checker : checkers.values()) {
            try {
                ItemCode itemCode = checker.getCheckerType();
                
                if (itemCode != null) {
                    checkerMap.put(itemCode, checker);
                    logger.info("注册检查器: {} -> {}", itemCode, checker.getClass().getSimpleName());
                } else {
                    logger.warn("检查器未定义类型: {}", checker.getClass().getName());
                }
            } catch (Exception e) {
                logger.error("注册检查器时发生异常: {}", checker.getClass().getName(), e);
            }
        }
        
        logger.info("检查器工厂初始化完成，共注册 {} 个检查器", checkerMap.size());
    }
    
    /**
     * 根据ItemCode获取对应的检查器
     * @param itemCode 检查项代码
     * @return 对应的检查器，如果不存在则返回null
     */
    public ItemChecker getChecker(ItemCode itemCode) {
        return checkerMap.get(itemCode);
    }
} 