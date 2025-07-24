
package com.datasophon.api.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.common.model.OperationLogProp;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.model.MPage;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/log")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final ClusterInfoService clusterInfoService;
    private final FrameInfoMapper frameInfoMapper;

    // 如果只有一个构造函数，甚至可以省略@Autowired注解
    public OperationLogController(
            OperationLogService operationLogService,
            ClusterInfoService clusterInfoService,
            FrameInfoMapper frameInfoMapper) {
        this.operationLogService = operationLogService;
        this.clusterInfoService = clusterInfoService;
        this.frameInfoMapper = frameInfoMapper;
    }

    /**
     * 列表带分页
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Result list(@RequestBody MPage<OperationLog> mPage) {
        return Result.success(operationLogService.pageOperationLog(mPage));
    }

    /**
     * 当前集群所有服务名称
     */
    @RequestMapping("/serviceNameList")
    public Result serviceNameList(@RequestParam("clusterId") Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.getClusterFrame());
        List<FrameServiceEntity> list = QueryChain.of(FrameServiceEntity.class)
                .where(FrameServiceEntity::getFrameId).eq(frameInfo.getId())
                .orderBy(FrameServiceEntity::getSortNum).asc()
                .list();
        return Result.success(list.stream().map(FrameServiceEntity::getServiceName).collect(Collectors.toList()));
    }

    /**
     * 当前集群所有操作模块
     */
    @RequestMapping("/moduleList")
    public Result moduleList() throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:templates/operation-log.json");
        String operationLogString = FileUtil.readString(file, StandardCharsets.UTF_8);
        List<OperationLogProp> operationLogProps = JSONArray.parseArray(operationLogString, OperationLogProp.class);
        return Result.success(operationLogProps.stream().map(OperationLogProp::getOperationModule).distinct()
                .collect(Collectors.toList()));
    }

}
