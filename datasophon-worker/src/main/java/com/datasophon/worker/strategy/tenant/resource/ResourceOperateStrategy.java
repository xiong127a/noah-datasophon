package com.datasophon.worker.strategy.tenant.resource;

import com.datasophon.common.utils.ExecResult;

public interface ResourceOperateStrategy {

    ExecResult addSource();

    ExecResult updateSource();

    ExecResult deleteSource();

}
