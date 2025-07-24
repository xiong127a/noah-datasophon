package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.model.tenant.resource.TenantFrameResource;

import java.lang.reflect.Constructor;

public class OperateStrategyFactory {

    public static AbstractOperateStrategy createOperateStrategy(String serviceName, TenantFrameResource tenantResource) throws Exception {
        String packageName = "com.datasophon.worker.strategy.tenantResource.";
        String fullClassName = packageName + serviceName + "ResourceOperateStrategy";
        Class<?> strategyClass = Class.forName(fullClassName);
        Constructor<?> constructor = strategyClass.getConstructor(TenantFrameResource.class);
        return (AbstractOperateStrategy) constructor.newInstance(tenantResource);
    }

}
