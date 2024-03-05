package com.datasophon.worker.strategy.tenantResource;

import cn.hutool.core.convert.Convert;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.utils.ExecResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractOperateStrategy implements ResourceOperateStrategy{

    ExecResult execResult;

    TenantFrameResource tenantResource;

    public AbstractOperateStrategy(TenantFrameResource tenantResource) {
        if (tenantResource == null) {
            throw new IllegalArgumentException("Tenant resource cannot be null");
        }
        this.execResult = new ExecResult();
        this.tenantResource = tenantResource;
    }

    public String convertGBToByte(String size) {
        return Convert.toStr(Long.parseLong(size) * 1024L * 1024L * 1024L);
    }

}
