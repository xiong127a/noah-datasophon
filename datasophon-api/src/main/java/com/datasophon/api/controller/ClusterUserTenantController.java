package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.utils.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("cluster/user/tenant")
@Validated
public class ClusterUserTenantController {

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public Result add(@RequestParam("clusterId") @NotNull Integer clusterId, @RequestParam("userId") @NotNull Integer userId, @RequestParam("tenantIds") @NotBlank String tenantIds) {
        return clusterUserTenantService.addUserToTenant(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Result delete(@RequestParam("clusterId") @NotNull Integer clusterId, @RequestParam("userId") @NotNull Integer userId, @RequestParam("tenantIds") @NotBlank String tenantIds) {
        return clusterUserTenantService.deleteUser(clusterId, userId, tenantIds);
    }

    @RequestMapping(value = "/getListByUserId")
    public Result getListByUserId(@RequestParam("clusterId") Integer clusterId, @RequestParam("userId") Integer userId) {
        return clusterUserTenantService.getListByUserId(clusterId, userId);
    }

}
