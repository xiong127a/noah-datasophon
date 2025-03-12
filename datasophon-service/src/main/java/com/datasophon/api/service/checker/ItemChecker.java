package com.datasophon.api.service.checker;

import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;

public interface ItemChecker {
    /**
     * 执行检查
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 更新后的检查项
     */
    CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem);
    
    /**
     * 执行修复
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 是否修复成功
     */
    boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem);
} 