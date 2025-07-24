package com.datasophon.worker.strategy.tenant.resource;

import com.datasophon.common.Constants;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantHdfsResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;

import java.util.StringJoiner;

public class HDFSResourceOperateStrategy extends AbstractOperateStrategy implements ResourceOperateStrategy {


    private TenantHdfsResource hdfsResource;

    public HDFSResourceOperateStrategy(TenantFrameResource tenantResource) {
        super(tenantResource);
        this.hdfsResource = (TenantHdfsResource) tenantResource;
    }

    @Override
    public ExecResult addSource() {
        execResult = createHdfsDir(hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            logger.info("hdfs create dir {} success", hdfsResource.getHdfsPath());
        } else {
            logger.error("hdfs create dir {} failed", hdfsResource.getHdfsPath());
            logger.error(execResult.getExecOut());
            return execResult;
        }

        execResult = setHdfsQuota(convertGBToByte(hdfsResource.getHdfsSpaceQuota()), hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            logger.info("hdfs set dir {} quota success", hdfsResource.getHdfsPath());
        } else {
            logger.error("hdfs set dir {} quota failed", hdfsResource.getHdfsPath());
            logger.error(execResult.getExecOut());
        }

        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = setHdfsQuota(convertGBToByte(hdfsResource.getHdfsSpaceQuota()), hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            logger.info("hdfs set dir {} quota success", hdfsResource.getHdfsPath());
        } else {
            logger.error("hdfs set dir {} quota failed", hdfsResource.getHdfsPath());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = deleteHdfsPath(hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            logger.info("delete hdfs path {} success", hdfsResource.getHdfsPath());
        } else {
            logger.error("delete hdfs path {} success", hdfsResource.getHdfsPath());
            logger.error(execResult.getExecOut());
        }
        return execResult;
    }

    /**
     * 创建hdfs目录
     */
    private ExecResult createHdfsDir(String hdfsPath) {
        StringJoiner commands = new StringJoiner(" ");
        if (hdfsResource.getEnableKerberos()) {
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
        commands.add("dfs");
        commands.add("-mkdir");
        commands.add("-p");
        commands.add(hdfsPath);
        commands.add(";");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-chmod");
        commands.add("777");
        commands.add(hdfsPath);
        return ShellUtils.exceShell(commands.toString());
    }

    /**
     * 设置hdfs文件夹容量限额
     */
    private ExecResult setHdfsQuota(String size, String hdfsPath) {
        // /opt/datasophon/hadoop-3.3.3/bin/hdfs dfsadmin -setSpaceQuota 1024 /tenant/t1
        StringJoiner commands = new StringJoiner(" ");
        if (hdfsResource.getEnableKerberos()) {
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

    private ExecResult deleteHdfsPath(String hdfsPath) {
        StringJoiner commands = new StringJoiner(" ");
        if (hdfsResource.getEnableKerberos()) {
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
        commands.add("dfs");
        commands.add("-rm");
        commands.add("-r");
        commands.add(hdfsPath);
        return ShellUtils.exceShell(commands.toString());
    }
}
