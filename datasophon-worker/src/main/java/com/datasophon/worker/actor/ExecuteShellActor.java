package com.datasophon.worker.actor;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;

public class ExecuteShellActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, command -> {
                    ExecResult execResult = ShellUtils.exceShell(command);
                    getSender().tell(execResult, getSelf());
                })
                .matchAny(this::unhandled)
                .build();
    }
}
