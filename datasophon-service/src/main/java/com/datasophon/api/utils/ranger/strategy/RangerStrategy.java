package com.datasophon.api.utils.ranger.strategy;

import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.utils.ExecResult;

public interface RangerStrategy {

    ExecResult createService() throws Exception;

    ExecResult operatePolicy(TenantResource resource) throws Exception;

}
