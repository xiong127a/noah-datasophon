package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class KAdminHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Long clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        if (CollUtil.isNotEmpty(hosts)) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${kadminHost}", hosts.getFirst());
        }
    }


    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto, Map<String, ClusterServiceRoleInstanceDTO> map) {
        // 调用通用方法，传递对应的actor路径
        performServiceRoleCheck(roleInstanceDto, "executeCmdActor");
    }

    @Override
    public void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto, Map<String, ClusterServiceRoleInstanceDTO> map) {
        // 调用通用方法，传递空字符串
        performServiceRoleCheck(roleInstanceDto, "");
    }

}
