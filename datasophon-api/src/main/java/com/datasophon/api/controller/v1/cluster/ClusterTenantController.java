package com.datasophon.api.controller.v1.cluster;

import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterTenant;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@ApiVersion(path = "cluster/tenant")
public class ClusterTenantController {

    @Autowired
    private ClusterTenantService clusterTenantService;


    /**
     * 查询租户列表
     */
    @RequestMapping("/listTenant")
    public Result listTenant(@RequestParam("clusterId") Integer clusterId, @RequestParam("page") Integer page, @RequestParam("size") Integer size, @RequestParam("tenantName") String tenantName) {
        return clusterTenantService.listTenant(clusterId, page, size, tenantName);
    }

    /**
     * 保存
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public Result save(@RequestBody ClusterTenant clusterTenant) {
        return clusterTenantService.saveOrUpdateTenant(clusterTenant);
    }

    /**
     * 更新
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Result update(@RequestBody ClusterTenant clusterTenant) {
        return clusterTenantService.saveOrUpdateTenant(clusterTenant);
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestParam("id") Integer id) {
        return clusterTenantService.deleteTenantById(id);
    }

}
