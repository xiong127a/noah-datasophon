package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.AutoScaleTaskEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.AutoScaleTaskEntityTableDef.AUTO_SCALE_TASK_ENTITY;

/**
 * 自动伸缩任务数据库映射接口
 * 按照架构重构规范，迁移QueryChain到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface AutoScaleTaskMapper extends BaseMapper<AutoScaleTaskEntity> {

    /**
     * 根据集群ID分页查询自动伸缩任务（按创建时间降序）
     *
     * @param clusterId 集群ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    default Page<AutoScaleTaskEntity> selectPageByClusterId(Long clusterId, Integer page, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
            .where(AUTO_SCALE_TASK_ENTITY.CLUSTER_ID.eq(clusterId))
            .orderBy(AUTO_SCALE_TASK_ENTITY.CREATED_AT.desc());
        return this.paginate(Page.of(page, pageSize), query);
    }

    /**
     * 根据集群ID查询启用的自动伸缩任务列表（按创建时间降序）
     *
     * @param clusterId 集群ID
     * @return 任务列表
     */
    default List<AutoScaleTaskEntity> selectEnabledByClusterId(Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
            .where(AUTO_SCALE_TASK_ENTITY.CLUSTER_ID.eq(clusterId))
            .and(AUTO_SCALE_TASK_ENTITY.ENABLED.eq(true))
            .orderBy(AUTO_SCALE_TASK_ENTITY.CREATED_AT.desc());
        return this.selectListByQuery(query);
    }
}