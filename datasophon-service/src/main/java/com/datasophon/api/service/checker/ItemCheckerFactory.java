package com.datasophon.api.service.checker;

import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class ItemCheckerFactory {
    private static final Logger logger = LoggerFactory.getLogger(ItemCheckerFactory.class);
    
    private final Map<ItemCode, ItemChecker> checkerMap = new HashMap<>();
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        Map<String, AbstractItemChecker> checkers = applicationContext.getBeansOfType(AbstractItemChecker.class);
        for (AbstractItemChecker checker : checkers.values()) {
            try {
                ItemCode itemCode = checker.getCheckerType();
                checkerMap.put(itemCode, checker);
                logger.info("注册检查器: {} -> {}", itemCode, checker.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("注册检查器失败: {}", checker.getClass().getSimpleName(), e);
            }
        }
    }
    
    public ItemChecker getChecker(ItemCode itemCode) {
        ItemChecker checker = checkerMap.get(itemCode);
        if (checker == null) {
            logger.warn("未找到检查器: {}", itemCode);
        }
        return checker;
    }
} 