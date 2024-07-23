package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.K8sClusterInfoEntity;
import com.datasophon.k8s.service.K8sClusterInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/k8s/cluster")
public class K8sClusterInfoController {
    @Autowired
    private K8sClusterInfoService k8sClusterInfoService;

    @RequestMapping("/save")
    @UserPermission
    public Result save(@RequestBody K8sClusterInfoEntity k8sClusterInfo) {
        return k8sClusterInfoService.saveCluster(k8sClusterInfo);
    }

}
