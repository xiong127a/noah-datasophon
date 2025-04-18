package com.datasophon.api.controller;

import com.datasophon.api.service.K8sServiceConfigService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/k8s")
public class K8sServiceConfigController {

    @Autowired
    private K8sServiceConfigService k8sServiceConfigService;

    @GetMapping("/configmaps")
    public Result getK8sConfigMaps(@RequestParam Integer clusterId,@RequestParam("serviceName") String serviceName) {
        return k8sServiceConfigService.getK8sConfigMaps(clusterId,serviceName);
    }

    @GetMapping("/configmap/detail")
    public Result getK8sConfigMapDetail(@RequestParam Integer clusterId,@RequestParam String name) {
        return k8sServiceConfigService.getK8sConfigMapDetail(clusterId,name);
    }

    @PostMapping("/configmap/update")
    public Result updateK8sConfigMap(@RequestParam Integer clusterId,@RequestParam String name, @RequestParam String content) {
        return k8sServiceConfigService.updateK8sConfigMap(clusterId,name, content);
    }

    @GetMapping("/services")
    public Result getK8sServices(@RequestParam Integer clusterId, @RequestParam String serviceName) {
        return k8sServiceConfigService.getK8sServices(clusterId, serviceName);
    }

    @GetMapping("/service/detail")
    public Result getK8sServiceDetail(@RequestParam Integer clusterId,@RequestParam String name) {
        return k8sServiceConfigService.getK8sServiceDetail(clusterId,name);
    }

    @PostMapping("/service/update")
    public Result updateK8sService(@RequestParam Integer clusterId,@RequestParam String name, @RequestParam String content) {
        return k8sServiceConfigService.updateK8sService(clusterId,name, content);
    }

    @GetMapping("/pvcs")
    public Result getK8sPvcs(@RequestParam String clusterId) {
        return k8sServiceConfigService.getK8sPvcs(clusterId);
    }

    @GetMapping("/pvc/detail")
    public Result getK8sPvcDetail(@RequestParam Integer clusterId,@RequestParam String name) {
        return k8sServiceConfigService.getK8sPvcDetail(clusterId,name);
    }

    @PostMapping("/pvc/update")
    public Result updateK8sPvc(@RequestParam Integer clusterId,@RequestParam String name, @RequestParam String content) {
        return k8sServiceConfigService.updateK8sPvc(clusterId,name, content);
    }
}