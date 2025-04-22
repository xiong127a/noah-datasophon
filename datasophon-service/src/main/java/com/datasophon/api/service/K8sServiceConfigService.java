package com.datasophon.api.service;

import com.datasophon.common.utils.Result;
import org.springframework.web.bind.annotation.RequestParam;

public interface K8sServiceConfigService {

    Result getK8sConfigMaps(Integer clusterId,String serviceName);

    Result getK8sConfigMapDetail(Integer clusterId,String name);

    Result updateK8sConfigMap(Integer clusterId,String name, String content);

    Result getK8sServices(Integer clusterId,String serviceName);

    Result getK8sServiceDetail(Integer clusterId,String name);

    Result updateK8sService(Integer clusterId,String name, String content);

    Result getK8sDeployments(Integer clusterId, String serviceName);

    Result getK8sDeploymentDetail(Integer clusterId, String name);

    Result updateK8sDeployment(Integer clusterId, String name, String content);

    Result getK8sStatefulSets(Integer clusterId, String serviceName);

    Result getK8sStatefulSetDetail(Integer clusterId, String name);

    Result updateK8sStatefulSet(Integer clusterId, String name, String content);

    Result getK8sPvcs(Integer clusterId, String content);

    Result getK8sPvcDetail(Integer clusterId,String name);

    Result updateK8sPvc(Integer clusterId,String name, String content);
}