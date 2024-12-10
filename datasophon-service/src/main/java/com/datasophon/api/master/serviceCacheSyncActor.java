package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.CacheCommand;
import com.datasophon.common.command.ConfigMapCacheCommand;
import com.datasophon.common.command.VariableCacheCommand;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class serviceCacheSyncActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(serviceCacheSyncActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        ExecResult result = new ExecResult();
        result.setExecResult(true);
        if (msg instanceof ConfigMapCacheCommand) {
            final ConfigMapCacheCommand configMapCacheCommand = (ConfigMapCacheCommand) msg;

            logger.info("receive cache configMap ： " + configMapCacheCommand.getKey());

            ServiceConfigMap.put(
                    configMapCacheCommand.getKey(),
                    configMapCacheCommand.getConfigs());
            logger.info("sync cache configMap： " + configMapCacheCommand.getKey());

            result.setExecOut("success cache configMap： " + configMapCacheCommand.getKey());

        } else if (msg instanceof VariableCacheCommand) {
            final VariableCacheCommand variableCacheCommand = (VariableCacheCommand) msg;

            logger.info("receive cache variable " + variableCacheCommand.getKey());

            Map<String, String> globalVariables = GlobalVariables.get(variableCacheCommand.getClusterId());

            globalVariables.put(variableCacheCommand.getKey(), variableCacheCommand.getValue());

            result.setExecOut("success cache variable " + variableCacheCommand.getKey());

        } else if (msg instanceof CacheCommand) {
            final CacheCommand cacheCommand = (CacheCommand) msg;

            logger.info("get cache key " + cacheCommand.getKey());

            String key = cacheCommand.getKey();

            if (cacheCommand.isDelete()) {
                CacheUtils.removeKey(key);
                return;
            }

            if (CacheUtils.constainsKey(key)) {
                result.setObject(CacheUtils.get(key));
                logger.info("get cache value success");
            } else {
                logger.warn("Cache key not found: " + key);
                result.setExecResult(false);
            }

        } else {

            result.setExecOut("receive unknown message");

        }
        getSender().tell(result, getSelf());
    }


}
