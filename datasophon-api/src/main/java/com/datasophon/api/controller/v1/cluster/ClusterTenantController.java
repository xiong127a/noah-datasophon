package com.datasophon.api.controller.v1.cluster;

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.converter.ClusterTenantConverter;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterTenantVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 集群租户控制器
 * 提供租户管理的HTTP接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/tenant")
public class ClusterTenantController {

    @Autowired
    private ClusterTenantService clusterTenantService;

    @Autowired
    private ClusterTenantConverter clusterTenantConverter;

    /**
     * 查询租户列表
     */
    @RequestMapping("/listTenant")
    public Result<PageResult<ClusterTenantVO>> listTenant(@ClusterId Long clusterId,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "tenantName", required = false) String tenantName) {
        PageResult<ClusterTenantDTO> pageResult = clusterTenantService.listTenant(clusterId, page, size, tenantName);
        PageResult<ClusterTenantVO> voPageResult = PageResult.of(
                clusterTenantConverter.dtoListToVoList(pageResult.getRecords()),
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getSize());
        return Result.success(voPageResult);
    }

    /**
     * 保存租户
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public Result<ClusterTenantVO> save(@RequestBody ClusterTenantDTO clusterTenantDTO) {
        try {
            ClusterTenantDTO savedDto = clusterTenantService.saveOrUpdateTenant(clusterTenantDTO);
            ClusterTenantVO vo = clusterTenantConverter.dtoToVo(savedDto);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新租户
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Result<ClusterTenantVO> update(@RequestBody ClusterTenantDTO clusterTenantDTO) {
        try {
            ClusterTenantDTO updatedDto = clusterTenantService.updateTenant(clusterTenantDTO);
            ClusterTenantVO vo = clusterTenantConverter.dtoToVo(updatedDto);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除租户
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("id") Long id) {
        try {
            boolean deleted = clusterTenantService.deleteTenantById(id);
            if (deleted) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取租户详情
     */
    @RequestMapping("/getById")
    public Result<ClusterTenantVO> getById(@RequestParam("id") Long id) {
        ClusterTenantDTO dto = clusterTenantService.getByIdAsDto(id);
        if (dto != null) {
            ClusterTenantVO vo = clusterTenantConverter.dtoToVo(dto);
            return Result.success(vo);
        } else {
            return Result.error("租户不存在");
        }
    }

}
