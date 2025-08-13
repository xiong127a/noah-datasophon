package com.datasophon.api.utils.ranger.strategy;

import java.lang.reflect.Constructor;

public class RangerStrategyFactory {

    public static AbstractRangerStrategy createRangerStrategy(String serviceName, Long clusterId) throws Exception {
        String packageName = "com.datasophon.api.utils.ranger.strategy.";
        String fullClassName = packageName + serviceName + "RangerStrategy";
        Class<?> strategyClass = Class.forName(fullClassName);
        Constructor<?> constructor = strategyClass.getConstructor(Integer.class);
        return (AbstractRangerStrategy) constructor.newInstance(clusterId);
    }

}
