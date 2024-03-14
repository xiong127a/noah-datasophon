package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUserTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("cluster/user/tenant")
public class ClusterUserTenantController {

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Result add(@RequestBody ClusterUserTenant clusterUserTenant) {
        return clusterUserTenantService.addUserToTenant(clusterUserTenant);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Result delete(Integer clusterId, String userName, String tenantName) {
        return clusterUserTenantService.deleteUser(clusterId, userName, tenantName);
    }

}
