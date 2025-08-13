package com.datasophon.api.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.datasophon.dao.entity.OperationLogEntity;
import com.datasophon.dao.model.MPage;

/**
 * 操作日志服务
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface OperationLogService extends IService<OperationLogEntity> {

    /**
     * 分页查询操作日志
     * 
     * @param mPage 分页参数
     * @return 分页结果
     */
    Page<OperationLogEntity> pageOperationLog(MPage<OperationLogEntity> mPage);
}
