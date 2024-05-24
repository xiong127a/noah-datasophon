package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHiveResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.utils.KerberosUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class HIVEResourceOperateStrategy extends AbstractOperateStrategy implements ResourceOperateStrategy {

    private final TenantHiveResource hiveResource;

    private final String dbPathDir;

    public HIVEResourceOperateStrategy(TenantFrameResource tenantResource) {
        super(tenantResource);
        this.hiveResource = (TenantHiveResource) tenantResource;
        this.dbPathDir = hiveResource.getHiveMetastoreDir() + "/" + hiveResource.getHiveDatabase();
    }

    @Override
    public ExecResult addSource() {
        execResult = createHiveDatabase(hiveResource.getHiveDatabase(), dbPathDir);
        if (execResult.getExecResult()) {
            logger.info("create hive database {} success", hiveResource.getHiveDatabase());
        } else {
            logger.error("create hive database {} failed", hiveResource.getHiveDatabase());
            logger.error(execResult.getExecOut());
            return execResult;
        }

        execResult = setHdfsQuota(convertGBToByte(hiveResource.getHiveDatabaseCapacity()), dbPathDir);
        if (execResult.getExecResult()) {
            logger.info("set hive dbDir {} quota success", dbPathDir);
        } else {
            logger.error("set hive dbDir {} quota failed", dbPathDir);
            logger.error(execResult.getExecOut());
        }

        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = setHdfsQuota(convertGBToByte(hiveResource.getHiveDatabaseCapacity()), dbPathDir);
        if (execResult.getExecResult()) {
            logger.info("set hive dbDir {} quota success", dbPathDir);
        } else {
            logger.error("set hive dbDir {} quota failed", dbPathDir);
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = dropHiveDatabase(hiveResource.getHiveDatabase());
        if (execResult.getExecResult()) {
            logger.info("drop hive database {} success", hiveResource.getHiveDatabase());
        } else {
            logger.error("drop hive database {} failed", hiveResource.getHiveDatabase());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    private ExecResult createHiveDatabase(String databaseName, String dbPathDir) {
        // /opt/datasophon/hive/bin/hive -e "CREATE DATABASE IF NOT EXISTS t1 LOCATION '/user/hive/warehouse/t1'"
        StringJoiner command = new StringJoiner(" ");
        if (hiveResource.getEnableKerberos()) {
            command.add(kinitKbStr("hive"));
            command.add(";");
        }
        command.add(Constants.INSTALL_PATH + "/hive/bin/hive");
        command.add("-e");
        command.add("\"CREATE DATABASE IF NOT EXISTS " + databaseName + " LOCATION '" + dbPathDir + "'\"");
        return ShellUtils.exceShell(command.toString());
    }

    private ExecResult dropHiveDatabase(String databaseName) {
        // /opt/datasophon/hive/bin/hive -e "DROP DATABASE IF EXISTS t1 CASCADE"
        StringJoiner command = new StringJoiner(" ");
        if (hiveResource.getEnableKerberos()) {
            command.add(kinitKbStr("hive"));
            command.add(";");
        }
        command.add(Constants.INSTALL_PATH + "/hive/bin/hive");
        command.add("-e");
        command.add("\"DROP DATABASE IF EXISTS " + databaseName + " CASCADE\"");
        return ShellUtils.exceShell(command.toString());
    }

    /**
     * 设置hdfs文件夹容量限额
     */
    private ExecResult setHdfsQuota(String size, String hdfsPath) {
        // /opt/datasophon/hadoop-3.3.3/bin/hdfs dfsadmin -setSpaceQuota 1024 /tenant/t1
        StringJoiner commands = new StringJoiner(" ");
        if (hiveResource.getEnableKerberos()) {
            KerberosUtils.downloadKeytabFromMaster("nn/" + CacheUtils.get(Constants.HOSTNAME), "nn.service.keytab");
            commands.add("sudo");
            commands.add("-u");
            commands.add("hdfs");
            commands.add(kinitKbStr("nn"));
            commands.add(";");
        }
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfsadmin");
        commands.add("-setSpaceQuota");
        commands.add(size);
        commands.add(hdfsPath);
        return ShellUtils.exceShell(commands.toString());
    }
}
