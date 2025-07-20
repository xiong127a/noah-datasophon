package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.command.OlapSqlExecCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MasterNodeProcessingActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(MasterNodeProcessingActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OlapSqlExecCommand.class, this::processOlapSqlCommand)
                .matchAny(this::unhandled)
                .build();
    }

    private void processOlapSqlCommand(OlapSqlExecCommand command) {
        try {
            logger.info("MasterNodeProcessingActor receive message: " + JSONUtil.toJsonStr(command));
            ExecResult execResult = new ExecResult();
            String tip = command.getOpsType().getDesc();
            switch (command.getOpsType()) {
                case ADD_BE:
                    execResult = OlapUtils.addBackend(command.getFeMaster(), command.getHostName());
                    break;
                case ADD_FE_FOLLOWER:
                    execResult = OlapUtils.addFollower(command.getFeMaster(), command.getHostName());
                    break;
                case ADD_FE_OBSERVER:
                    execResult = OlapUtils.addObserver(command.getFeMaster(), command.getHostName());
                    break;
                case ADD_CN:
                    execResult = OlapUtils.addCn(command.getFeMaster(), command.getHostName());
                    break;
            }
            if (execResult.getExecResult()) {
                logger.info(command.getHostName() + " " + tip + " added success");
            } else {
                logger.info(command.getHostName() + " " + tip + " added failed");
            }
            int tryTimes = 0;
            while (!execResult.getExecResult() && tryTimes < 3) {
                try {
                    TimeUnit.SECONDS.sleep(10L);
                    switch (command.getOpsType()) {
                        case ADD_BE:
                            execResult = OlapUtils.addBackendBySqlClient(command.getFeMaster(), command.getHostName());
                            break;
                        case ADD_FE_FOLLOWER:
                            execResult = OlapUtils.addFollowerBySqlClient(command.getFeMaster(), command.getHostName());
                            break;
                        case ADD_FE_OBSERVER:
                            execResult = OlapUtils.addObserverBySqlClient(command.getFeMaster(), command.getHostName());
                            break;
                        case ADD_CN:
                            execResult = OlapUtils.addCnBySqlClient(command.getFeMaster(), command.getHostName());
                            break;
                    }
                    if (execResult.getExecResult()) {
                        logger.info(command.getHostName() + " " + tip + " added success");
                        break;
                    } else {
                        logger.info(command.getHostName() + " " + tip + " added failed");
                    }
                    tryTimes++;
                } catch (InterruptedException e) {
                    logger.info("The SR operate be sleep operation failed");
                }
            }
        } catch (Throwable e) {
            logger.error("Error processing OlapSqlExecCommand", e);
        }
    }
}
