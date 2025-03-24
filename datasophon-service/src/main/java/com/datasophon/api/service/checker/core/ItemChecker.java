package com.datasophon.api.service.checker.core;

import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;

public interface ItemChecker {
    /**
     * 获取检查器类型
     * @return 检查器对应的检查项编码
     */
    ItemCode getCheckerType();

    /**
     * 执行检查
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 更新后的检查项
     * @throws Exception 如果执行过程中发生异常
     */
    CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) throws Exception;
    
    /**
     * 执行修复
     * @param clusterId 集群ID
     * @param hostInfo 主机信息
     * @param checkItem 检查项
     * @return 修复是否成功
     * @throws Exception 如果执行过程中发生异常
     */
    boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) throws Exception;
} 