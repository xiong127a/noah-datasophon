package com.datasophon.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.model.MPage;

/**
 * 操作日志
 */
public interface OperationLogService extends IService<OperationLog> {

    IPage<OperationLog> pageOperationLog(MPage<OperationLog> mPage);
}
