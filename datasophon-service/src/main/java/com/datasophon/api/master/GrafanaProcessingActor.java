package com.datasophon.api.master;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.command.Sqlite3ExecCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Sqlite3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GrafanaProcessingActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(GrafanaProcessingActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Sqlite3ExecCommand.class, this::processSqlite3Command)
                .matchAny(this::unhandled)
                .build();
    }

    private void processSqlite3Command(Sqlite3ExecCommand command) {
        try {
            logger.info("MasterNodeProcessingActor receive message: " + JSONUtil.toJsonStr(command));
            ExecResult execResult = new ExecResult();

            String dbFilePath = "/opt/datasophon/grafana/data/grafana.db";
            execResult = Sqlite3Utils.updateDatasource(dbFilePath, command.getUrl());
            if (execResult.getExecResult()) {
                logger.info(command.getGrafanaIp() + " update success");
            } else {
                logger.info(command.getGrafanaIp() + " update failed");
            }
            int tryTimes = 0;
            while (!execResult.getExecResult() && tryTimes < 3) {
                try {
                    TimeUnit.SECONDS.sleep(10L);

                    execResult = Sqlite3Utils.updateDatasource(dbFilePath, command.getUrl());

                    if (execResult.getExecResult()) {
                        logger.info(command.getGrafanaIp() + " update success");
                        break;
                    } else {
                        logger.info(command.getGrafanaIp() + " update failed");
                    }
                    tryTimes++;
                } catch (InterruptedException e) {
                    logger.info("The SR operate be sleep operation failed");
                }
            }
        } catch (Throwable e) {
            logger.error("Error processing Sqlite3ExecCommand", e);
        }
    }
}
