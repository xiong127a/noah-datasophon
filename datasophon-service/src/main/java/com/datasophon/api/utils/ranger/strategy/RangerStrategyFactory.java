package com.datasophon.api.utils.ranger.strategy;

import java.lang.reflect.Constructor;

public class RangerStrategyFactory {

    public static AbstractRangerStrategy createRangerStrategy(String serviceName, Integer clusterId) throws Exception {
        String fullClassName = serviceName + "RangerStrategy";
        Class<?> strategyClass = Class.forName(fullClassName);
        Constructor<?> constructor = strategyClass.getConstructor(Integer.class);
        return (AbstractRangerStrategy) constructor.newInstance(clusterId);
    }

}
