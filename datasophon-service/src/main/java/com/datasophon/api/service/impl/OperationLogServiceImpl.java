package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.mapper.OperationLogMapper;
import com.datasophon.dao.model.MPage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("operationLogService")
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public Page<OperationLog> pageOperationLog(MPage<OperationLog> mPage) {
        // 获取查询参数
        OperationLog param = Optional.ofNullable(mPage.getParam())
                .orElse(OperationLog.builder().build());

        // 构建查询条件
        QueryChain<OperationLog> query = QueryChain.of(OperationLog.class)
                .where(OperationLog::getClusterId).isNotNull();

        // 添加可选过滤条件
        if (StrUtil.isNotBlank(param.getOperationModule())) {
            query.and(OperationLog::getOperationModule).eq(param.getOperationModule());
        }

        if (StrUtil.isNotBlank(param.getOperateUser())) {
            query.and(OperationLog::getOperateUser).eq(param.getOperateUser());
        }

        if (StrUtil.isNotBlank(param.getServiceName())) {
            query.and(OperationLog::getServiceName).eq(param.getServiceName());
        }

        // 添加排序条件
        query.orderBy(OperationLog::getId).desc();

        // 执行分页查询
        Page<OperationLog> page = query.page(mPage);

        // 后处理查询结果
        List<OperationLog> records = page.getRecords();

        // 获取所有状态码与消息的映射
        Map<Integer, String> codeToMessageMap = Arrays.stream(Status.values())
                .collect(Collectors.toMap(
                        Status::getCode,
                        Status::getMsg,
                        (existingValue, newValue) -> existingValue,
                        HashMap::new));

        // 为每条记录设置状态消息并清除参数信息
        records.forEach(record -> {
            record.setReturnMsg(codeToMessageMap.get(record.getReturnCode()));
            record.setParam(null);
        });

        return page;
    }
}