package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("cluster/tenant")
public class ClusterTenantController {

    @Autowired
    private ClusterTenantService clusterTenantService;

    /**
     * 查询租户列表
     */
    @RequestMapping("/listTenant")
    public Result listTenant(Integer clusterId, Integer page, Integer size) {
        return clusterTenantService.listTenant(clusterId, page, size);
    }

    /**
     * 保存
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public Result save(@RequestBody ClusterTenant clusterTenant) throws Exception {
        return clusterTenantService.saveTenant(clusterTenant);
    }

    /**
     * 更新
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterTenant clusterTenant) {
        clusterTenantService.saveOrUpdate(clusterTenant);
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer id) {
        clusterTenantService.removeById(id);
        return Result.success();
    }

}
