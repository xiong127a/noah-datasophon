package com.datasophon.common.enums.handler;

import com.datasophon.common.enums.ClusterType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;


import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ClusterType枚举的MyBatis类型处理器
 * 使用code字段进行数据库存储和读取
 * 
 * @author DataSophon Team
 */
@MappedTypes(ClusterType.class)
public class ClusterTypeHandler extends BaseTypeHandler<ClusterType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ClusterType parameter, JdbcType jdbcType) throws SQLException {
        // 存储时使用code字段
        ps.setString(i, parameter.getCode());
    }

    @Override
    public ClusterType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : ClusterType.fromCode(code);
    }

    @Override
    public ClusterType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : ClusterType.fromCode(code);
    }

    @Override
    public ClusterType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : ClusterType.fromCode(code);
    }
}