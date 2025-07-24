package com.datasophon.worker.actor;

import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.strategy.tenantResource.AbstractOperateStrategy;
import com.datasophon.worker.strategy.tenantResource.OperateStrategyFactory;

public class TenantResourceActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TenantFrameResource.class, resource -> {
                    AbstractOperateStrategy operateStrategy = OperateStrategyFactory
                            .createOperateStrategy(resource.getServiceName(), resource);
                    ExecResult execResult = executeOperation(operateStrategy, resource.getType());
                    getSender().tell(execResult, getSelf());
                })
                .matchAny(this::unhandled)
                .build();
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