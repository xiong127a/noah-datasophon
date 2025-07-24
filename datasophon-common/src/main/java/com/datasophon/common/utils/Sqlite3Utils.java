/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class Sqlite3Utils {

    private static final Logger logger = LoggerFactory.getLogger(Sqlite3Utils.class);


    private static void executeSql(String dbFilePath,
                                   String sql) throws SQLException {
        Connection connection = getConnection(dbFilePath);
        Statement statement = connection.createStatement();
        if (Objects.nonNull(statement)) {
            statement.executeUpdate(sql);
        }
        close(connection, statement);
    }


    public static ExecResult updateDatasource(String dbFilePath, String url) {
        ExecResult execResult = new ExecResult();
        String sql = String.format("UPDATE data_source SET url = '%s' WHERE uid = '1qRX1WdNk';", url);
        logger.info("update Datasource , the sql is {}", sql);
        try {
            executeSql(dbFilePath, sql);
            execResult.setExecResult(true);
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        return execResult;
    }


    private static Connection getConnection(String dbFilePath) throws SQLException {
        String url = "jdbc:sqlite:" + dbFilePath;
        return DriverManager.getConnection(url);
    }

    private static void close(Connection connection, Statement statement) throws SQLException {
        if (Objects.nonNull(connection) && Objects.nonNull(statement)) {
            statement.close();
            connection.close();
        }
    }


}
