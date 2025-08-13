package com.datasophon.dao.mapper;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.datasophon.dao.entity.OperationLogEntity;
import com.datasophon.dao.model.MPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志映射器
 * 按照架构重构规范，迁移QueryChain到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

    /**
     * 分页查询操作日志（带多条件过滤）
     *
     * @param mPage 分页参数
     * @return 分页结果
     */
    default Page<OperationLogEntity> selectPageWithFilters(@Param("mPage") MPage<OperationLogEntity> mPage) {
        OperationLogEntity param = mPage.getParam();

        QueryWrapper query = QueryWrapper.create()
                .where(OperationLogEntity::getClusterId).isNotNull()
                .orderBy(OperationLogEntity::getId, false); // 降序排列

        // 添加可选过滤条件
        if (param != null) {
            if (StrUtil.isNotBlank(param.getOperationModule())) {
                query.and(OperationLogEntity::getOperationModule).eq(param.getOperationModule());
            }

            if (StrUtil.isNotBlank(param.getOperateUser())) {
                query.and(OperationLogEntity::getOperateUser).eq(param.getOperateUser());
            }

            if (StrUtil.isNotBlank(param.getServiceName())) {
                query.and(OperationLogEntity::getServiceName).eq(param.getServiceName());
            }
        }

        return this.paginate(mPage, query);
    }
}
