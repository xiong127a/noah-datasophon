package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("cluster/user/tenant")
@Validated
public class ClusterUserTenantController {

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public Result add(@NotNull Integer clusterId, @NotNull Integer userId, @NotBlank String tenantIds) {
        return clusterUserTenantService.addUserToTenant(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Result delete(@NotNull Integer clusterId, @NotNull Integer userId, @NotBlank String tenantIds) {
        return clusterUserTenantService.deleteUser(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/getListByUserId")
    public Result getListByUserId(Integer clusterId, Integer userId) {
        return clusterUserTenantService.getListByUserId(clusterId, userId);
    }

}
