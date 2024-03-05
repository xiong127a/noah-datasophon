package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.TenantResource.*;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.strategy.tenantResource.AbstractOperateStrategy;
import com.datasophon.worker.strategy.tenantResource.OperateStrategyFactory;

public class TenantResourceActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof TenantFrameResource) {
            TenantFrameResource resource = (TenantFrameResource) message;
            AbstractOperateStrategy operateStrategy = OperateStrategyFactory.createOperateStrategy(resource.getServiceName(), resource);
            ExecResult execResult = executeOperation(operateStrategy, resource.getType());
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(message);
        }
    }

    private ExecResult executeOperation(AbstractOperateStrategy operateStrategy, String operationType) {
        ExecResult execResult;
        TROperateType opType;
        try {
            opType = TROperateType.valueOf(operationType);
        } catch (IllegalArgumentException e) {
            opType = TROperateType.NONE;
        }

        switch (opType) {
            case ADD:
                execResult = operateStrategy.addSource();
                break;
            case UPDATE:
                execResult = operateStrategy.updateSource();
                break;
            case DELETE:
                execResult = operateStrategy.deleteSource();
                break;
            case NONE:
                execResult = new ExecResult();
                execResult.setExecResult(true);
                break;
            default:
                execResult = new ExecResult();
                break;
        }
        return execResult;
    }

}