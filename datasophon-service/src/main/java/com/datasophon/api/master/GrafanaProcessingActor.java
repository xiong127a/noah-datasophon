package com.datasophon.api.master;

import akka.actor.UntypedActor;
import cn.hutool.json.JSONUtil;
import com.datasophon.common.command.Sqlite3ExecCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Sqlite3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GrafanaProcessingActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(GrafanaProcessingActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        logger.info("MasterNodeProcessingActor receive message: " + JSONUtil.toJsonStr(message));
        if (message instanceof Sqlite3ExecCommand) {
            Sqlite3ExecCommand command = (Sqlite3ExecCommand) message;
            ExecResult execResult = new ExecResult();

            String dbFilePath = "/opt/datasophon/grafana/data/grafana.db";
            execResult = Sqlite3Utils.updateDatasource(dbFilePath,command.getUrl());
            if (execResult.getExecResult()) {
                logger.info(command.getGrafanaIp() + " update success");
            } else {
                logger.info(command.getGrafanaIp() + " update failed");
            }
            int tryTimes = 0;
            while (!execResult.getExecResult() && tryTimes < 3) {
                try {
                    TimeUnit.SECONDS.sleep(10L);

                    execResult = Sqlite3Utils.updateDatasource(dbFilePath,command.getUrl());

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
        } else {
            unhandled(message);
        }
    }
}
