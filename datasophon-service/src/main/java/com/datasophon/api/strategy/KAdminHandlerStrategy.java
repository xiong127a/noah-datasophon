package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class KAdminHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (CollUtil.isNotEmpty(hosts)) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${kadminHost}", hosts.get(0));
        }
    }


    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        // 调用通用方法，传递对应的actor路径
        performServiceRoleCheck(roleInstanceEntity, "executeCmdActor");
    }

    @Override
    public void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        // 调用通用方法，传递空字符串
        performServiceRoleCheck(roleInstanceEntity, "");
    }

}
