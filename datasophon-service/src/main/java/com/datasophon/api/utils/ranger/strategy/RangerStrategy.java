package com.datasophon.api.utils.ranger.strategy;

import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.common.utils.ExecResult;

public interface RangerStrategy {

    ExecResult createService();

    ExecResult operatePolicy(TenantResource resource);

    ExecResult deletePolicy(String policyName);
}
