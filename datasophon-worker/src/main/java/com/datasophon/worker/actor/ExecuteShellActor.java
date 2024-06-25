package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;

public class ExecuteShellActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof String) {
            String command = (String) message;
            ExecResult execResult = ShellUtils.exceShell(command);
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(message);
        }
    }
}
