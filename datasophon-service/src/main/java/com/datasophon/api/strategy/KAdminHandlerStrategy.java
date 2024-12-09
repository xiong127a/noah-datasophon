package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.k8s.util.K8sUtil;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;

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
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        // To be implemented, if needed.
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        // To be implemented, if needed.
    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
        // To be implemented, if needed.
    }


    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        // 调用通用方法，传递对应的actor路径
        performServiceRoleCheck(roleInstanceEntity, "executeCmdActor");
    }

    @Override
    public void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        // 调用通用方法，传递空字符串
        performServiceRoleCheck(roleInstanceEntity, "");
    }

}
