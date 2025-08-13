package com.datasophon.api.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.dao.entity.OperationLogEntity;
import com.datasophon.dao.mapper.OperationLogMapper;
import com.datasophon.dao.model.MPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现
 * 按照架构重构规范，迁移QueryChain到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("operationLogService")
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLogEntity>
        implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Override
    public Page<OperationLogEntity> pageOperationLog(MPage<OperationLogEntity> mPage) {
        // 调用Mapper中的分页查询方法
        Page<OperationLogEntity> page = getMapper().selectPageWithFilters(mPage);

        // 后处理查询结果
        List<OperationLogEntity> records = page.getRecords();

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

        logger.debug("分页查询操作日志完成，返回 {} 条记录", records.size());
        return page;
    }
}