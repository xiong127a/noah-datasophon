package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.mapper.OperationLogMapper;
import com.datasophon.dao.model.MPage;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("OperationLogService")
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public IPage<OperationLog> pageOperationLog(MPage<OperationLog> mPage) {

        //获取查询参数
        OperationLog param = Optional.ofNullable(mPage.getParam()).orElse(OperationLog.builder().build());

        //设置查询条件
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getOperationType())) {
            query.eq(OperationLog::getOperationType, param.getOperationType());
        }


        //分页查询
        return page(mPage, query);
    }



}
