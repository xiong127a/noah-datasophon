package com.datasophon.api.service.checker;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractItemChecker implements ItemChecker {
    private static final Logger logger = LoggerFactory.getLogger(AbstractItemChecker.class);
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";

    @Override
    public final CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        // 先将状态设置为检查中
        checkItem.setStatus(CheckItem.Status.CHECKING);
        checkItem.setMessage("检查中...");
        updateCheckStatus(clusterId, hostInfo, checkItem);

        return doCheck(hostInfo, checkItem);
    }

    @Override
    public boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 获取SSH会话并执行具体修复逻辑
            boolean doFix = doFix(hostInfo, checkItem);
            doCheck(hostInfo,checkItem);
            return doFix;
        } catch (Exception e) {
            logger.error("修复失败", e);
            return false;
        }
    }

    /**
     * 获取检查器类型
     */
    protected abstract ItemCode getCheckerType();

    /**
     * 执行具体的检查逻辑
     */
    protected abstract CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem);

    /**
     * 执行具体的修复逻辑
     */
    protected abstract boolean doFix(HostInfo hostInfo, CheckItem checkItem);

    private void updateCheckStatus(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String cacheKey = clusterId + Constants.HOST_MAP;
        Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
        if (hostInfoMap != null) {
            HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getHostname());
            if (cachedHostInfo != null) {
                cachedHostInfo.getCheckItems().stream()
                        .filter(item -> item.getId().equals(checkItem.getId()))
                        .findFirst()
                        .ifPresent(item -> {
                            item.setStatus(checkItem.getStatus());
                            item.setMessage(checkItem.getMessage());
                        });
                hostInfoMap.put(hostInfo.getHostname(), cachedHostInfo);
                CacheUtils.put(cacheKey, hostInfoMap);
            }
        }
    }

} 