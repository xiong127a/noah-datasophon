package com.datasophon.worker.strategy.tenantResource;

import cn.hutool.core.convert.Convert;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.TaskConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public abstract class AbstractOperateStrategy implements ResourceOperateStrategy{

    ExecResult execResult;

    TenantFrameResource tenantResource;

    public Logger logger;

    public AbstractOperateStrategy(TenantFrameResource tenantResource) {
        if (tenantResource == null) {
            throw new IllegalArgumentException("Tenant resource cannot be null");
        }
        this.execResult = new ExecResult();
        this.tenantResource = tenantResource;
        String loggerName = String.format("%s-%s", "TenantResourceOperateLogger", tenantResource.getServiceName());
        logger = LoggerFactory.getLogger(loggerName);
    }

    public String convertGBToByte(String size) {
        return Convert.toStr(Long.parseLong(size) * 1024L * 1024L * 1024L);
    }

}
