package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.AutoScaleTaskEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动伸缩任务数据库映射接口
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper
public interface AutoScaleTaskMapper extends BaseMapper<AutoScaleTaskEntity> {
    // 基础CRUD操作由BaseMapper提供
    // 只在需要复杂查询时添加自定义方法
}