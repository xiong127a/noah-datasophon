package com.datasophon.worker.actor;

import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class RMStateActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ExecuteCmdCommand.class, command -> {
                    ExecResult execResult = ShellUtils.exceShell(command.getCommandLine());
                    getSender().tell(execResult, getSelf());
                })
                .matchAny(this::unhandled)
                .build();
    }
}
