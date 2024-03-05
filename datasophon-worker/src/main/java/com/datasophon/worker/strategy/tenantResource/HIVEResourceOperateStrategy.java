package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.Constants;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHiveResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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
            log.info("create hive database {} success", hiveResource.getHiveDatabase());
        } else {
            log.error("create hive database {} failed", hiveResource.getHiveDatabase());
            log.error(execResult.getExecErrOut());
            return execResult;
        }

        execResult = setHdfsQuota(convertGBToByte(hiveResource.getHiveDatabaseCapacity()), dbPathDir);
        if (execResult.getExecResult()) {
            log.info("set hive dbDir {} quota success", dbPathDir);
        } else {
            log.error("set hive dbDir {} quota failed", dbPathDir);
            log.error(execResult.getExecErrOut());
        }

        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = setHdfsQuota(convertGBToByte(hiveResource.getHiveDatabaseCapacity()), dbPathDir);
        if (execResult.getExecResult()) {
            log.info("set hive dbDir {} quota success", dbPathDir);
        } else {
            log.error("set hive dbDir {} quota failed", dbPathDir);
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = dropHiveDatabase(hiveResource.getHiveDatabase());
        if (execResult.getExecResult()) {
            log.info("drop hive database {} success", hiveResource.getHiveDatabase());
        } else {
            log.error("drop hive database {} failed", hiveResource.getHiveDatabase());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    private ExecResult createHiveDatabase(String databaseName, String dbPathDir) {
        // /opt/datasophon/hive/bin/hive -e "CREATE DATABASE IF NOT EXISTS t1 LOCATION '/user/hive/warehouse/t1'"
        return ShellUtils.exceShell(
                Constants.INSTALL_PATH +
                        "/hive/bin/hive -e \"CREATE DATABASE IF NOT EXISTS " +
                        databaseName +
                        " LOCATION '" +
                        dbPathDir +
                        "'\"");
    }

    private ExecResult dropHiveDatabase(String databaseName) {
        // /opt/datasophon/hive/bin/hive -e "DROP DATABASE IF EXISTS t1 CASCADE"
        return ShellUtils.exceShell(
                Constants.INSTALL_PATH +
                        "/hive/bin/hive -e \"DROP DATABASE IF EXISTS " +
                        databaseName +
                        " CASCADE\"");
    }

    /**
     * 设置hdfs文件夹容量限额
     */
    private ExecResult setHdfsQuota(String size, String hdfsPath) {
        // /opt/datasophon/hadoop-3.3.3/bin/hdfs dfsadmin -setSpaceQuota 1024 /tenant/t1
        List<String> commands = new ArrayList<>();
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfsadmin");
        commands.add("-setSpaceQuota");
        commands.add(size);
        commands.add(hdfsPath);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, log);
    }
}
