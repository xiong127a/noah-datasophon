package com.datasophon.api.utils.ranger.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;

import java.util.Map;


public abstract class AbstractRangerStrategy implements RangerStrategy{

    public RangerClient rangerClient;

    public RangerClient rangerKmsClient;

    public Map<String, String> globalVariables;

    public ExecResult execResult;

    public Logger logger;

    public AbstractRangerStrategy(Integer clusterId) throws Exception {
        this.rangerClient = RangerUtil.getRangerClient(clusterId);
        this.rangerKmsClient = RangerUtil.getRangerKmsClient(clusterId);
        this.globalVariables = GlobalVariables.get(clusterId);
        this.execResult = new ExecResult();
    }

}
