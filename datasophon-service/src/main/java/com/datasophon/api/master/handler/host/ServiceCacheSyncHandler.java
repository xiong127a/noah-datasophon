package com.datasophon.api.master.handler.host;


import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.datasophon.common.utils.HostUtils.GetMasterHost;

public class ServiceCacheSyncHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCacheSyncHandler.class);

    public ServiceCacheSyncHandler() {

    }

    public ExecResult serviceCacheSync(Object object) {
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(false);
        boolean isHa = PropertyUtils.getBoolean("isHa");
        if (!isHa){
            return execResult;
        }

        Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        List<String> masterhosts = GetMasterHost();

        for (String hostname : masterhosts) {
            try {
                //不与自身交互
                if (hostname.equals(InetAddress.getLocalHost().getHostName())) {
                    continue;
                }
                //跳过不在线的服务
                if (!HostUtils.checkServiceOnlineWithRetry(hostname,PropertyUtils.getInt(Constants.MASTER_WEB_PORT),1,1000)){
                    logger.info("Service at host {} is offline.", hostname);
                    continue;
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            ActorSelection cacheSyncActor = ActorUtils.actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + hostname + ":2551/user/serviceCacheSyncActor");
            Future<Object> future = Patterns.ask(cacheSyncActor, object, timeout);
            try {
                ExecResult result = (ExecResult) Await.result(future, timeout.duration());
                logger.info(result.getExecOut());
                return result;
            } catch (Exception e) {
                execResult.setExecResult(false);
                return execResult;
            }
        }
        return execResult;
    }




}
