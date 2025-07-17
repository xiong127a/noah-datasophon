package com.datasophon.api.service.checker.core;

import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;

/**
 * 主机检查接口
 * 所有具体的检查项实现类都应实现此接口
 */
public interface HostChecker {
    
    /**
     * 执行检查
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 检查结果
     */
    CheckResult check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem);
    
    /**
     * 执行修复
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @param checkResult 检查结果
     * @return 是否修复成功
     */
    boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem, CheckResult checkResult);
} 