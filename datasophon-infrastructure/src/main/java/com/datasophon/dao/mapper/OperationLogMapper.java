package com.datasophon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

}
