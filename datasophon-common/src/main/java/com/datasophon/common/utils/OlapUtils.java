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

import com.datasophon.common.model.ProcInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility class for interacting with Doris/StarRocks databases.
 * Provides methods for cluster management operations such as adding followers,
 * observers, backends,
 * and retrieving cluster node status.
 */
public class OlapUtils {

    private static final Logger logger = LoggerFactory.getLogger(OlapUtils.class);

    // Constants for database connections and operations
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final int FE_QUERY_PORT = 9030;
    private static final int FE_RPC_PORT = 9010;
    private static final int BE_RPC_PORT = 9050;

    // SQL statement constants
    private static final String ADD_FOLLOWER_SQL = "ALTER SYSTEM ADD FOLLOWER ?";
    private static final String ADD_OBSERVER_SQL = "ALTER SYSTEM ADD OBSERVER ?";
    private static final String ADD_BACKEND_SQL = "ALTER SYSTEM ADD BACKEND ?";
    private static final String ADD_COMPUTE_NODE_SQL = "ALTER SYSTEM ADD COMPUTE NODE ?";
    private static final String SHOW_FRONTENDS_SQL = "SHOW PROC '/frontends'";
    private static final String SHOW_BACKENDS_SQL = "SHOW PROC '/backends'";
    private static final String SHOW_COMPUTE_NODES_SQL = "SHOW PROC '/compute_nodes'";

    /**
     * Adds a follower node to the Doris/StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the follower node to add
     * @return The execution result
     */
    public static ExecResult addFollower(String feMaster, String hostname) {
        ExecResult execResult = new ExecResult();
        logger.info("Adding follower node: {} to cluster with master: {}", hostname, feMaster);

        try {
            executeSql(feMaster, ADD_FOLLOWER_SQL, hostname + ":" + FE_RPC_PORT);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("Failed to add follower: {}", hostname, e);
        }
        return execResult;
    }

    /**
     * Adds an observer node to the Doris/StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the observer node to add
     * @return The execution result
     */
    public static ExecResult addObserver(String feMaster, String hostname) {
        ExecResult execResult = new ExecResult();
        logger.info("Adding observer node: {} to cluster with master: {}", hostname, feMaster);

        try {
            executeSql(feMaster, ADD_OBSERVER_SQL, hostname + ":" + FE_RPC_PORT);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("Failed to add observer: {}", hostname, e);
        }
        return execResult;
    }

    /**
     * Adds a backend node to the Doris/StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the backend node to add
     * @return The execution result
     */
    public static ExecResult addBackend(String feMaster, String hostname) {
        ExecResult execResult = new ExecResult();
        logger.info("Adding backend node: {} to cluster with master: {}", hostname, feMaster);

        try {
            executeSql(feMaster, ADD_BACKEND_SQL, hostname + ":" + BE_RPC_PORT);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("Failed to add backend: {}", hostname, e);
        }
        return execResult;
    }

    /**
     * Adds a compute node to the Doris/StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the compute node to add
     * @return The execution result
     */
    public static ExecResult addCn(String feMaster, String hostname) {
        ExecResult execResult = new ExecResult();
        logger.info("Adding compute node: {} to cluster with master: {}", hostname, feMaster);

        try {
            executeSql(feMaster, ADD_COMPUTE_NODE_SQL, hostname + ":" + BE_RPC_PORT);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("Failed to add compute node: {}", hostname, e);
        }
        return execResult;
    }

    /**
     * Executes a SQL statement with parameters using JDBC.
     *
     * @param feMaster The master FE hostname
     * @param sql      The SQL statement to execute
     * @param params   The parameters for the SQL statement
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    private static void executeSql(String feMaster, String sql, String... params)
            throws SQLException, ClassNotFoundException {
        try (Connection connection = getConnection(feMaster);
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                preparedStatement.setString(i + 1, params[i]);
            }

            preparedStatement.executeUpdate();
        }
    }

    /**
     * Adds a follower node using the mysql CLI client.
     * This is an alternative to JDBC when direct database access is preferred.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the follower node to add
     * @return The execution result
     */
    public static ExecResult addFollowerBySqlClient(String feMaster, String hostname) {
        String[] command = {
                "mysql",
                "-h", feMaster,
                "-u" + DEFAULT_USER,
                "-P" + FE_QUERY_PORT,
                "-e", String.format("ALTER SYSTEM ADD FOLLOWER \"%s:%d\"", hostname, FE_RPC_PORT)
        };

        return ShellUtils.exceShell(String.join(" ", command));
    }

    /**
     * Adds an observer node using the mysql CLI client.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the observer node to add
     * @return The execution result
     */
    public static ExecResult addObserverBySqlClient(String feMaster, String hostname) {
        String[] command = {
                "mysql",
                "-h", feMaster,
                "-u" + DEFAULT_USER,
                "-P" + FE_QUERY_PORT,
                "-e", String.format("ALTER SYSTEM ADD OBSERVER \"%s:%d\"", hostname, FE_RPC_PORT)
        };

        return ShellUtils.exceShell(String.join(" ", command));
    }

    /**
     * Adds a backend node using the mysql CLI client.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the backend node to add
     * @return The execution result
     */
    public static ExecResult addBackendBySqlClient(String feMaster, String hostname) {
        String[] command = {
                "mysql",
                "-h", feMaster,
                "-u" + DEFAULT_USER,
                "-P" + FE_QUERY_PORT,
                "-e", String.format("ALTER SYSTEM ADD BACKEND \"%s:%d\"", hostname, BE_RPC_PORT)
        };

        return ShellUtils.exceShell(String.join(" ", command));
    }

    /**
     * Adds a compute node using the mysql CLI client.
     *
     * @param feMaster The master FE hostname
     * @param hostname The hostname of the compute node to add
     * @return The execution result
     */
    public static ExecResult addCnBySqlClient(String feMaster, String hostname) {
        String[] command = {
                "mysql",
                "-h", feMaster,
                "-u" + DEFAULT_USER,
                "-P" + FE_QUERY_PORT,
                "-e", String.format("ALTER SYSTEM ADD COMPUTE NODE \"%s:%d\"", hostname, BE_RPC_PORT)
        };

        return ShellUtils.exceShell(String.join(" ", command));
    }

    /**
     * Creates a database connection to the specified FE master node.
     *
     * @param feMaster The master FE hostname
     * @return A database connection
     * @throws ClassNotFoundException If the driver class is not found
     * @throws SQLException           If a database access error occurs
     */
    private static Connection getConnection(String feMaster) throws ClassNotFoundException, SQLException {
        String url = String.format("jdbc:mysql://%s:%d", feMaster, FE_QUERY_PORT);

        // Load the driver
        Class.forName(JDBC_DRIVER);

        Properties info = new Properties();
        info.setProperty("user", DEFAULT_USER);
        info.setProperty("password", DEFAULT_PASSWORD);

        return DriverManager.getConnection(url, info);
    }

    /**
     * Returns a list of frontends in the Doris cluster.
     *
     * @param feMaster The master FE hostname
     * @return A list of ProcInfo objects representing the frontends
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    public static List<ProcInfo> showFrontends(String feMaster) throws SQLException, ClassNotFoundException {
        return executeQueryProcInfo(feMaster, SHOW_FRONTENDS_SQL, "HostName");
    }

    /**
     * Returns a list of frontends in the StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @return A list of ProcInfo objects representing the frontends
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    public static List<ProcInfo> showSRFrontends(String feMaster) throws SQLException, ClassNotFoundException {
        return executeQueryProcInfo(feMaster, SHOW_FRONTENDS_SQL, "IP");
    }

    /**
     * Returns a list of backends in the Doris cluster.
     *
     * @param feMaster The master FE hostname
     * @return A list of ProcInfo objects representing the backends
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    public static List<ProcInfo> showBackends(String feMaster) throws SQLException, ClassNotFoundException {
        return executeQueryProcInfo(feMaster, SHOW_BACKENDS_SQL, "HostName");
    }

    /**
     * Returns a list of backends in the StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @return A list of ProcInfo objects representing the backends
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    public static List<ProcInfo> showSRBackends(String feMaster) throws SQLException, ClassNotFoundException {
        return executeQueryProcInfo(feMaster, SHOW_BACKENDS_SQL, "IP");
    }

    /**
     * Returns a list of compute nodes in the StarRocks cluster.
     *
     * @param feMaster The master FE hostname
     * @return A list of ProcInfo objects representing the compute nodes
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    public static List<ProcInfo> showSRComputes(String feMaster) throws SQLException, ClassNotFoundException {
        return executeQueryProcInfo(feMaster, SHOW_COMPUTE_NODES_SQL, "IP");
    }

    /**
     * Executes a query and converts the result to a list of ProcInfo objects.
     *
     * @param feMaster       The master FE hostname
     * @param sql            The SQL query to execute
     * @param hostColumnName The name of the column that contains the hostname
     * @return A list of ProcInfo objects
     * @throws SQLException           If a database access error occurs
     * @throws ClassNotFoundException If the driver class is not found
     */
    private static List<ProcInfo> executeQueryProcInfo(String feMaster, String sql, String hostColumnName)
            throws SQLException, ClassNotFoundException {

        List<ProcInfo> list = new ArrayList<>();

        try (Connection connection = getConnection(feMaster);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                ProcInfo procInfo = new ProcInfo();
                procInfo.setHostName(resultSet.getString(hostColumnName));
                procInfo.setAlive(resultSet.getBoolean("Alive"));
                procInfo.setErrMsg(resultSet.getString("ErrMsg"));
                list.add(procInfo);
            }
        }

        return list;
    }
}
