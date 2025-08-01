package com.datasophon.api.service;

import com.mybatisflex.core.paginate.Page;

import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.model.MPage;

/**
 * 操作日志
 */
public interface OperationLogService {

    Page<OperationLog> pageOperationLog(MPage<OperationLog> mPage);
}
