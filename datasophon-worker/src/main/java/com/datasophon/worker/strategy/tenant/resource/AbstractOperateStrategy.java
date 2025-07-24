package com.datasophon.worker.strategy.tenant.resource;

import cn.hutool.core.convert.Convert;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.utils.ExecResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public abstract class AbstractOperateStrategy implements ResourceOperateStrategy {

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

    public String kinitKbStr(String user) {
        return "kinit -kt /etc/security/keytab/" + user + ".service.keytab " + user + "/" + Convert.toStr(CacheUtils.get(Constants.HOSTNAME)) + "@HADOOP.COM";
    }

}
