package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
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

public class serviceCacheSyncActor extends AbstractActor {
    private static final Logger logger = LoggerFactory.getLogger(serviceCacheSyncActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ConfigMapCacheCommand.class, this::handleConfigMapCache)
                .match(VariableCacheCommand.class, this::handleVariableCache)
                .match(CacheCommand.class, this::handleCache)
                .matchAny(msg -> {
                    ExecResult result = new ExecResult();
                    result.setExecResult(true);
                    result.setExecOut("receive unknown message");
                    getSender().tell(result, getSelf());
                })
                .build();
    }

    private void handleConfigMapCache(ConfigMapCacheCommand configMapCacheCommand) {
        logger.info("receive cache configMap ： {}", configMapCacheCommand.getKey());

        ServiceConfigMap.put(
                configMapCacheCommand.getKey(),
                configMapCacheCommand.getConfigs());
        logger.info("sync cache configMap： {}", configMapCacheCommand.getKey());

        ExecResult result = new ExecResult();
        result.setExecResult(true);
        result.setExecOut("success cache configMap： " + configMapCacheCommand.getKey());
        getSender().tell(result, getSelf());
    }

    private void handleVariableCache(VariableCacheCommand variableCacheCommand) {
        logger.info("receive cache variable {}", variableCacheCommand.getKey());

        Map<String, String> globalVariables = GlobalVariables.get(variableCacheCommand.getClusterId());

        globalVariables.put(variableCacheCommand.getKey(), variableCacheCommand.getValue());

        ExecResult result = new ExecResult();
        result.setExecResult(true);
        result.setExecOut("success cache variable " + variableCacheCommand.getKey());
        getSender().tell(result, getSelf());
    }

    private void handleCache(CacheCommand cacheCommand) {
        logger.info("get cache key {}", cacheCommand.getKey());

        String key = cacheCommand.getKey();
        ExecResult result = new ExecResult();
        result.setExecResult(true);

        if (cacheCommand.isDelete()) {
            CacheUtils.removeKey(key);
            return;
        }

        if (CacheUtils.containsKey(key)) {
            result.setObject(CacheUtils.get(key));
            logger.info("get cache value success");
        } else {
            logger.warn("Cache key not found: {}", key);
            result.setExecResult(false);
        }

        getSender().tell(result, getSelf());
    }
}
