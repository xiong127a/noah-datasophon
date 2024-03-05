package com.datasophon.api.utils.ranger.strategy;

import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;

public interface RangerStrategy {

    ExecResult createService() throws Exception;

    ExecResult operatePolicy(ClusterTenant clusterTenant) throws Exception;

}
