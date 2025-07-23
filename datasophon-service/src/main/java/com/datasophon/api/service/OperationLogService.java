package com.datasophon.api.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.model.MPage;

/**
 * 操作日志
 */
public interface OperationLogService extends IService<OperationLog> {

    Page<OperationLog> pageOperationLog(MPage<OperationLog> mPage);
}
