package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.mapper.OperationLogMapper;
import com.datasophon.dao.model.MPage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("OperationLogService")
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public IPage<OperationLog> pageOperationLog(MPage<OperationLog> mPage) {

        //获取查询参数
        OperationLog param = Optional.ofNullable(mPage.getParam()).orElse(OperationLog.builder().build());

        //设置查询条件
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<>();
        query
                .isNotNull(OperationLog::getClusterId)
                .eq(StrUtil.isNotBlank(param.getOperationModule()), OperationLog::getOperationModule, param.getOperationModule())
                .eq(StrUtil.isNotBlank(param.getOperateUser()), OperationLog::getOperateUser, param.getOperateUser())
                .eq(StrUtil.isNotBlank(param.getServiceName()), OperationLog::getServiceName, param.getServiceName());

        IPage<OperationLog> page = page(mPage, query);
        List<OperationLog> records = page.getRecords();
        Map<Integer, String> codeMap =
                Arrays.stream(Status.values()).collect(Collectors.toMap(Status::getCode, Status::getMsg, (a, b) -> a, HashMap::new));
        for (OperationLog record : records) {
            record.setReturnMsg(codeMap.get(record.getReturnCode()));
            record.setParam(null);
        }
        page.setRecords(records);

        //分页查询
        return page;
    }

}