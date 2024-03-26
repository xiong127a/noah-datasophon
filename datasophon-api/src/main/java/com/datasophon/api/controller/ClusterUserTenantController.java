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

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public Result add(Integer clusterId, Integer userId, String tenantIds) {
        return clusterUserTenantService.addUserToTenant(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Result delete(Integer clusterId, Integer userId, String tenantIds) {
        return clusterUserTenantService.deleteUser(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/getListByUserId")
    public Result getListByUserId(Integer clusterId, Integer userId) {
        return clusterUserTenantService.getListByUserId(clusterId, userId);
    }

}
